package com.github.speedrunshowdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
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
        if (bedLocation == null) return;
        if (!(bedLocation.getBlock().getState() instanceof Bed)) return;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            player.teleport(bedLocation.add(0, 0.5, 0));
            plugin.getServer().broadcastMessage(ChatColor.RED+"The Game Thinks your Bed was Obstructed");
            plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE+"This Fail Safe Only Works ONCE!");
            plugin.getServer().broadcastMessage(ChatColor.DARK_PURPLE+"RESET YOUR SPAWN!!!");
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1, 1);
        }, 20);
    }

}
