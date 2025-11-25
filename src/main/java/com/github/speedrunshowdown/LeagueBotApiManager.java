package com.github.speedrunshowdown;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LeagueBotApiManager {

    private final SpeedrunShowdown plugin;

    public LeagueBotApiManager() {
        plugin = SpeedrunShowdown.getInstance();
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

        long guildId = plugin.getConfig().getLong("league_bot_guild_id");
        String leagueName = plugin.getConfig().getString("league_bot_league_name");
        String leagueBotURL = plugin.getConfig().getString("league_bot_url");
        leagueBotURL += "/league/createset/autoteams?guildId="+guildId+"&leagueName="+leagueName;
        leagueBotURL += "&team1Name="+team1Name+"&team2Name="+team2Name;

        String mcUUIDList = "";
        for (Player player : players) mcUUIDList += player.getUniqueId() + ",";
        mcUUIDList = mcUUIDList.substring(0, mcUUIDList.length()-1);
        leagueBotURL += "&mcUUIDList="+mcUUIDList;

        String response;
        try {
            URL url = new URL(leagueBotURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(1000);
            con.setReadTimeout(1000);
            response = getBufferedReader(con);
            con.disconnect();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED+"Failed: "+e.getMessage());
            e.printStackTrace();
            return false;
        }

        sender.sendMessage(ChatColor.YELLOW+response);

        return true;
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
