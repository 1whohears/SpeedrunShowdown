package com.github.speedrunshowdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Bed;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RespawnFixerManager {

    private final SpeedrunShowdown plugin;
    private final Map<String, Location> respawnOverrides = new HashMap<>();
    private final Map<String, Long> resetKBResMap = new HashMap<>();

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
            player.sendMessage(ChatColor.RED+"The Game Thinks your Bed was Obstructed");
            player.sendMessage(ChatColor.LIGHT_PURPLE+"This Fail Safe Only Works ONCE!");
            player.sendMessage(ChatColor.DARK_PURPLE+"RESET YOUR SPAWN!!!");
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1, 1);
        }, 20);
    }

    public void tempDisableKB(@NotNull Player player) {
        player.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        resetKBResMap.put(player.getName(), player.getWorld().getGameTime());
    }

    public void tick() {
        int kbResTicks = plugin.getConfig().getInt("portal-invincibility") * 20;
        resetKBResMap.forEach((name, startTime) -> {
            Player player = plugin.getServer().getPlayer(name);
            if (player == null) return;
            long currentTime = player.getWorld().getGameTime();
            if (currentTime - startTime <= kbResTicks) return;
            player.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        });
    }

}
