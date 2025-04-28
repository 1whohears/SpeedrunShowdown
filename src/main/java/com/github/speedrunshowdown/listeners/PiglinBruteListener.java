package com.github.speedrunshowdown.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class PiglinBruteListener implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.PIGLIN_BRUTE) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getEntityType() == EntityType.PIGLIN_BRUTE) {
            event.getEntity().remove();
        }
    }

}
