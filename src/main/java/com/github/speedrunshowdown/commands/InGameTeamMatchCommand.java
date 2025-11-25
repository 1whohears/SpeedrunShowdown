package com.github.speedrunshowdown.commands;

import com.github.speedrunshowdown.SpeedrunShowdown;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class InGameTeamMatchCommand {
    public static LiteralCommandNode<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("createingameteammatch")
                .requires(sender -> sender.getSender().hasPermission("speedrunshowdown"))
                .then(Commands.argument("team1Name", StringArgumentType.word())
                        .suggests(getTeamSuggestions())
                        .then(Commands.argument("team2Name", StringArgumentType.word())
                                .suggests(getTeamSuggestions())
                                .executes(context -> {
                                    String team1Name = StringArgumentType.getString(context, "team1Name");
                                    String team2Name = StringArgumentType.getString(context, "team2Name");
                                    if (SpeedrunShowdown.getInstance().getLeagueBotApiManager().createTeamMatch(
                                            context.getSource().getSender(), team1Name, team2Name))
                                        return Command.SINGLE_SUCCESS;
                                    else return 0;
                                })
                )
        );
        return root.build();
    }

    public static SuggestionProvider<CommandSourceStack> getTeamSuggestions() {
        return (ctx, builder) -> {
            ScoreboardManager scoreboard = ctx.getSource().getSender().getServer().getScoreboardManager();
            scoreboard.getMainScoreboard().getTeams().stream().map(Team::getName).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
