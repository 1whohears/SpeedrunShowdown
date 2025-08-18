package com.github.speedrunshowdown.commands;

import com.github.speedrunshowdown.SpeedrunShowdown;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ProgressionPoints implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
                             @NotNull String s, @NotNull String @NotNull [] strings) {
        commandSender.sendMessage(ChatColor.YELLOW+"All Progression Points!");
        SpeedrunShowdown.getInstance().getProgressionPointManager().listAll(commandSender);
        return true;
    }
}
