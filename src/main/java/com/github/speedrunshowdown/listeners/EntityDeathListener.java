package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.ChatColor;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (plugin.isRunning() && event.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            DamageSource source = event.getDamageSource();
            Entity cause = source.getCausingEntity();
            Player player;
            if (cause == null) {
                if (plugin.getBedExplodeInEndTick() == plugin.getServer().getCurrentTick()
                        && plugin.getBedExplodeInEndPlayer() != null) {
                    player = plugin.getBedExplodeInEndPlayer();
                } else {
                    plugin.getServer().sendPlainMessage(ChatColor.YELLOW + "The dragon died from an unknown cause. " +
                            "The winner may need to be decided manually!");
                    return;
                }
            } else if (cause.getType().equals(EntityType.PLAYER)) {
                player = (Player) cause;
            } else {
                plugin.getServer().sendPlainMessage(ChatColor.YELLOW + "The dragon died from a non player entity." +
                        "The winner may need to be decided manually!");
                return;
            }
            plugin.win(
                    plugin.getScoreboardManager().getTeam(player),
                    player.getName() + " killed the dragon!"
            );
        }
    }
}
