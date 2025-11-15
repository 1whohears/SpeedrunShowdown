package com.github.speedrunshowdown.listeners;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SetSpawnListener implements Listener {

    @EventHandler
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getCause() != PlayerSetSpawnEvent.Cause.BED) return;
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (!plugin.isRunning()) return;
        plugin.getRespawnFixerManager().onSetBedSpawn(event.getPlayer(), event.getLocation());
    }

}
