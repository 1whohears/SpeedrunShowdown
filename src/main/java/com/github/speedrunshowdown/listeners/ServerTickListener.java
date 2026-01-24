package com.github.speedrunshowdown.listeners;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.github.speedrunshowdown.SpeedrunShowdown;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ServerTickListener implements Listener {
    @EventHandler
    public void onServerTick(ServerTickEndEvent event) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        if (plugin.getServer().isStopping()) {
            ByteArrayDataOutput bado = ByteStreams.newDataOutput();
            bado.writeUTF("OFFLINE");
            plugin.getServer().sendPluginMessage(plugin, "srsdranked:from_gp/status", bado.toByteArray());
        }
    }
}
