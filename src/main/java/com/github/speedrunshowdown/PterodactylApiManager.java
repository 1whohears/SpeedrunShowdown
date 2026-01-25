package com.github.speedrunshowdown;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.speedrunshowdown.LeagueBotApiManager.GSON;

public class PterodactylApiManager {

    private final SpeedrunShowdown plugin;

    public void restartServer() {
        String requestUrl = getRequestURL("power")+"?signal=restart";
        String jsonBody = "{\"signal\": \"restart\"}";
        handlePostAsync(requestUrl, jsonBody, null, response -> {
            plugin.getLogger().info(response.getAsString());
        });
    }

    public String getRequestURL(String type) {
        String url = plugin.getConfig().getString("pterodactyl_server_url");
        String serverId = plugin.getConfig().getString("pterodactyl_server_id");
        return url+"/api/client/servers/"+serverId+"/"+type;
    }

    public void handlePostAsync(String requestURL, String jsonBody, @Nullable CommandSender sender,
                                 @NotNull Consumer<JsonObject> responseHandler) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return post(requestURL, jsonBody, sender);
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(responseStr -> {
            Bukkit.getScheduler().runTask(SpeedrunShowdown.getInstance(), () -> {
                if (responseStr == null) return;
                try {
                    JsonObject response = GSON.fromJson(responseStr, JsonObject.class);
                    responseHandler.accept(response);
                } catch (Exception e) {
                    SpeedrunShowdown.getInstance().getServer().broadcastMessage(ChatColor.RED+
                            "FAILED TO HANDLE RESPONSE: " + e.getMessage());
                    SpeedrunShowdown.getInstance().getLogger().severe("FAILED TO HANDLE RESPONSE: " +
                            e.getMessage()+"\n"+responseStr);
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
                .header("Authorization", "Bearer "+plugin.getConfig().getString("pterodactyl_api_key"))
                .header("Accept", "Application/vnd.pterodactyl.v1+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

    private static @NotNull String getBufferedReader(HttpURLConnection con) throws IOException {
        int status = con.getResponseCode();
        Reader streamReader = null;
        if (status > 299) {
            streamReader = new InputStreamReader(con.getErrorStream());
        } else {
            streamReader = new InputStreamReader(con.getInputStream());
        }
        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    public PterodactylApiManager() {
        plugin = SpeedrunShowdown.getInstance();
    }
}
