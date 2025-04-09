package com.github.speedrunshowdown;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProgressionPointsManager {

    private final SpeedrunShowdown plugin;
    private final Map<String,String> pointTeamMap = new HashMap<>();

    public ProgressionPointsManager() {
        plugin = SpeedrunShowdown.getInstance();
    }

    public void init() {
        pointTeamMap.clear();
    }

    public static boolean isProgressionPoint(Advancement adv) {
        String key = adv.getKey().toString();
        for (String k : Constants.PROGRESSION_POINTS) if (k.equals(key)) return true;
        return false;
    }

    public void onAdvancement(Advancement advancement, Player player) {
        if (!isProgressionPoint(advancement)) return;
        String key = advancement.getKey().toString();
        if (pointTeamMap.containsKey(key)) return;
        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) return;
        String teamName = team.getName();
        pointTeamMap.put(key, teamName);
        int points = getNumPoints(teamName);
        plugin.getServer().broadcastMessage("Team "+teamName+" now has "+points+" Progression Points!");
    }

    public int getNumPoints(@Nonnull String teamName) {
        int num = 0;
        for (String tn : pointTeamMap.values()) if (teamName.equals(tn)) ++num;
        return num;
    }

    public void onGameEnd() {
        Set<String> teamNames = new HashSet<>(pointTeamMap.values());
        for (String tn : teamNames) {
            int points = getNumPoints(tn);
            plugin.getServer().broadcastMessage("Team "+tn+" has "+points+" Progression Points!");
        }
    }
}
