package com.github.speedrunshowdown.listeners;

import com.github.speedrunshowdown.SpeedrunShowdown;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class SRSDPluginMessageListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (channel.equals("srsdranked:to_gp/set_queue")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            int lobbyId = in.readInt();
            int queueId = in.readInt();
            if (lobbyId != SpeedrunShowdown.getInstance().getGameplayServerId()) return;
            SpeedrunShowdown.getInstance().getLeagueBotApiManager().setCurrentQueueId(queueId);
        } else if (channel.equals("srsdranked:to_gp/reset_seed")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            int lobbyId = in.readInt();
            if (lobbyId != SpeedrunShowdown.getInstance().getGameplayServerId()) return;
            SpeedrunShowdown.getInstance().resetSeed();
        }
    }
}
