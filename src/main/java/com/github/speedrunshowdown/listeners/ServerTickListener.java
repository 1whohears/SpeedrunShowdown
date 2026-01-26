package com.github.speedrunshowdown.listeners;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ServerTickListener implements Listener {
    @EventHandler
    public void onServerTick(ServerTickEndEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (plugin.getServer().isStopping()) {
            plugin.getInternalApiManager().sendStatus("OFFLINE");
        }
    }
}
