package com.github.speedrunshowdown;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

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
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        });
    }

    public int getNumPoints(@Nonnull String teamName) {
        int num = 0;
        for (String tn : pointTeamMap.values()) if (teamName.equals(tn)) ++num;
        return num;
    }

    public void onGameEnd() {
        for (String tn : pointTeamMap.values()) {
            int points = getNumPoints(tn);
            plugin.getServer().broadcastMessage("Team "+tn+" has "+points+" Progression Points!");
        }
    }

    public void listAll(CommandSender sender) {
        for (String k : Constants.PROGRESSION_POINTS) {
            TextColor color = TextColor.color(0xFFFFFF);
            Component teamName = Component.empty();
            if (pointTeamMap.containsKey(k)) {
                String teamId = pointTeamMap.get(k);
                Team team = plugin.getScoreboardManager().getScoreboard().getTeam(teamId);
                if (team != null) {
                    color = team.color();
                    teamName = team.displayName();
                } else {
                    teamName = Component.text(teamId);
                }
            }
            sender.sendMessage(getAdvancementTitleComponent(k).color(color)
                    .append(Component.text(" ")).append(teamName));
        }
    }

    public static Component getAdvancementTitleComponent(String keyString) {
        NamespacedKey key = NamespacedKey.fromString(keyString);
        if (key == null) return Component.text(keyString); // fallback

        String translationKey = "advancements." + key.getKey().replace('/', '.') + ".title";
        return Component.translatable(translationKey);
    }
}
