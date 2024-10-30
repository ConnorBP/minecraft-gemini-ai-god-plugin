package net.bigyous.gptgodmc.utils;

import org.bukkit.command.CommandSender;

import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.WorldManager;

public class CommandHelper {
    private static boolean dispatch(String command, CommandSender console) {
        // can't let GPT turn off mob spawning
        if (command.contains("doMobSpawning")) {
            return false;
        }
        command = command.charAt(0) == '/' ? command.substring(1) : command;
        if (command.matches(".*\\bgive\\b.*") || command.contains(" in ")) {
            return GPTGOD.SERVER.dispatchCommand(console, command);
        } else {
            if (!(command.contains(" as ") || command.contains(" at "))
                    && (command.contains("~") || command.contains("^"))) {
                command = "execute at @r run " + command;
            }
            return GPTGOD.SERVER.dispatchCommand(console,
                    String.format("execute in %s run %s", WorldManager.getDimensionName(),
                            command));
        }
    }

    public static String executeCommands(String[] commands) {
        GPTCommandSender console = new GPTCommandSender(GPTGOD.SERVER.getConsoleSender());
        for (String command : commands) {
            if (!dispatch(command, console)) {
                return "error in command dispatch";
            }
        }
        return console.getOutput();
    }

    public static String executeCommand(String command) {
        GPTCommandSender console = new GPTCommandSender(GPTGOD.SERVER.getConsoleSender());
        dispatch(command, console);
        return console.getOutput();
    }
}
