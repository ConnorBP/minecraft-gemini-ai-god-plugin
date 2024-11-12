package net.bigyous.gptgodmc.GPT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;

import net.bigyous.gptgodmc.EventLogger;
import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.Structure;
import net.bigyous.gptgodmc.StructureManager;
import net.bigyous.gptgodmc.WorldManager;
import net.bigyous.gptgodmc.GPT.Json.FunctionDeclaration;
import net.bigyous.gptgodmc.GPT.Json.Schema;
import net.bigyous.gptgodmc.GPT.Json.Tool;
import net.bigyous.gptgodmc.interfaces.SimpFunction;
import net.bigyous.gptgodmc.loggables.GPTActionLoggable;
import net.bigyous.gptgodmc.utils.BukkitUtils;
import net.bigyous.gptgodmc.utils.GptObjectiveTracker;
import net.bigyous.gptgodmc.utils.ImageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class GptActions {
        private int tokens = -1;
        private static Gson gson = new Gson();
        private static JavaPlugin plugin = JavaPlugin.getPlugin(GPTGOD.class);
        private static Boolean useTts = plugin.getConfig().getBoolean("tts");

        private static void staticWhisper(String playerName, String message) {
                Player player = GPTGOD.SERVER.getPlayerExact(playerName);
                player.sendRichMessage("<i>You hear something whisper to you...</i>");
                player.sendMessage(message);
                if (useTts) {
                        Speechify.makeSpeech(message, player);
                }
                EventLogger.addLoggable(
                                new GPTActionLoggable(String.format("whispered \"%s\" to %s", message, playerName)));
        }

        private static void staticAnnounce(String message) {
                GPTGOD.SERVER.broadcast(Component.text("A Loud voice bellows from the heavens", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.BOLD, true));
                GPTGOD.SERVER.broadcast(Component.text(message, NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.BOLD, true));
                if (useTts) {
                        Speechify.makeSpeech(message, null);
                }
                EventLogger.addLoggable(new GPTActionLoggable(String.format("announced \"%s\"", message)));
        }
        // in hindsight, I should have used an interface or abstract class to do this
        // but oh well...

        private static SimpFunction<JsonObject> whisper = (JsonObject args) -> {
                TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>() {
                };
                Map<String, String> argsMap = gson.fromJson(args, mapType);
                String message = argsMap.get("message");
                String playerName = argsMap.get("playerName");
                if (playerName == null) {
                        playerName = argsMap.get("player_name");
                }

                staticWhisper(playerName, message);
                return;
        };

        private static SimpFunction<JsonObject> announce = (JsonObject args) -> {
                String message = gson.fromJson(args.get("message"), String.class);
                staticAnnounce(message);
        };
        private static SimpFunction<JsonObject> giveItem = (JsonObject argObject) -> {
                String playerName = gson.fromJson(argObject.get("playerName"), String.class);
                String itemId = gson.fromJson(argObject.get("itemId"), String.class);
                int count = gson.fromJson(argObject.get("count"), Integer.class);
                // executeCommand(String.format("/give %s %s %d", playerName, itemId, count));
                if (Material.matchMaterial(itemId) == null)
                        return;
                Player player = GPTGOD.SERVER.getPlayer(playerName);
                player.getInventory().addItem(new ItemStack(Material.matchMaterial(itemId), count));
                player.sendRichMessage(String.format("<i>A %s appeared in your inventory</i>", itemId));
                EventLogger.addLoggable(
                                new GPTActionLoggable(String.format("gave %d %s to %s", count, itemId, playerName)));
        };
        private static SimpFunction<JsonObject> command = (JsonObject args) -> {
                String prompt = gson.fromJson(args.get("prompt"), String.class);
                GenerateCommands.generate(prompt);
                EventLogger.addLoggable(new GPTActionLoggable(String.format("commanded \"%s\" to happen", prompt)));
        };
        private static SimpFunction<JsonObject> smite = (JsonObject argObject) -> {
                String playerName = gson.fromJson(argObject.get("playerName"), String.class);
                int power = gson.fromJson(argObject.get("power"), Integer.class);
                Player player = GPTGOD.SERVER.getPlayer(playerName);
                for (int i = 0; i < power; i++) {
                        WorldManager.getCurrentWorld().strikeLightning(player.getLocation());
                }
                EventLogger.addLoggable(new GPTActionLoggable(String.format("smited %s", playerName)));
        };
        private static SimpFunction<JsonObject> spawnEntity = (JsonObject argObject) -> {
                String position = gson.fromJson(argObject.get("position"), String.class);
                String entityName = gson.fromJson(argObject.get("entity"), String.class);
                int count = gson.fromJson(argObject.get("count"), Integer.class);
                String customName = gson.fromJson(argObject.get("customName"), String.class);
                Location location = StructureManager.hasStructure(position)
                                ? StructureManager.getStructure(position).getLocation()
                                : GPTGOD.SERVER.getPlayer(position).getLocation();
                // try to get a safe location for the spawn
                Location safeLocation = BukkitUtils.getSafeLocation(location, true, 256);
                // if the safeLocation is not null, then use it instead of location
                location = safeLocation == null ? location : safeLocation;

                EntityType type = EntityType.fromName(entityName);
                for (int i = 0; i < count; i++) {
                        double r = Math.random() / Math.nextDown(1.0);
                        double offset = 0 * (1.0 - 1) + 3 * r;
                        Entity ent = WorldManager.getCurrentWorld().spawnEntity(location
                                        .offset(offset - i, 0, offset + i).toLocation(WorldManager.getCurrentWorld()),
                                        type, true);
                        TextComponent nameComponent = customName != null
                                        ? PlainTextComponentSerializer.plainText().deserialize(String.format("%s%s",
                                                        customName, i > 0 ? " " + String.valueOf(i) : ""))
                                        : null;
                        ent.customName(nameComponent);
                }
                EventLogger.addLoggable(new GPTActionLoggable(String.format("summoned %d %s%s near %s", count,
                                entityName, customName != null ? String.format(" named: %s,", customName) : "",
                                position)));
        };
        private static SimpFunction<JsonObject> summonSupplyChest = (JsonObject argObject) -> {
                TypeToken<List<String>> stringArrayType = new TypeToken<List<String>>() {
                };
                String playerName = gson.fromJson(argObject.get("playerName"), String.class);
                List<String> itemNames = gson.fromJson(argObject.get("items"), stringArrayType);
                boolean fullStacks = gson.fromJson(argObject.get("fullStacks"), Boolean.class) != null
                                ? gson.fromJson(argObject.get("fullStacks"), Boolean.class)
                                : false;
                List<ItemStack> items = itemNames.stream().map((String itemName) -> {
                        Material mat = Material.matchMaterial(itemName);
                        if (mat == null) {
                                return new ItemStack(Material.COBWEB);
                        }
                        return new ItemStack(mat, fullStacks ? mat.getMaxStackSize() : 1);
                }).toList();
                Location playerLoc = GPTGOD.SERVER.getPlayer(playerName).getLocation();
                Block currentBlock = WorldManager.getCurrentWorld()
                                .getBlockAt(playerLoc
                                                .offset(playerLoc.getDirection().getBlockX() + 1, 0,
                                                                playerLoc.getDirection().getBlockZ() + 1)
                                                .toLocation(null));
                currentBlock.setType(Material.CHEST);
                Chest chest = (Chest) currentBlock.getState();
                chest.getBlockInventory().addItem(items.toArray(new ItemStack[itemNames.size()]));
                chest.open();
                WorldManager.getCurrentWorld().spawnParticle(Particle.WAX_OFF, chest.getLocation().toCenterLocation(),
                                100, 2, 3, 2);
                EventLogger.addLoggable(
                                new GPTActionLoggable(String.format("summoned a chest with: %s inside next to %s",
                                                String.join(", ", itemNames), playerName)));

        };
        private static SimpFunction<JsonObject> transformStructure = (JsonObject argObject) -> {
                String structure = gson.fromJson(argObject.get("structure"), String.class);
                String blockType = gson.fromJson(argObject.get("block"), String.class);
                Structure structureObj = StructureManager.getStructure(structure);
                // blocktypes that we don't want god to accidentally transform
                List<Material> protectedBlockTypes = Arrays.asList(Material.CHEST, Material.ENDER_CHEST,
                                Material.TRAPPED_CHEST, Material.FURNACE, Material.CRAFTING_TABLE,
                                Material.BLAST_FURNACE, Material.ENDER_CHEST, Material.ARMOR_STAND, Material.CAULDRON,
                                Material.BREWING_STAND, Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE,
                                Material.SMOKER, Material.BARREL, Material.GRINDSTONE, Material.COMPOSTER,
                                Material.SMITHING_TABLE, Material.STONECUTTER, Material.BELL, Material.LANTERN,
                                Material.SOUL_LANTERN, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SHROOMLIGHT,
                                Material.PLAYER_HEAD, Material.PIGLIN_HEAD, Material.DRAGON_HEAD, Material.CREEPER_HEAD,
                                Material.ZOMBIE_HEAD, Material.WITHER_SKELETON_SKULL, Material.SKELETON_SKULL,
                                // all doors as of 1.21.1
                                Material.IRON_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.ACACIA_DOOR,
                                Material.OAK_DOOR, Material.BAMBOO_DOOR, Material.CHERRY_DOOR, Material.COPPER_DOOR,
                                Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.WARPED_DOOR, Material.CRIMSON_DOOR,
                                Material.MANGROVE_DOOR,
                                // all buttons as of 1.21.1
                                Material.OAK_BUTTON, Material.BIRCH_BUTTON, Material.STONE_BUTTON,
                                Material.ACACIA_BUTTON, Material.BAMBOO_BUTTON, Material.BAMBOO_BUTTON,
                                Material.CHERRY_BUTTON, Material.JUNGLE_BUTTON, Material.SPRUCE_BUTTON,
                                Material.WARPED_BUTTON, Material.CRIMSON_BUTTON, Material.DARK_OAK_BUTTON,
                                Material.MANGROVE_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON,
                                // all trapdoors as of 1.21.1
                                Material.IRON_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
                                Material.ACACIA_TRAPDOOR, Material.OAK_TRAPDOOR, Material.BAMBOO_TRAPDOOR,
                                Material.CHERRY_TRAPDOOR, Material.COPPER_TRAPDOOR, Material.JUNGLE_TRAPDOOR,
                                Material.SPRUCE_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.CRIMSON_TRAPDOOR,
                                Material.MANGROVE_TRAPDOOR,
                                // all pressure plates as of 1.21.1
                                Material.OAK_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
                                Material.STONE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
                                Material.BAMBOO_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE,
                                Material.JUNGLE_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE,
                                Material.WARPED_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
                                Material.DARK_OAK_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE,
                                Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                                Material.POLISHED_BLACKSTONE_PRESSURE_PLATE, Material.LEVER);
                if (structureObj == null) {
                        EventLogger.addLoggable(new GPTActionLoggable(
                                        "tried to transform non existant structure \"" + structure + "\""));
                        return;
                }
                structureObj.getBlocks().forEach((Block b) -> {
                        if (!protectedBlockTypes.contains(b.getType())) {
                                b.setType(Material.matchMaterial(blockType));
                        }
                });
                EventLogger.addLoggable(new GPTActionLoggable(
                                String.format("turned all the blocks in Structure %s to %s", structure, blockType)));

        };
        private static SimpFunction<JsonObject> revive = (JsonObject args) -> {
                TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>() {
                };
                Map<String, String> argsMap = gson.fromJson(args, mapType);
                String playerName = argsMap.get("playerName");
                Player player = GPTGOD.SERVER.getPlayer(playerName);
                if (player.getGameMode().equals(GameMode.SURVIVAL)) {
                        return;
                }
                Location spawn = player.getRespawnLocation() != null ? player.getRespawnLocation()
                                : WorldManager.getCurrentWorld().getSpawnLocation();
                if (!BukkitUtils.safeTeleport(player, spawn)) {
                        // fallback to regular if it fails for now
                        player.teleport(spawn);
                }
                player.setGameMode(GameMode.SURVIVAL);
                EventLogger.addLoggable(new GPTActionLoggable(String.format("revived %s", playerName)));
        };
        private static SimpFunction<JsonObject> teleport = (JsonObject args) -> {
                TypeToken<Map<String, String>> mapType = new TypeToken<Map<String, String>>() {
                };
                Map<String, String> argsMap = gson.fromJson(args, mapType);
                String playerName = argsMap.get("playerName");
                String destName = argsMap.get("destination");
                Player player = GPTGOD.SERVER.getPlayer(playerName);
                Location destination = StructureManager.hasStructure(destName)
                                ? StructureManager.getStructure(destName).getLocation()
                                : GPTGOD.SERVER.getPlayer(destName).getLocation();

                BukkitUtils.safeTeleport(player, destination);
                EventLogger.addLoggable(
                                new GPTActionLoggable(String.format("teleported %s to %s", playerName, destName)));
        };
        private static SimpFunction<JsonObject> setObjective = (JsonObject args) -> {
                String objective = gson.fromJson(args.get("objective"), String.class);

                Score score = GPTGOD.GPT_OBJECTIVES
                                .getScore(objective.length() > 45 ? objective.substring(0, 44) : objective);
                score.setScore(plugin.getConfig().getInt("objectiveDecay"));
                // decrement the score by one every minute until the score reaches zero
                GptObjectiveTracker tracker = new GptObjectiveTracker(score);
                tracker.setTaskId(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, tracker, 0, 1200));
                if (GPTGOD.GPT_OBJECTIVES.getDisplaySlot() == null)
                        GPTGOD.GPT_OBJECTIVES.setDisplaySlot(DisplaySlot.SIDEBAR);
                EventLogger.addLoggable(new GPTActionLoggable(String.format("set objective %s", objective)));

        };
        private static SimpFunction<JsonObject> clearObjective = (JsonObject args) -> {
                String objective = gson.fromJson(args.get("objective"), String.class);
                GPTGOD.GPT_OBJECTIVES.getScore(objective).resetScore();
                if (GPTGOD.SCOREBOARD.getEntries().stream().filter(entry -> GPTGOD.SERVER.getPlayer(entry) == null)
                                .count() < 1) {
                        GPTGOD.GPT_OBJECTIVES.setDisplaySlot(null);
                }
                EventLogger.addLoggable(
                                new GPTActionLoggable(String.format("declared objective %s as completed", objective)));

        };
        private static SimpFunction<JsonObject> decreeMessage = (JsonObject args) -> {
                String name = gson.fromJson(args.get("playerName"), String.class);
                String message = gson.fromJson(args.get("message"), String.class);
                Player player = GPTGOD.SERVER.getPlayer(name);
                if (player == null) {
                        return;
                }

                Entity ent = player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
                ent.customName(Component.text(message).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
                                .color(TextColor.color(255, 45, 45)));
                // teehee
                ent.setCustomNameVisible(true);
                ent.setInvisible(true);
                ent.setGravity(false);
                ent.setInvulnerable(true);
                ent.setVisualFire(true);
                ent.setGlowing(true);
        };
        private static SimpFunction<JsonObject> detonateStructure = (JsonObject argObject) -> {
                String structure = gson.fromJson(argObject.get("structure"), String.class);
                boolean setFire = gson.fromJson(argObject.get("setFire"), Boolean.class);
                int power = gson.fromJson(argObject.get("power"), Integer.class);
                Structure structureObj = StructureManager.getStructure(structure);
                if(structureObj == null) {
                        // tell god to stop being stupid
                        EventLogger.addLoggable(new GPTActionLoggable(String.format("failed to detonate invalid structure name %s (no such structure found)", structure)));
                        return;
                }
                structureObj.getLocation().createExplosion(power, setFire, true);
                EventLogger.addLoggable(new GPTActionLoggable(String.format("detonated Structure: %s", structure)));
        };
        // private static Function<JsonObject> lookThroughPlayerEyes = (JsonObject args)
        // -> {
        // String playerName = gson.fromJson(args.get("playerName"), String.class);
        // Player player = GPTGOD.SERVER.getPlayer(playerName);
        // ImageUtils.takePicture(player);
        // };
        private static SimpFunction<JsonObject> lookAtStructure = (JsonObject args) -> {
                String structureName = gson.fromJson(args.get("structureName"), String.class);
                if (structureName == null) {
                        GPTGOD.LOGGER.warn("gptGod called lookAtStructure with null structureName");
                        return;
                }
                Structure structure = StructureManager.getStructure(structureName);
                if (structure == null) {
                        GPTGOD.LOGGER.warn("gptGod called lookAtStructure with non existant structure name "
                                        + structureName);
                        return;
                }
                ImageUtils.takePicture(structure, structureName);
        };
        private static Map<String, FunctionDeclaration> functionMap = Map.ofEntries(
                        Map.entry("decree", new FunctionDeclaration("decree",
                                        "display a heavenly decree in front of a specific player in the world. Use only to communicate displeasure in some action. Use no more than 12 words in the message.",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "name of the player to send the decree to"),
                                                        "message",
                                                        new Schema(Schema.Type.STRING, "the message of this decree"))),
                                        decreeMessage)),
                        Map.entry("whisper", new FunctionDeclaration("whisper",
                                        "privately send a message to a player. Avoid repeating things that have already been said. Keep messages short, concise, and no more than 100 characters.",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "name of the player to privately send to"),
                                                        "message", new Schema(Schema.Type.STRING, "the message"))),
                                        whisper)),
                        Map.entry("announce", new FunctionDeclaration("announce",
                                        "brodcast a message to all players. Avoid repeating things that have already been said. Keep messages short, concise, and no more than 100 characters.",
                                        new Schema(Map.of("message", new Schema(Schema.Type.STRING, "the message"))),
                                        announce)),
                        Map.entry("giveItem", new FunctionDeclaration("giveItem", "give a player any amount of an item",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING, "name of the Player"), "itemId",
                                                        new Schema(Schema.Type.STRING,
                                                                        "the name of the minecraft item"),
                                                        "count",
                                                        new Schema(Schema.Type.INTEGER, "amount of the item"))),
                                        giveItem)),
                        Map.entry("command", new FunctionDeclaration("command",
                                        "Describe a series of events you would like to take place, taking into consideration the limitations of minecraft",
                                        new Schema(Collections.singletonMap("prompt",
                                                        new Schema(Schema.Type.STRING,
                                                                        "a description of what will happen"))),
                                        command)),
                        Map.entry("smite", new FunctionDeclaration("smite",
                                        "Strike a player down with lightning, reserve this punishment for repeat offenders",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING, "the player's name"), "power",
                                                        new Schema(Schema.Type.INTEGER,
                                                                        "the strength of this smiting"))),
                                        smite)),
                        Map.entry("transformStructure", new FunctionDeclaration("transformStructure",
                                        "replace all the blocks in a structure with any block",
                                        new Schema(Map.of("structure",
                                                        new Schema(Schema.Type.STRING, "name of the structure"),
                                                        "block",
                                                        new Schema(Schema.Type.STRING,
                                                                        "The name of the minecraft block"))),
                                        transformStructure)),
                        Map.entry("spawnEntity", new FunctionDeclaration("spawnEntity",
                                        "spawn any minecraft entity next to a player or structure",
                                        new Schema(Map.of("position",
                                                        new Schema(Schema.Type.STRING,
                                                                        "name of the Player or Structure"),
                                                        "entity",
                                                        new Schema(Schema.Type.STRING,
                                                                        "the name of the minecraft entity name will be underscore deliminated eg. \"mushroom_cow\""),
                                                        "count",
                                                        new Schema(Schema.Type.INTEGER,
                                                                        "the amount of the entity that will be spawned"),
                                                        "customName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "(optional) custom name that will be given to the spawned entities, set to null to leave entities unnamed"))),
                                        spawnEntity)),
                        Map.entry("summonSupplyChest", new FunctionDeclaration("summonSupplyChest",
                                        "spawn chest full of items for use in a project next to a player",
                                        new Schema(Map.of("items", new Schema(Schema.Type.ARRAY,
                                                        "names of the minecraft items you would like to put in the chest, each item takes up one of 8 slots",
                                                        Schema.Type.STRING), "fullStacks",
                                                        new Schema(Schema.Type.BOOLEAN,
                                                                        "put the maximum stack size of each item?"),
                                                        "playerName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "The name of the player that will recieve this chest"))),
                                        summonSupplyChest)),
                        Map.entry("revive", new FunctionDeclaration("revive", "bring a player back from the dead",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING, "The name of the player"))),
                                        revive)),
                        Map.entry("teleport", new FunctionDeclaration("teleport",
                                        "teleport a player to another player or a structure",
                                        new Schema(Map.of("playerName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "name of the player to be teleported"),
                                                        "destination",
                                                        new Schema(Schema.Type.STRING,
                                                                        "The name of the player or Structure the player will be sent to"))),
                                        teleport)),
                        Map.entry("setObjective", new FunctionDeclaration("setObjective",
                                        "set an objective for players to complete. base this off of the behaviors observed in the logs. objectives can't be longer than 45 characters",
                                        new Schema(Map.of("objective", new Schema(Schema.Type.STRING,
                                                        "the objective to set, if it's for a specific player, be sure to include their name"))),
                                        setObjective)),
                        Map.entry("clearObjective",
                                        new FunctionDeclaration("clearObjective",
                                                        "set an objective as complete. Follow this up with a reward",
                                                        new Schema(Map.of("objective", new Schema(Schema.Type.STRING,
                                                                        "the objective to mark as complete"))),
                                                        clearObjective)),
                        Map.entry("detonateStructure", new FunctionDeclaration("detonateStructure",
                                        "cause an explosion at a Structure",
                                        new Schema(Map.of("structure",
                                                        new Schema(Schema.Type.STRING,
                                                                        "name of the structure (not a player name)"),
                                                        "setFire",
                                                        new Schema(Schema.Type.BOOLEAN,
                                                                        "will this explosion cause fires?"),
                                                        "power",
                                                        new Schema(Schema.Type.INTEGER,
                                                                        "the strength of this explosion where 4 is the strength of TNT"))),
                                        detonateStructure)),
                        Map.entry("lookAtStructure", new FunctionDeclaration("lookAtStructure",
                                        "request to view what a specific structure looks like. A render request will be sent off, and the resulting image will later be sent to the vision api to describe what it sees for you.",
                                        new Schema(Map.of("structureName",
                                                        new Schema(Schema.Type.STRING,
                                                                        "the exact name of the structure to look at"))),
                                        lookAtStructure)));
        // private static Map<String, FunctionDeclaration> speechFunctionMap = new
        // HashMap<>(functionMap);
        // private static Map<String, FunctionDeclaration> actionFunctionMap = new
        // HashMap<>(functionMap);

        private static Tool tools;
        // private static Tool[] actionTools;
        // private static Tool[] speechTools;
        // private static final List<String> speechActionKeys =
        // Arrays.asList("announce", "whisper", "setObjective", "clearObjective",
        // "decree");
        // private static final List<String> persistentActionKeys =
        // Arrays.asList("command");

        // todo: experiment with wrapping a list of functions in a single tool for
        // google
        public static Tool wrapFunctions(Map<String, FunctionDeclaration> functions) {
                FunctionDeclaration[] funcList = functions.values().toArray(new FunctionDeclaration[functions.size()]);
                Tool toolList = new Tool(funcList);
                return toolList;
        }

        public static Tool GetAllTools() {
                if (tools != null) {
                        return tools;
                }
                tools = wrapFunctions(functionMap);
                return tools;
        }

        public static Map<String, FunctionDeclaration> getFunctionMap() {
                return functionMap;
        }

        // public static Tool[] GetActionTools() {
        // if (actionTools == null || actionTools[0] == null) {
        // actionFunctionMap.keySet().removeAll(speechActionKeys);
        // actionFunctionMap.keySet().removeAll(persistentActionKeys);
        // actionTools = wrapFunctions(actionFunctionMap);
        // }
        // Tool[] newTools = GPTUtils.randomToolSubset(actionTools, 3);
        // Tool[] persistentTools = persistentActionKeys.stream().map(key -> {
        // return new Tool(functionMap.get(key));
        // }).toArray(Tool[]::new);
        // // I could do this nicer, but I don't feel like it
        // newTools = GPTUtils.concatWithArrayCopy(newTools, persistentTools);
        // return newTools;
        // }

        // public static Tool[] GetSpeechTools() {
        // if (speechTools != null && speechTools[0] != null) {
        // return speechTools;
        // }
        // speechFunctionMap.keySet().retainAll(speechActionKeys);
        // speechTools = wrapFunctions(speechFunctionMap);
        // return speechTools;
        // }

        public static int run(String functionName, JsonObject jsonArgs) {
                GPTGOD.LOGGER.info(String.format("running function \"%s\" with json arguments \"%s\"", functionName,
                                jsonArgs.toString()));
                Bukkit.getScheduler().runTask(plugin, () -> {
                        functionMap.get(functionName).runFunction(jsonArgs);
                });
                return 1;
        }

        private int calculateFunctionTokens() {
                int sum = 0;
                for (FunctionDeclaration function : functionMap.values()) {
                        sum += function.calculateFunctionTokens();
                }
                return sum;
        }

        public int getTokens() {
                if (tokens >= 0) {
                        return tokens;
                }
                return calculateFunctionTokens();
        }

}
