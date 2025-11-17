package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceBlockListener implements Listener {

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (!plugin.isRunning()) return;
        Location location = event.getBlockPlaced().getLocation();
        if (location.getWorld().getEnvironment() != World.Environment.THE_END) return;
        double height = plugin.getConfig().getDouble("end_high_block_stop_height", 100);
        if (location.getBlockY() < height) return;
        Location center = new Location(location.getWorld(), 0, location.getY(), 0);
        if (location.distance(center) > plugin.getConfig().getDouble("end_high_block_stop_radius", 24)) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED+"Blocks cannot be placed above Y level "+height+" above the fountain!");
    }

}
