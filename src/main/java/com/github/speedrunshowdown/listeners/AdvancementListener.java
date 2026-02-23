package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {
    @EventHandler
    public void onAdvancementObtained(PlayerAdvancementDoneEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (plugin.isRunning()) {
            // If should hide spectator advancements, turn announce advancements gamerule off and schedule it to return to its original value
            if (plugin.getConfig().getBoolean("hide-spectator-advancements")) {
                final boolean announceAdvancements = event.getPlayer().getWorld().getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES);
                if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                    event.getPlayer().getWorld().setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        event.getPlayer().getWorld().setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, announceAdvancements);
                    }, 1L);
                }
            }

            // If should hide player advancements, turn announce advancements gamerule off and schedule it to return to its original value
            if (plugin.getConfig().getBoolean("hide-player-advancements")) {
                final boolean announceAdvancements = event.getPlayer().getWorld().getGameRuleValue(GameRules.SHOW_ADVANCEMENT_MESSAGES);
                event.getPlayer().getWorld().setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    event.getPlayer().getWorld().setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, announceAdvancements);
                }, 1L);
            }

            plugin.getProgressionPointManager().onAdvancement(event.getAdvancement(), event.getPlayer());
        }
    }

}
