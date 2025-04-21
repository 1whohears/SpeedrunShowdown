package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureSpawnListener implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (plugin.isRunning() && event.getEntityType() == EntityType.PIGLIN_BRUTE) {
            event.setCancelled(true);
        }
    }

}
