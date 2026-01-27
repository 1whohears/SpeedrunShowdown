package com.github.speedrunshowdown;

import com.github.speedrunshowdown.commands.StartCommand;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.speedrunshowdown.LeagueBotApiManager.GSON;

public class InternalApiServer {

    private HttpServer server;
    private final SpeedrunShowdown plugin;

    public void sendStatus(String status) {
        String url = getRequestURL("status");
        String jsonBody = "{\"status\":\""+status+"\",\"gameId\":\""+plugin.getGameplayServerId()+"\"}";
        handlePostAsync(url, jsonBody, null,
                response -> plugin.getLogger().info("Sent Status "+status));
    }

    public void sendResetRequest() {
        String url = getRequestURL("reset_seed");
        String jsonBody = "{\"gameId\":\""+plugin.getGameplayServerId()+"\"}";
        handlePostAsync(url, jsonBody, null,
                response -> plugin.getLogger().info("Sent Reset Request"));
    }

    public InternalApiServer() {
        this.plugin = SpeedrunShowdown.getInstance();
    }

    public void start() throws IOException {
        int port = plugin.getConfig().getInt("gameplay_server_api_port");

        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/ping", this::handlePing);
        server.createContext("/seed_reset", this::handleReset);
        server.createContext("/set_queue", this::handleSetQueue);

        server.setExecutor(null);
        server.start();

        plugin.getLogger().info("API started on :"+port);
    }

    private void handlePing(HttpExchange ex) throws IOException {
        reply(ex, 200, "game server "+plugin.getGameplayServerId()+" ok");
    }

    private void handleReset(HttpExchange ex) throws IOException {
        //plugin.getLogger().info("received seed_reset "+ex.getRequestHeaders().toString());
        String key = ex.getRequestHeaders().getFirst("X-Auth");
        if (!key.equals(plugin.getConfig().getString("pterodactyl_api_key"))) {
            //plugin.getLogger().warning("Reset Failed Cause Bad Key "+key);
            ex.sendResponseHeaders(401, -1);
            return;
        }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            //plugin.getLogger().warning("Reset Failed Cause Not POST");
            ex.sendResponseHeaders(405, -1);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, plugin::resetSeed);

        reply(ex, 200, "{\"result\":\"Restarting...\"}");
    }

    private void handleSetQueue(HttpExchange ex) throws IOException {
        String key = ex.getRequestHeaders().getFirst("X-Auth");
        if (!key.equals(plugin.getConfig().getString("pterodactyl_api_key"))) {
            ex.sendResponseHeaders(401, -1);
            return;
        }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes());
        JsonObject response;
        try {
            response = GSON.fromJson(body, JsonObject.class);
        } catch (JsonSyntaxException e) {
            plugin.getLogger().warning("Failed to parse json: "+body);
            ex.sendResponseHeaders(410, -1);
            return;
        }

        if (!response.has("queueId")) {
            reply(ex, 400, "Missing queueId");
            return;
        }
        int queueId = response.get("queueId").getAsInt();

        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLeagueBotApiManager().setCurrentQueueId(queueId));

        plugin.getLogger().info("Queue ID has Been set to "+queueId);

        reply(ex, 200, "{\"result\":\"Set Queue to "+queueId+"\"}");
    }

    private void reply(HttpExchange ex, int code, String msg) throws IOException {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private String getRequestURL(String type) {
        String host = plugin.getConfig().getString("velocity_ip");
        int port = plugin.getConfig().getInt("velocity_port");
        String url = host+":"+port+"/"+type;
        plugin.getLogger().info("CREATED URL: "+url);
        return url;
    }

    private void handlePostAsync(String requestURL, String jsonBody, @Nullable CommandSender sender,
                                 @NotNull Consumer<JsonObject> responseHandler) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return post(requestURL, jsonBody, sender);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(responseStr -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (responseStr == null) return;
                try {
                    JsonObject response = GSON.fromJson(responseStr, JsonObject.class);
                    responseHandler.accept(response);
                } catch (Exception e) {
                    plugin.getServer().broadcastMessage(ChatColor.RED+
                            "FAILED TO HANDLE RESPONSE: " + e.getMessage());
                    plugin.getLogger().severe("FAILED TO HANDLE RESPONSE: " + requestURL
                            +"\n"+e.getMessage()+"\n"+responseStr);
                    e.printStackTrace();
                }
            });
        });
    }

    @Nullable
    private String post(String requestURL, String jsonBody, @Nullable CommandSender sender) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestURL))
                .header("X-Auth", plugin.getConfig().getString("pterodactyl_api_key"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code > 299) {
                Component msg = Component.text("Failed: " + code)
                        .color(TextColor.color(0xFF0000));
                if (sender != null) sender.sendMessage(msg);
                else SpeedrunShowdown.getInstance().getServer().broadcast(msg);
                return null;
            }
            return response.body();
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(SpeedrunShowdown.getInstance(), () -> {
                String msg = ChatColor.RED + "Failed: " + e.getMessage();
                if (sender != null) sender.sendMessage(msg);
                else SpeedrunShowdown.getInstance().getServer().broadcastMessage(msg);
            });
            e.printStackTrace();
            return null;
        }
    }
}

