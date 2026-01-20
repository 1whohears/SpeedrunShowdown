package com.github.speedrunshowdown.commands;

import com.github.speedrunshowdown.SpeedrunShowdown;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LinkDiscordCommand {
    public static LiteralCommandNode<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("linkdiscord")
                .requires(sender -> sender.getSender().hasPermission("netherlocate"))
                .then(Commands.argument("linkCode", IntegerArgumentType.integer(0, 999999))
                                .executes(context -> {
                                    int linkCode = IntegerArgumentType.getInteger(context, "linkCode");
                                    if (!(context.getSource().getExecutor() instanceof Player)) {
                                        context.getSource().getSender().sendMessage(ChatColor.RED+"Executor must be a player!");
                                        return 0;
                                    }
                                    Player player = (Player) context.getSource().getExecutor();
                                    if (SpeedrunShowdown.getInstance().getLeagueBotApiManager().linkDiscordAccount(
                                            context.getSource().getSender(), player, linkCode))
                                        return Command.SINGLE_SUCCESS;
                                    else return 0;
                                })
                );
        return root.build();
    }
}
