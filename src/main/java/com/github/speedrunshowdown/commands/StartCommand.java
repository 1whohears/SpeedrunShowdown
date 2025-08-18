package com.github.speedrunshowdown.commands;

import com.github.speedrunshowdown.SpeedrunShowdown;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.NotNull;

public class StartCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();

        // If plugin is running, give warning
        if (plugin.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Game already running!");
        }
        // Else, schedule tasks to show titles to players
        else {
            if (!verifyNetherStructures()) {
                plugin.getServer().broadcastMessage(ChatColor.RED+"The Nether is missing a required Structure! " +
                        "Please use a different seed!");
                return true;
            }
            startCountdown(sender);
        }

        return true;
    }

    public static boolean verifyNetherStructures() {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();
        World nether = plugin.getTheNether();
        boolean fortress = checkNetherStructure(nether, Structure.FORTRESS);
        boolean bastian = checkNetherStructure(nether, Structure.BASTION_REMNANT);
        if (!fortress) {
            plugin.getServer().broadcastMessage(ChatColor.RED+"There is no Nether Fortress within the world boarder!");
        }
        if (!bastian) {
            plugin.getServer().broadcastMessage(ChatColor.RED+"There is no Bastian Remnant within the world boarder!");
        }
        return fortress && bastian;
    }

    private static boolean checkNetherStructure(World nether, Structure structure) {
        StructureSearchResult result = LocStrucCommand.multiFindStructure(nether,
                new Location(nether, 0, 60, 0), structure);
        return result != null;
    }

    public void startCountdown(CommandSender sender) {
        SpeedrunShowdown plugin = SpeedrunShowdown.getInstance();

        sender.sendMessage(ChatColor.GREEN + "Countdown started!");
        sendStartingTimerTile(ChatColor.YELLOW + "Game starting soon...", false);

        int countdownTime = plugin.getConfig().getInt("countdown-time");
        for (int i = 0; i <= countdownTime; i++) {
            final int seconds = i;
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                // If no seconds left, give starting message and start
                if (seconds == 0) {
                    sendStartingTimerTile(ChatColor.GREEN + "Go!", true);
                    plugin.start();
                }
                // Else, display seconds left
                else {
                    sendStartingTimerTile(ChatColor.YELLOW + "Starting in " + seconds + "...", false);
                }
            }, 30L + (countdownTime - i) * 30L);
        }
    }

    public void sendStartingTimerTile(String subtitle, boolean higherDing) {
        float pitch = 1;
        if (higherDing) {
            pitch = 2;
        }
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, pitch);
            player.sendTitle(ChatColor.YELLOW + ChatColor.BOLD.toString() + "SPEEDRUN SHOWDOWN", subtitle, 0, 60, 10);
        }
    }
}
