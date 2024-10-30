package net.bigyous.gptgodmc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.bigyous.gptgodmc.GPT.GPTModels;
import net.bigyous.gptgodmc.GPT.GptAPI;
import net.bigyous.gptgodmc.GPT.Personality;
import net.bigyous.gptgodmc.GPT.Prompts;
import net.bigyous.gptgodmc.utils.GPTUtils;
import net.bigyous.gptgodmc.utils.BukkitUtils;

import java.util.ArrayList;
import java.util.List;

public class GameLoop {
    private static JavaPlugin plugin = JavaPlugin.getPlugin(GPTGOD.class);
    private static FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();
    private static GptAPI GPT_API;
    private static int taskId;
    public static boolean isRunning = false;
    private static String PROMPT;
    private static String PROMPT_BASE = "React only to current events and reference server history for any recurring player behaviors. Use all communication tools available to you in creative ways and in varying tones, adapting to the context and each player's actions. If there are no objectives set, make sure to add one for each player. function parameter names must match the original camel cased name.";
    private static String REQUIREMENTS = "Role Requirements: When interacting with players, choose from a range of responses: use whisper for private or subtle guidance, announce for dramatic proclamations, and decree to reinforce in-world commandments. Avoid repeating the same type of response for variety.";
    private static String GUIDANCE = """
                Behavior Guidance:
                Communicate with all tools available to you.
                Use a mixture of gift and punishment actions in addition to the text based communications.
                Set interesting objectives to perform around the island, especially if none exist yet.
                Make objectives interesting and creative, keeping in mind your likes and dislikes when you create them.
                Reward players who complete their objectives within a minecraft day cycle and punish those who do not.
                do NOT give out missions to a player which are already in the objectives list.
            """;
    private static String STYLE = """
                Response Style:
                When communicating, vary tone and intensity:
                For minor infractions: start with a light-hearted or humorous whisper or decree.
                For repeated actions: reinforce your message with an announce or objective and add a clear consequence if ignored.
                Example responses:
                Whisper: “A gentle reminder, dear mortal, mind your words.”
                Announce: “Mortals, let it be known that peace shall reign, free of bickering!”
                Objective: “MoistPyro, seek a lily to calm thy spirit.”
            """;
    private static String ESCALATION = """
                Gradual Escalation:
                Respond to behavior with increasing intensity if actions persist.
                Start by setting objectives or whispering reminders,
                then follow up with announcements,
                then smite or detonateStructure for repeated rule breaking, blasphemy, blatant defiance, or group defiance on strike two or three.
            """;
    private static String ROLEPLAY = "Remain fully in character, addressing players as their god, and adapt your responses to create an engaging, immersive environment.";

    private static ArrayList<String> previousActions = new ArrayList<String>();
    private static String personality;
    private static int rate = config.getInt("rate") < 1 ? 40 : config.getInt("rate");
    private static double tempurature = config.getDouble("model-tempurature") < 0.01 ? 1.0
            : config.getDouble("model-tempurature");

    public static void init() {
        if (isRunning || !config.getBoolean("enabled"))
            return;
        GPT_API = new GptAPI(GPTModels.getMainModel(), tempurature);
        BukkitTask task = GPTGOD.SERVER.getScheduler().runTaskTimerAsynchronously(plugin, new GPTTask(),
                BukkitUtils.secondsToTicks(30),
                BukkitUtils.secondsToTicks(rate));
        taskId = task.getTaskId();
        personality = Personality.generatePersonality();
        PROMPT = Prompts.getGamemodePrompt(GPTGOD.gameMode);
        String[] systemPrompt = new String[] {
                PROMPT,
                personality,
                PROMPT_BASE,
                REQUIREMENTS,
                GUIDANCE,
                STYLE,
                ESCALATION,
                ROLEPLAY
        };
        GPT_API.setSystemContext(systemPrompt);
        // set tool only mode
        GPT_API.setToolOnlyAllTools();

        isRunning = true;
        GPTGOD.LOGGER.info("GameLoop Started, the minecraft god has awoken");
    }

    public static void stop() {
        if (!isRunning)
            return;
        GPTGOD.SERVER.getScheduler().cancelTask(taskId);
        EventLogger.reset();
        GPT_API = null;
        isRunning = false;
        GPTGOD.LOGGER.info("GameLoop Stoppped");
    }

    public static void logAction(String actionLog) {
        previousActions.add(actionLog);
    }

    private static String getPreviousActions() {
        if (previousActions.isEmpty()) {
            return "";
        }
        String out = " You (God) Just did the following actions: " + String.join(",", previousActions);
        previousActions = new ArrayList<String>();
        return out;
    }

    private static class GPTTask implements Runnable {

        @Override
        public void run() {
            while (EventLogger.isGeneratingSummary() && !EventLogger.hasSummary()) {
                Thread.onSpinWait();
            }
            if (EventLogger.hasSummary()) {
                GPT_API.addLogs("Summary of Server History: " + EventLogger.getSummary(), "summary");
            }

            // get logs since last flush then clear
            List<String> logs = EventLogger.flushLogs();

            // event logger never needs to be culled since we are using dump to clear it
            // EventLogger.cull(GPT_API.getMaxTokens() - nonLogTokens);
            // instead we cull at GPT_API now with room for the next logs
            GPT_API.cull(GPTUtils.countTokens(logs));

            // add logs in series with responses
            GPT_API.addMessages(logs.toArray(new String[logs.size()]));

            if (!previousActions.isEmpty()) {
                GPT_API.addLogs(getPreviousActions(), "previous_actions");
            }

            // prompt the ai if the latest content is from the model
            if (GPT_API.isLatestMessageFromModel()) {
                GPT_API.addMessage("what would you like to do or say next?");
            }
            GPT_API.send();

            while (GPT_API.isSending()) {
                Thread.onSpinWait();
            }
            GPT_API.addMessage(
                    "Now, choose an interesting non-verbal action to perform which has not already been done.");

            GPT_API.send();

            // Cannot determine why this was deemed necessary by yous
            // Thread.currentThread().interrupt();
        }

    }
}
