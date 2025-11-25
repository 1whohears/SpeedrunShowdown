package com.github.speedrunshowdown;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LeagueBotApiManager {

    public static final Gson GSON = new Gson();

    private final SpeedrunShowdown plugin;

    public LeagueBotApiManager() {
        plugin = SpeedrunShowdown.getInstance();
    }

    public boolean createTeamMatch(CommandSender sender, String team1Name, String team2Name) {
        if (plugin.isRunning()) {
            sender.sendMessage(ChatColor.RED+"Game is already running!");
            return false;
        }
        if (team1Name.equals(team2Name)) {
            sender.sendMessage(ChatColor.RED+"Team Names can't be the same!");
            return false;
        }

        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team team1 = scoreboard.getTeam(team1Name);
        Team team2 = scoreboard.getTeam(team2Name);

        if (team1 == null) {
            sender.sendMessage(ChatColor.RED+team1Name+" does not exist!");
            return false;
        }
        if (team2 == null) {
            sender.sendMessage(ChatColor.RED+team2Name+" does not exist!");
            return false;
        }

        List<Player> team1Players = getPlayers(team1);
        List<Player> team2Players = getPlayers(team2);

        if (team1Players.isEmpty() || team2Players.isEmpty()) {
            sender.sendMessage(ChatColor.RED+"One or both teams are empty!");
            return false;
        }

        String leagueBotURL = getRequestURL("/league/createset/ingameteams");
        leagueBotURL += "&team1Name="+team1Name+"&team2Name="+team2Name;
        leagueBotURL += "&team1MCUUIDList="+getMCUUIDList(team1Players);
        leagueBotURL += "&team2MCUUIDList="+getMCUUIDList(team2Players);

        String responseStr = getResponse(leagueBotURL, sender);
        if (responseStr == null) return false;
        JsonObject response = GSON.fromJson(responseStr, JsonObject.class);

        List<Player> allPlayers = new ArrayList<>(team1Players);
        allPlayers.addAll(team2Players);

        if (response.has("error")) {
            String error = ChatColor.RED+response.get("error").getAsString();
            if (response.has("badUUID")) {
                String badUUID = response.get("badUUID").getAsString();
                Player badPlayer = getPlayerInList(badUUID, allPlayers);
                String name = "Unknown player";
                if (badPlayer != null) name = badPlayer.getName();
                error += " "+name;
            }
            sender.sendMessage(error);
            return false;
        }
        sender.sendMessage(ChatColor.GREEN+response.get("result").getAsString());

        int setId = response.get("set_id").getAsInt();
        sender.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE+"Set "+setId+" | "+team1+" vs "+team2
                +" | has been created and will begin shortly!");
        for (Player player : allPlayers) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        return true;
    }

    public boolean createAutoTeamMatch(CommandSender sender, List<Player> players,
                                       String team1Name, String team2Name) {
        if (plugin.isRunning()) {
            sender.sendMessage(ChatColor.RED+"Game is already running!");
            return false;
        }
        if (team1Name.equals(team2Name)) {
            sender.sendMessage(ChatColor.RED+"Team Names can't be the same!");
            return false;
        }
        if (players.size() < 2) {
            sender.sendMessage(ChatColor.RED+"Need at least 2 players!");
            return false;
        }
        String leagueBotURL = getRequestURL("/league/createset/autoteams");
        leagueBotURL += "&team1Name="+team1Name+"&team2Name="+team2Name;
        leagueBotURL += "&mcUUIDList="+getMCUUIDList(players);

        String responseStr = getResponse(leagueBotURL, sender);
        if (responseStr == null) return false;
        JsonObject response = GSON.fromJson(responseStr, JsonObject.class);

        if (response.has("error")) {
            String error = ChatColor.RED+response.get("error").getAsString();
            if (response.has("badUUID")) {
                String badUUID = response.get("badUUID").getAsString();
                Player badPlayer = getPlayerInList(badUUID, players);
                String name = "Unknown player";
                if (badPlayer != null) name = badPlayer.getName();
                error += " "+name;
            }
            sender.sendMessage(error);
            return false;
        }
        sender.sendMessage(ChatColor.GREEN+response.get("result").getAsString());

        int setId = response.get("set_id").getAsInt();
        JsonObject team1 = response.getAsJsonObject("team1");
        JsonObject team2 = response.getAsJsonObject("team2");

        handleTeamResponse(team1, players, team1Name);
        handleTeamResponse(team2, players, team2Name);

        sender.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE+"Set "+setId
                +" | "+team1.get("name").getAsString()+" vs "+team2.get("name").getAsString()
                +" | has been created and will begin shortly!");
        for (Player player : players) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        return true;
    }

    private void handleTeamResponse(JsonObject team, List<Player> players, String requestTeamName) {
        //String responseTeamName = team.get("name").getAsString();
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team mcTeam = scoreboard.getTeam(requestTeamName);
        if (mcTeam == null) return;
        JsonArray members = team.get("members").getAsJsonArray();
        for (int i = 0; i < members.size(); ++i) {
            JsonObject member = members.get(i).getAsJsonObject();
            //long id = member.get("id").getAsLong();
            String uuid = member.get("mcUUID").getAsString();
            Player player = getPlayerInList(uuid, players);
            if (player == null) {
                System.out.println("could not find player with uuid "+uuid);
                continue;
            }
            mcTeam.addEntity(player);
        }
    }

    public List<Player> getPlayers(Team team) {
        List<Player> players = new ArrayList<>();
        for (String name : team.getEntries()) {
            Player player = plugin.getServer().getPlayer(name);
            if (player != null) players.add(player);
        }
        return players;
    }

    private String getMCUUIDList(List<Player> players) {
        String mcUUIDList = "";
        for (Player player : players) mcUUIDList += player.getUniqueId() + ",";
        return mcUUIDList.substring(0, mcUUIDList.length()-1);
    }

    @Nullable
    private static Player getPlayerInList(String uuid, List<Player> players) {
        for (Player player : players)
            if (player.getUniqueId().toString().equals(uuid))
                return player;
        return null;
    }

    public boolean linkDiscordAccount(CommandSender sender, Player player, String discordUsername) {
        String leagueBotURL = getRequestURL("/league/link/minecraft/player");
        leagueBotURL += "&mcUUID="+player.getUniqueId()+"&discordUsername="+discordUsername;

        String responseStr = getResponse(leagueBotURL, sender);
        if (responseStr == null) return false;
        JsonObject response = GSON.fromJson(responseStr, JsonObject.class);

        if (response.has("error")) {
            sender.sendMessage(ChatColor.RED+response.get("error").getAsString());
            return false;
        }
        sender.sendMessage(ChatColor.GREEN+response.get("result").getAsString());

        return true;
    }

    public String getRequestURL(String type) {
        long guildId = plugin.getConfig().getLong("league_bot_guild_id");
        String leagueName = plugin.getConfig().getString("league_bot_league_name");
        String leagueBotURL = plugin.getConfig().getString("league_bot_url");
        leagueBotURL += type+"?guildId="+guildId+"&leagueName="+leagueName;
        return leagueBotURL;
    }

    @Nullable
    private static String getResponse(String requestURL, CommandSender sender) {
        System.out.println("GET: "+requestURL);
        String response;
        try {
            URL url = new URL(requestURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            response = getBufferedReader(con);
            con.disconnect();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED+"Failed: "+e.getMessage());
            e.printStackTrace();
            return null;
        }
        System.out.println("RESPONSE: "+response);
        return response;
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

}
