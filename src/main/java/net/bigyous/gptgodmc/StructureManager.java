package net.bigyous.gptgodmc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import net.bigyous.gptgodmc.Structure.CiritiqueStatus;
import net.bigyous.gptgodmc.loggables.GenericEventLoggable;

import org.bukkit.entity.Player;

public class StructureManager implements Listener {
    private static ConcurrentHashMap<String, Structure> structures = new ConcurrentHashMap<String, Structure>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Location newBlock = event.getBlock().getLocation();
        addBlockToStructures(newBlock, event.getPlayer());
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        removeBlockFromAllStructures(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        removeBlockFromAllStructures(event.getBlock().getLocation());
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        removeBlockFromAllStructures(event.getBlock().getLocation());
        String structure = getStructureThatContains(event.getBlock().getLocation());
        if (structure != null) {
            EventLogger.addLoggable(new GenericEventLoggable(structure + " is on fire!"));
        }
    }

    @EventHandler
    public void onBlockExploded(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location location = block.getLocation();
            removeBlockFromAllStructures(location);
        }
    }

    @EventHandler
    public void onExploded(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location location = block.getLocation();
            removeBlockFromAllStructures(location);
        }
    }

    private void addBlockToStructures(Location block, Player builder) {
        Structure parentStructure = null;
        for (String structurekey : structures.keySet()) {
            Structure structure = structures.get(structurekey);
            if (structure.isBlockConnected(block)) {
                if (parentStructure != null) {
                    parentStructure.merge(structure);
                    structures.remove(structurekey);
                } else {
                    structure.addBlock(block);
                    parentStructure = structure;
                }
            }
        }
        if (parentStructure == null) {
            String name = nameStructure(block);
            structures.put(name, new Structure(block, builder, name));
        }
    }

    // for the AI to rename a structure and declare it pretty or ugly
    public static boolean updateStructureDetails(String originalStructureName, String newStructureName,
            String description, boolean isItUgly) {
        Structure structure = structures.get(originalStructureName);
        if (structure == null) {
            GPTGOD.LOGGER.error(String.format("Failed to update the %s structure %s to new name %s.",
                    isItUgly ? "ugly" : "pretty", originalStructureName, newStructureName));
            return false;
        }

        // fall back to original structure name
        if (newStructureName == null || newStructureName.length() < 1) {
            newStructureName = originalStructureName;
        }

        structure.setCritique(isItUgly);
        structure.setDescription(description);
        structure.setName(newStructureName);
        // insert the structure under the new name
        structures.put(newStructureName, structure);
        // replace structure in place
        // structures.put(originalStructureName, structure);

        return true;
    }

    private String nameStructure(Location block) {
        // Look we are just going to assume the first block placed will make up the
        // majority of blocks in the structure
        return String.format("%s_Structure_%d", block.getBlock().getType().name(), structures.size());
    }

    public static List<String> getStructures() {
        return structures.keySet().stream().filter(key -> structures.get(key).getSize() > 20).toList();
    }

    public static List<String> getAllStructures() {
        return structures.keySet().stream().toList();
    }

    public static Structure getStructure(String name) {
        return structures.get(name);
    }

    public static void reset() {
        structures = new ConcurrentHashMap<String, Structure>();
    }

    private static String getStructureThatContains(Location block) {
        for (String key : structures.keySet()) {
            Structure structure = structures.get(key);
            if (structure.containsBlock(block)) {
                return key;
            }
        }
        return null;
    }

    private static void removeBlockFromAllStructures(Location block) {
        for (String key : structures.keySet()) {
            Structure structure = structures.get(key);
            if (structure.containsBlock(block)) {
                structure.removeBlock(block);
                if (structure.getSize() < 1)
                    structures.remove(key);
            }
        }
    }

    public static Structure getClosestStructureToLocation(Location location) {
        if (getStructures().isEmpty())
            return null;

        int distance = Integer.MAX_VALUE;
        Structure closest = null;
        for (String key : getStructures()) {
            Structure s = structures.get(key);
            if (s == null) continue;
            if (!s.getLocation().getWorld().equals(location.getWorld())) continue;
            int temp = s.getDistanceToI(location);
            if (temp < distance) {
                distance = temp;
                Structure newStructure = getStructure(key);;
                if(newStructure != null) closest = newStructure;
            }
        }

        return closest;
    }

    public static String getStructureDescription(Structure closestStructure, Location currentPlayerLocation) {
        if (!currentPlayerLocation.getWorld().getName().equals(WorldManager.getCurrentWorld().getName()))
            return "In a different dimension";
        if(closestStructure == null) return "";
        int distance = closestStructure.getDistanceToI(currentPlayerLocation);

        if (distance < 10) {
            return String.format("Location: near %s\n", closestStructure);
        } else if (distance < 50) {
            return String.format("Location: %d blocks away from %s\n", distance, closestStructure);
        } else {
            return "";
        }
    }

    public static StructureProximityData getStructureProximityData(Location location) {
        if (!location.getWorld().getName().equals(WorldManager.getCurrentWorld().getName()))
            return null;
        if (getStructures().isEmpty())
            return null;
        int distance = Integer.MAX_VALUE;
        String closest = "";
        for (String key : getStructures()) {
            int temp = Math.toIntExact(Math.round(location.distance(structures.get(key).getLocation())));
            if (temp < distance) {
                distance = temp;
                closest = key;
            }
        }
        if (distance < 50) {
            return new StructureProximityData(closest, distance);
        } else {
            return null;
        }
    }

    public static boolean hasStructure(String key) {
        return structures.containsKey(key);
    }

    public static class StructureProximityData {
        private int distance;
        private String structure;

        public StructureProximityData(String structure, int distance) {
            this.distance = distance;
            this.structure = structure;
        }

        public int getDistance() {
            return distance;
        }

        public String getStructure() {
            return structure;
        }
    }

    public static String getDisplayString(boolean getAll) {
        List<String> structureNames = getAll ? StructureManager.getAllStructures() : StructureManager.getStructures();
        Object[] structures = structureNames.stream().map((String key) -> {
            Structure structure = StructureManager.getStructure(key);

            String critique = "";
            if (structure.getCritique() == CiritiqueStatus.PRETTY) {
                critique = "Pretty ";
            } else if (structure.getCritique() == CiritiqueStatus.UGLY) {
                critique = "Ugly ";
            }

            return String.format("%s%s built by %s (at %s)", critique, key, structure.getBuilder().getName(),
                    structure.getLocation().toVector().toString());
        }).toArray();

        // explicitly tell the LLM if the array is empty
        return (structures.length > 0) ? Arrays.toString(structures) : "NONE";
    }

    public static String getDisplayString() {
        return getDisplayString(false);
    }
}
