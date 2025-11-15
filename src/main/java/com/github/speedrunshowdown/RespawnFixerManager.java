package com.github.speedrunshowdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Bed;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RespawnFixerManager {

    private final SpeedrunShowdown plugin;
    private final Map<String, Location> respawnOverrides = new HashMap<>();

    public RespawnFixerManager() {
        plugin = SpeedrunShowdown.getInstance();
    }

    public void init() {
        respawnOverrides.clear();
    }

    public void onSetBedSpawn(Player player, @Nullable Location bedLocation) {
        if (bedLocation == null) return;
        respawnOverrides.put(player.getName(), bedLocation);
    }

    public void onPlayerRespawn(Player player) {
        Location bedLocation = respawnOverrides.get(player.getName());
        if (bedLocation == null) {
            plugin.getServer().broadcastMessage(ChatColor.YELLOW+" Player "+player.getName()
                    +" respawn bed location null");
            return;
        }
        if (!(bedLocation.getBlock().getState() instanceof Bed)) {
            plugin.getServer().broadcastMessage(ChatColor.YELLOW+" Player "+player.getName()
                    +" respawn bed location "+bedLocation+" NOT bed");
            return;
        }
        plugin.getServer().broadcastMessage(ChatColor.GREEN+" Player "+player.getName()
                +" respawn bed location "+bedLocation+" IS bed, but bed obstructed??? Teleporting...");
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.teleport(bedLocation);
        }, 20);
    }

}
