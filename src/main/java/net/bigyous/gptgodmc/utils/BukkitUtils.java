package net.bigyous.gptgodmc.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.bigyous.gptgodmc.GPTGOD;

public class BukkitUtils {
    // converts seconds into ticks
    public static long secondsToTicks(long seconds) {
        return seconds * 20;
    }

    // checks if block is one of a few different blocktypes which players can be
    // inside of
    public static boolean isBlockBreathable(Block block) {
        return
        // block is passable but not liquid (might be too permissive?)
        (block.isPassable() && !block.isLiquid())
                // or the block is a cobweb, ladder, or vine
                || block.getType().equals(Material.COBWEB)
                || block.getType().equals(Material.LADDER)
                || block.getType().equals(Material.VINE);
    }

    public static boolean testBlocks(Location loc, boolean ignoreWater) {
        Location underplayer = new Location(loc.getWorld(), loc.getBlockX(), (loc.getBlockY() - 1), loc.getBlockZ());// Block
                                                                                                                     // under
                                                                                                                     // player
        Location topblock = new Location(loc.getWorld(), loc.getBlockX(), (loc.getBlockY() + 1), loc.getBlockZ());// player
                                                                                                                  // location
                                                                                                                  // top

        // no liquid that is lava
        boolean belowSafe = !(underplayer.getBlock().isLiquid()
                && underplayer.getBlock().getType().equals(Material.LAVA))
                // either we are ignoring water or it is not water
                && (ignoreWater || !(underplayer.getBlock().isLiquid()
                        && underplayer.getBlock().getType().equals(Material.WATER)))
                // not air
                && !underplayer.getBlock().isEmpty()
                // not bedrock (in case of falling out of world glitches)
                && !underplayer.getBlock().getType().equals(Material.BEDROCK);

        // empty = good for the blocks the player is in
        boolean upSafe = topblock.getBlock().isEmpty() || isBlockBreathable(topblock.getBlock());
        boolean locSafe = loc.getBlock().isEmpty() || isBlockBreathable(loc.getBlock());

        // safety is based on all three factors
        return belowSafe && upSafe && locSafe;
    };

    // ensure that spawns or teleports are not in a block
    // moves the spawn up until a safe position is found
    // or returns null if none is found
    public static Location getSafeLocation(Location destination, boolean ignoreWater, int maxDistance) {

        // copy location
        Location newLoc = new Location(destination.getWorld(), destination.getBlockX(), destination.getBlockY(),
                destination.getBlockZ());
        int distance = 0;
        while (!testBlocks(newLoc, ignoreWater)) {
            distance++;
            if (distance > maxDistance) {
                GPTGOD.LOGGER.warn(String.format(
                        "getSafeLocation hit max height for safety checks. No safe location found at (%d, %d, %d)",
                        newLoc.getX(), newLoc.getY(), newLoc.getZ()));
                return null;
            }
            // move the location up
            newLoc.setY(newLoc.getY());

        }
        return newLoc;
    }

    // ensures that the player who is being teleported by our blind god has at least
    // some chance of survival
    public static boolean safeTeleport(Player player, Location destination) {
        // checks current location
        Location safeLocation = getSafeLocation(destination, false, 128);
        if (safeLocation == null) {
            return false;
        }

        return player.teleport(safeLocation);
    }
}
