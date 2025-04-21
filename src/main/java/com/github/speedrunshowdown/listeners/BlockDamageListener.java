package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

public class BlockDamageListener implements Listener {
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();

        // If plugin is running and spawners are indestructable
        // and player breaking spawner, prevent breaking
        if (plugin.isRunning() &&
            plugin.getConfig().getBoolean("indestructable-spawners") &&
            event.getBlock().getType() == Material.SPAWNER
        ) {
            BlockState state = event.getBlock().getState();
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                if (spawner.getSpawnedType() == EntityType.BLAZE) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Cannot break spawners!");
                }
            }
        }
    }
}