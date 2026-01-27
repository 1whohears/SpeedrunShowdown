package com.github.speedrunshowdown;

import com.github.speedrunshowdown.commands.StartCommand;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LeagueBotApiManager {

    public static final Gson GSON = new Gson();
    public static final Random RANDOM = new Random();
    private static final String[] TEAMS = new String[] {
        "redstone", "crimson",
        "emerald", "slime",
        "lapis", "diamond",
        "gold", "glowstone",
        "purpur", "chorus"
    };

    private final SpeedrunShowdown plugin;

    private QueueState queueState = QueueState.NONE;
    private int currentQueueId = -1;
    private int currentSetId = -1;
    private String player1UUID = "";
    private String player2UUID = "";
    private String player1Team = "";
    private String player2Team = "";
    private boolean matchReported = false;

    private final @NotNull Consumer<JsonObject> queueResponseHandler = response -> {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (response.has("error")) {
            plugin.getServer().broadcastMessage(ChatColor.RED + response.get("error").getAsString());
            return;
        }
        JsonObject queueData = response.getAsJsonObject("queue");
        QueueState queueState = readQueueState(queueData.get("queueState").getAsString());
        plugin.getLeagueBotApiManager().queueState = queueState;
        if (queueState == QueueState.CLOSED) {
            if (currentSetId == -1) {
                currentSetId = queueData.get("resolvedSetId").getAsInt();
                handleSetResponse(res -> {
                    JsonObject con1Data = res.getAsJsonObject("contestant1");
                    JsonObject con2Data = res.getAsJsonObject("contestant2");
                    String team1Name = randomTeam(null);
                    String team2Name = randomTeam(team1Name);
                    String player1UUID = handleContestantResponse(team1Name, con1Data);
                    String player2UUID = handleContestantResponse(team2Name, con2Data);
                    if (player1UUID == null || player2UUID == null) {
                        plugin.getServer().broadcastMessage(ChatColor.RED+"Could not start match because " +
                                "there is a player that does not have a linked discord account!");
                        return;
                    }
                    plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "Set " + currentSetId
                            + " | " + team1Name + " vs " + team2Name
                            + " | has been created and will begin shortly!");
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                    setCurrentSetParameters(currentSetId, player1UUID, player2UUID, team1Name, team2Name);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> StartCommand.startCountdown(null), 200);
                });
            }
        }
    };

    @Nullable
    private String handleContestantResponse(String mcTeamName, JsonObject conData) {
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team mcTeam = scoreboard.getTeam(mcTeamName);
        if (mcTeam == null) return null;
        String type = conData.get("type").getAsString();
        if (type.equals("INDIVIDUAL")) {
            return handleUser(mcTeam, conData);
        } else if (type.equals("TEAM")) {
            String uuid = null;
            JsonArray teamMembers = conData.getAsJsonArray("team_members");
            for (int i = 0; i < teamMembers.size(); ++i) {
                String id = handleUser(mcTeam, teamMembers.get(i).getAsJsonObject());
                if (id == null) return null;
                uuid = id;
            }
            return uuid;
        }
        return null;
    }

    @Nullable
    private String handleUser(@NotNull Team mcTeam, JsonObject userData) {
        JsonObject extraData = userData.getAsJsonObject("extra_data");
        if (extraData.has("mcUUID")) {
            String uuid = extraData.get("mcUUID").getAsString();
            Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));
            if (player == null) return null;
            mcTeam.addEntity(player);
            return uuid;
        }
        return null;
    }

    public LeagueBotApiManager() {
        plugin = SpeedrunShowdown.getInstance();
    }

    public void queueUpdate() {
        if (currentQueueId == -1) return;
        handleQueueResponse(queueResponseHandler);
    }

    public void handleContestantResponse(long contestantId, @NotNull Consumer<JsonObject> responseHandler) {
        String leagueBotURL = getRequestURL("/league/info/contestant");
        leagueBotURL += "&contestantId="+contestantId+"&includeFullTeam=true";
        handleResponseAsync(leagueBotURL, null, responseHandler);
    }

    public void handleSetResponse(@NotNull Consumer<JsonObject> responseHandler) {
        if (currentSetId == -1) return;
        String leagueBotURL = getRequestURL("/league/info/set");
        leagueBotURL += "&setId="+currentSetId+"&includeContestants=true&includeFullTeam=true";
        handleResponseAsync(leagueBotURL, null, responseHandler);
    }

    public void handleQueueResponse(@NotNull Consumer<JsonObject> responseHandler) {
        if (currentQueueId == -1) return;
        String leagueBotURL = getRequestURL("/league/queue/state");
        leagueBotURL += "&queueId="+currentQueueId;
        handleResponseAsync(leagueBotURL, null, responseHandler);
    }

    public boolean reportMatch(String winningTeamName, int winningScore, int losingScore) {
        if (matchReported) {
            plugin.getServer().broadcastMessage(ChatColor.RED
                    +"Already reported set "+currentSetId);
            return false;
        }
        if (!plugin.isRunning()) {
            plugin.getServer().broadcastMessage(ChatColor.RED
                    +"Failed to report a match. Game is not running!");
            return false;
        }
        int setId = getCurrentSetId();
        if (setId == -1) {
            plugin.getServer().broadcastMessage(ChatColor.RED
                    +"Failed to report a match. Plugin does not know the current set id!");
            return false;
        }
        int score1, score2;
        if (winningScore == losingScore) {
            score1 = winningScore;
            score2 = losingScore;
        } else if (winningTeamName.equals(player1Team)) {
            score1 = winningScore;
            score2 = losingScore;
        } else if (winningTeamName.equals(player2Team)) {
            score1 = losingScore;
            score2 = winningScore;
        } else {
            plugin.getServer().broadcastMessage(ChatColor.RED
                    +"Failed to report a match. This team was not registered in match "+setId);
            return false;
        }

        String leagueBotURL = getRequestURL("/league/reportadmin");
        leagueBotURL += "&setId="+setId+"&updateRanks=true";
        leagueBotURL += "&player1UUID="+player1UUID+"&player2UUID="+player2UUID;
        leagueBotURL += "&player1Score="+score1+"&player2Score="+score2;

        handleResponseAsync(leagueBotURL, null, response -> {
            if (response.has("error")) {
                plugin.getServer().broadcastMessage(ChatColor.RED + response.get("error").getAsString());
                return;
            }
            plugin.getServer().broadcastMessage(ChatColor.GREEN + response.get("result").getAsString());
            matchReported = true;
        });

        return true;
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

        handleResponseAsync(leagueBotURL, sender, response -> {
            List<Player> allPlayers = new ArrayList<>(team1Players);
            allPlayers.addAll(team2Players);

            if (response.has("error")) {
                String error = ChatColor.RED + response.get("error").getAsString();
                if (response.has("badUUID")) {
                    String badUUID = response.get("badUUID").getAsString();
                    Player badPlayer = getPlayerInList(badUUID, allPlayers);
                    String name = "Unknown player";
                    if (badPlayer != null) name = badPlayer.getName();
                    error += " " + name;
                }
                sender.sendMessage(error);
                return;
            }
            sender.sendMessage(ChatColor.GREEN + response.get("result").getAsString());

            int setId = response.get("set_id").getAsInt();
            sender.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "Set " + setId
                    + " | " + team1.getName() + " vs " + team2.getName()
                    + " | has been created and will begin shortly!");
            for (Player player : allPlayers) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            setCurrentSetParameters(setId,
                    team1Players.getFirst().getUniqueId() + "",
                    team2Players.getFirst().getUniqueId() + "",
                    team1Name, team2Name
            );
        });

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

        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        if (scoreboard.getTeam(team1Name) == null) {
            sender.sendMessage(ChatColor.RED+team1Name+" does not exist!");
            return false;
        }
        if (scoreboard.getTeam(team2Name) == null) {
            sender.sendMessage(ChatColor.RED+team2Name+" does not exist!");
            return false;
        }

        String leagueBotURL = getRequestURL("/league/createset/autoteams");
        leagueBotURL += "&team1Name="+team1Name+"&team2Name="+team2Name;
        leagueBotURL += "&mcUUIDList="+getMCUUIDList(players);

        handleResponseAsync(leagueBotURL, sender, response -> {
            if (response.has("error")) {
                String error = ChatColor.RED + response.get("error").getAsString();
                if (response.has("badUUID")) {
                    String badUUID = response.get("badUUID").getAsString();
                    Player badPlayer = getPlayerInList(badUUID, players);
                    String name = "Unknown player";
                    if (badPlayer != null) name = badPlayer.getName();
                    error += " " + name;
                }
                sender.sendMessage(error);
                return;
            }
            sender.sendMessage(ChatColor.GREEN + response.get("result").getAsString());

            int setId = response.get("set_id").getAsInt();
            JsonObject team1 = response.getAsJsonObject("team1");
            JsonObject team2 = response.getAsJsonObject("team2");

            String player1UUID = handleTeamResponse(team1, players, team1Name);
            String player2UUID = handleTeamResponse(team2, players, team2Name);

            sender.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "Set " + setId
                    + " | " + team1.get("name").getAsString() + " vs " + team2.get("name").getAsString()
                    + " | has been created and will begin shortly!");
            for (Player player : players) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            setCurrentSetParameters(setId, player1UUID, player2UUID, team1Name, team2Name);
        });

        return true;
    }

    private String handleTeamResponse(JsonObject team, List<Player> players, String requestTeamName) {
        //String responseTeamName = team.get("name").getAsString();
        Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        Team mcTeam = scoreboard.getTeam(requestTeamName);
        if (mcTeam == null) return "";
        String player1UUID = "";
        JsonArray members = team.get("members").getAsJsonArray();
        for (int i = 0; i < members.size(); ++i) {
            JsonObject member = members.get(i).getAsJsonObject();
            //long id = member.get("id").getAsLong();
            String uuid = member.get("mcUUID").getAsString();
            Player player = getPlayerInList(uuid, players);
            if (player == null) continue;
            mcTeam.addEntity(player);
            if (player1UUID.isEmpty()) player1UUID = uuid;
        }
        return player1UUID;
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

    public boolean linkDiscordAccount(CommandSender sender, Player player, int linkCode) {
        String leagueBotURL = getRequestURL("/league/link/minecraft/player");
        leagueBotURL += "&mcUUID="+player.getUniqueId()+"&linkCode="+linkCode;

        handleResponseAsync(leagueBotURL, sender, response -> {
            if (response.has("error")) {
                sender.sendMessage(ChatColor.RED+response.get("error").getAsString());
                return;
            }
            sender.sendMessage(ChatColor.GREEN+response.get("result").getAsString());
        });

        return true;
    }

    public String getRequestURL(String type) {
        String apikey = plugin.getConfig().getString("league_bot_api_key");
        long guildId = plugin.getConfig().getLong("league_bot_guild_id");
        String leagueName = plugin.getConfig().getString("league_bot_league_name");
        String leagueBotURL = plugin.getConfig().getString("league_bot_url");
        leagueBotURL += type+"?apikey="+apikey+"&guildId="+guildId+"&leagueName="+leagueName;
        return leagueBotURL;
    }

    public static void handleResponseAsync(String requestURL, @Nullable CommandSender sender,
                                           @NotNull Consumer<JsonObject> responseHandler) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return getResponse(requestURL, sender);
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
    private static String getResponse(String requestURL, @Nullable CommandSender sender) {
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
            Bukkit.getScheduler().runTask(SpeedrunShowdown.getInstance(), () -> {
                String msg = ChatColor.RED + "Failed: " + e.getMessage();
                if (sender != null) sender.sendMessage(msg);
                else SpeedrunShowdown.getInstance().getServer().broadcastMessage(msg);
            });
            SpeedrunShowdown.getInstance().getLogger().severe(requestURL+" | "+e.getMessage());
            e.printStackTrace();
            return null;
        }
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

    public int getCurrentSetId() {
        return currentSetId;
    }

    public boolean setCurrentSetParameters(int id, String player1UUID, String player2UUID,
                                           String player1Team, String player2Team) {
        if (plugin.isRunning()) return false;
        if (currentQueueId != -1) return false;
        this.currentSetId = id;
        this.player1UUID = player1UUID;
        this.player2UUID = player2UUID;
        this.player1Team = player1Team;
        this.player2Team = player2Team;
        matchReported = false;
        return true;
    }

    public boolean isMatchReported() {
        return matchReported;
    }

    public String getPlayer1Team() {
        return player1Team;
    }

    public String getPlayer2Team() {
        return player2Team;
    }

    public int getCurrentQueueId() {
        return currentQueueId;
    }

    public void setCurrentQueueId(int currentQueueId) {
        this.currentQueueId = currentQueueId;
    }

    public enum QueueState {
        NONE,
        ENROLL,
        FINAL_ENROLL_TICK,
        PREGAME,
        PREGAME_SUBS,
        FINAL_PREGAME_TICK,
        CLOSED
    }

    public static QueueState readQueueState(String name) {
        return QueueState.valueOf(name);
    }

    public QueueState getQueueState() {
        return queueState;
    }

    public static String randomTeam(@Nullable String exclude) {
        int num = TEAMS.length;
        if (exclude == null) return TEAMS[RANDOM.nextInt(num)];
        int excludeIndex = -1;
        for (int i = 0; i < TEAMS.length; ++i) {
            if (TEAMS[i].equals(exclude)) {
                excludeIndex = i;
                break;
            }
        }
        while (true) {
            int index = RANDOM.nextInt(num);
            String name = TEAMS[index];
            if (excludeIndex != -1) {
                if (excludeIndex % 2 == 0 && index == excludeIndex+1) continue;
                else if (excludeIndex % 2 == 1 && index == excludeIndex-1) continue;
            }
            if (name.equals(exclude)) continue;
            return name;
        }
    }

}
