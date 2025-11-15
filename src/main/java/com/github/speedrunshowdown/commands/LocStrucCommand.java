package com.github.speedrunshowdown.commands;

import com.github.speedrunshowdown.SpeedrunShowdown;
import com.github.speedrunshowdown.border.WorldBorderManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.util.StructureSearchResult;
import org.jetbrains.annotations.Nullable;

public class LocStrucCommand implements CommandExecutor {

    public static final int CHUNKS_TO_CHECK = 4000;
    public static final int STRUCTURE_VERIFY_RANGE = 250;

    private final Structure structure;
    private final String structure_name;

    public LocStrucCommand(Structure structure, String structure_name) {
        this.structure = structure;
        this.structure_name = structure_name;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
            sender.sendMessage(ChatColor.RED + "You must be in the nether to use this command!");
            return true;
        }
        Score score = null;
        if (SpeedrunShowdown.getInstance().isRunning()) {
            Objective nether_time_objective = player.getScoreboard().getObjective("nether_time");
            if (nether_time_objective == null) {
                sender.sendMessage(ChatColor.RED + "The nether_time objective doesn't exist yet. Has the game started?");
                return true;
            }
            score = nether_time_objective.getScore(player.getName());
            int time = score.getScore();
            int min_time = SpeedrunShowdown.getInstance().getConfig().getInt("unlock_locate_nether_time", 480);
            if (time < min_time) {
                sender.sendMessage(ChatColor.RED + "You must be in the nether for "
                        + (min_time - time) + " more seconds to use this command!");
                return true;
            }
        }
        // for some reason locateNearestStructure gives different results every time
        // and the boolean flag also gives different results
        // so this min sort loop is needed to find the actual closest bastion within a world border
        StructureSearchResult result = multiFindStructure(player, structure);
        if (result == null) {
            player.sendMessage(ChatColor.RED + "No "+structure_name+"s were Found Inside the World Border.");
            return true;
        }
        if (score != null) score.setScore(0);
        player.sendMessage(ChatColor.GREEN + "Nearest "+structure_name+": (X:"
                +(int)result.getLocation().getX()+",Z:"+(int)result.getLocation().getZ()+") "
                +(int)result.getLocation().distance(player.getLocation())+" Blocks Away!");
        return true;
    }
    @Nullable
    public static StructureSearchResult multiFindStructure(Player player, Structure structure) {
        return multiFindStructure(player.getWorld(), player.getLocation(), structure);
    }
    @Nullable
    public static StructureSearchResult multiFindStructure(World world, Location playerLocation, Structure structure) {
        boolean worldbordercheck = SpeedrunShowdown.getInstance().getConfig().getBoolean("world-border");
        StructureSearchResult result = findStructure(world, playerLocation, playerLocation, worldbordercheck, structure);
        if (result == null && worldbordercheck) {
            // if nothing was found still, look for structures from different locations
            // cause apparently that also makes different results
            result = findStructure(world, playerLocation, new Location(world,
                    184, 60, 184), true, structure);
            if (result == null)
                result = findStructure(world, playerLocation, new Location(world,
                        -248, 60, -248), true, structure);
            if (result == null)
                result = findStructure(world, playerLocation, new Location(world,
                        184, 60, -248), true, structure);
            if (result == null)
                result = findStructure(world, playerLocation, new Location(world,
                        -248, 60, 184), true, structure);
        }
        return result;
    }
    public static boolean checkInWorldBorder(Location location) {
        return checkInRange(location, WorldBorderManager.NETHER_BORDER_SIZE / 2);
    }
    public static boolean checkInRange(Location location, int radius) {
        return location.getX() < radius && location.getX() > -radius &&
                location.getZ() < radius && location.getZ() > -radius;
    }
    @Nullable
    public static StructureSearchResult findStructure(Player player, Location lookPos,
                                                      boolean worldbordercheck, Structure structure) {
        return findStructure(player.getWorld(), player.getLocation(), lookPos, worldbordercheck, structure);
    }
    @Nullable
    public static StructureSearchResult findStructure(World world, Location playerLoc, Location lookPos,
                                                      boolean worldbordercheck, Structure structure) {
        StructureSearchResult result = null;
        double mindist = Double.MAX_VALUE;
        for (int i = 0; i < 10; ++i) {
            StructureSearchResult r1 = world.locateNearestStructure(
                    lookPos, structure, CHUNKS_TO_CHECK, true);
            if (r1 != null) {
                double d = r1.getLocation().distanceSquared(playerLoc);
                if (d < mindist && (!worldbordercheck || checkInWorldBorder(r1.getLocation()))) {
                    result = r1;
                    mindist = d;
                }
            }
            StructureSearchResult r2 = world.locateNearestStructure(
                    lookPos, structure, CHUNKS_TO_CHECK, false);
            if (r2 != null) {
                double d = r2.getLocation().distanceSquared(playerLoc);
                if (d < mindist && (!worldbordercheck || checkInWorldBorder(r2.getLocation()))) {
                    result = r2;
                    mindist = d;
                }
            }
        }
        return result;
    }
}
