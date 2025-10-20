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
        if (plugin.isRunning() && event.getEntity().getType() == EntityType.ENDER_DRAGON) {
            DamageSource source = event.getDamageSource();
            Entity cause = source.getCausingEntity();
            if (cause == null) {
                plugin.getServer().sendPlainMessage(ChatColor.YELLOW + "The dragon died from an unknown cause. " +
                        "The winner may need to be decided manually!");
                return;
            }
            if (!(cause.getType() == EntityType.PLAYER)) {
                plugin.getServer().sendPlainMessage(ChatColor.YELLOW + "The dragon died from a non player entity." +
                        "The winner may need to be decided manually!");
                return;
            }
            Player player = (Player) cause;
            plugin.win(
                    plugin.getScoreboardManager().getTeam(player),
                    player.getName() + " killed the dragon!"
            );
        }
    }
}
