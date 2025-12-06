package com.github.speedrunshowdown.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class VerifyNetherStructuresCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] strings) {
        if (StartCommand.verifyNetherStructures()) {
            sender.getServer().broadcastMessage(ChatColor.GREEN
                    +"The Nether has at least 1 Fortress and at least 1 Bastian inside the world border!");
        } else {
            sender.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE
                    +"The Nether is missing a required Structure! Please use a different seed!");
        }
        return false;
    }
}
