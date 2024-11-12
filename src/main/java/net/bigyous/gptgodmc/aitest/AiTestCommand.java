package net.bigyous.gptgodmc.aitest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;

import net.bigyous.gptgodmc.Structure;
import net.bigyous.gptgodmc.StructureManager;
import net.bigyous.gptgodmc.GPT.GptActions;
import net.bigyous.gptgodmc.GPT.Json.SpeechifyVoiceInfo;
import net.bigyous.gptgodmc.GPT.Json.SpeechifyVoiceInfo.VoiceType;
import net.bigyous.gptgodmc.utils.ImageUtils;
import net.bigyous.gptgodmc.GPT.Speechify;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class AiTestCommand implements CommandExecutor {

    private static final String subcommandsList = "structure render tts voices";

    private static final String structureHelpText = """
            optional parameters have ? on them
                /aitest s [list|info|render|testcam]
                    list: lists all structure names
                    info [structure name]: gets info about a specific structure
                    render [structure name] [axis?:x|y|z|-x|-y|-z] [distance?]
                    testcam [structure name] [axis?:x|y|z|-x|-y|-z] [distance?]
            """;

    private static final String ttsHelpText = """
                /aitest t [text to speak] ex /tts hello there mortals, how are you?
            """;

    private static final String voicesHelpText = """
                /aitest v [list|set] (personal|page-num)
            """;

    private static final String commandUsage = """
                /aitest [s|structure|t|tts|v|voices] [subcommand args]
                Structure Command:
            """ + structureHelpText + """

                TTS Args:
            """ + ttsHelpText + """

                Voice Args:
            """ + voicesHelpText;

    private Vector getAxisVector(String axis, Vector defaultVec) {
        switch (axis.toLowerCase()) {
        case "e":
        case "x":
        case "+x":
            return new Vector(1, 0, 0);
        case "u":
        case "up":
        case "y":
        case "+y":
            return new Vector(0, 1, 0);
        case "s":
        case "z":
        case "+z":
            return new Vector(0, 0, 1);
        case "w":
        case "-x":
            return new Vector(-1, 0, 0);
        case "d":
        case "down":
        case "-y":
            return new Vector(0, -1, 0);
        case "n":
        case "-z":
            return new Vector(0, 0, -1);
        default:
            return defaultVec;
        }
    }

    public boolean handleTestCam(CommandSender sender, Command command, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("missing args to structure render command! Please include structure name");
            sender.sendMessage(structureHelpText);
            return false;
        }

        Structure structure = StructureManager.getStructure(args[0]);

        if (structure == null) {
            sender.sendMessage("failed to find structure by name: " + args[0]);
            return false;
        }

        // set default angle as isometric top corner
        Vector cameraDirection = new Vector(1, 1, 1).normalize();
        if (args.length >= 2) {
            cameraDirection = getAxisVector(args[1], cameraDirection);
        }

        double cameraDistance = 5;

        if (args.length >= 3) {
            try {
                double d = Double.parseDouble(args[2]);
                if (d > 0.25) {
                    cameraDistance = d;
                }
            } catch (NullPointerException e) {
            } catch (NumberFormatException e) {
            }
        }

        ImageUtils.lookAt(structure.getLocation(), cameraDirection, cameraDistance);

        return true;
    }

    public boolean handleStructureRenderCommand(CommandSender sender, Command command, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("missing args to structure render command! Please include structure name");
            sender.sendMessage(structureHelpText);
            return false;
        }

        Structure structure = StructureManager.getStructure(args[0]);

        if (structure == null) {
            sender.sendMessage("failed to find structure by name: " + args[0]);
            return false;
        }

        // set default angle as isometric top corner
        Vector cameraDirection = new Vector(1, 1, 1).normalize();

        if (args.length >= 2) {
            cameraDirection = getAxisVector(args[1], cameraDirection);
        }

        ImageUtils.takePicture(structure, args[0], cameraDirection.normalize());
        return true;
    }

    public boolean handleStructureCommand(CommandSender sender, Command command, @NotNull String[] args) {

        if (args.length < 1) {
            sender.sendMessage("missing args to structure command!");
            sender.sendMessage(structureHelpText);
            return false;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (args[0].toLowerCase()) {
        case "list":
        case "l":
            sender.sendMessage(StructureManager.getDisplayString(true));
            break;
        case "info":
        case "i":
            break;
        case "render":
        case "r":
            handleStructureRenderCommand(sender, command, subArgs);
        case "testcam":
        case "t":
            return handleTestCam(sender, command, subArgs);
        case "help":
        case "h":
        case "?":
            sender.sendMessage(structureHelpText);
            return true;
        default:
            sender.sendMessage("invalid option. Please include a valid argument for the structure command.");
            sender.sendMessage(structureHelpText);
            return false;
        }

        return true;
    }

    // just a wrapper arround the announce command to say god messages and test the
    // voice
    public boolean handleTtsCommand(CommandSender sender, Command command, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("please include a message with the tts command!");
            sender.sendMessage(ttsHelpText);
            return false;
        }

        JsonObject params = new JsonObject();
        params.addProperty("message", String.join(" ", args));

        GptActions.run("announce", params);
        return true;
    }

    // helper command so that users can easily find out what voices are available to
    // them
    // or find out the real id (not just display name) of their custom voices
    public boolean handleVoicesCommand(CommandSender sender, Command command, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("please include a subcommand for the voices command");
            sender.sendMessage(voicesHelpText);
            return false;
        }

        boolean personalOnly = false;
        int page = 0;
        int pageSize = 10;
        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
            case "p":
            case "personal":
                personalOnly = true;
                break;
            default:
                try {
                    int getPage = Integer.parseUnsignedInt(args[1]);
                    if(getPage > 0) page = getPage-1;
                } catch(NumberFormatException e) {
                    sender.sendMessage(Component.text("invalid page number!").color(TextColor.color(255, 40, 40)));
                }
                break;
            }
        }

        switch (args[0].toLowerCase()) {
        case "l":
        case "list":
            // todo thread this blocking web request
            SpeechifyVoiceInfo[] voices = Speechify.requestAllVoices();
            List<String> voicesMsg = new ArrayList<>();
            int pageCount = 1 + voices.length / pageSize;
            if (personalOnly) {
                voicesMsg.add("Listing personal voices only:");
            } else {
                voicesMsg.add(String.format("All Speechify voices (Page %d of %d):", page+1, pageCount));
            }
            int pageStart = page * pageSize;
            for (int i = pageStart; i < voices.length && i < (pageStart + pageSize); i++) {
                if (!personalOnly || voices[i].getType() == VoiceType.personal) {
                    voicesMsg.add(String.format("%s: %s", voices[i].getDisplay_name(), voices[i].getId()));
                }
            }
            sender.sendMessage(voicesMsg.toArray(new String[voicesMsg.size()]));
            break;
        case "s":
        case "set":
            sender.sendMessage("TODO");
            break;
        default:
            sender.sendMessage(voicesHelpText);
            return false;
        }

        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (!sender.hasPermission("admin.debug.aitest")) {
            sender.sendMessage(Component.text("ERROR! You do not have the required permission admin.debug.aitest")
                    .decorate(TextDecoration.BOLD).color(TextColor.color(90, 30, 45)));
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage("please choose at least one subcommand such as: " + subcommandsList);
            return false;
        }

        // copy inclusive bottom range exclusive top to new array for subcommands
        // skipping main cmd
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (args[0].toLowerCase()) {
        case "s":
        case "structure":
            return handleStructureCommand(sender, command, subArgs);
        case "v":
        case "voice":
        case "voices":
            return handleVoicesCommand(sender, command, subArgs);
        case "t":
        case "tts":
            return handleTtsCommand(sender, command, subArgs);
        case "r":
        case "render":
            sender.sendMessage("todo. try structure render. Standalone render function may not be needed");
            return true; // todo
        case "help":
        case "?":
            sender.sendMessage(commandUsage);
            return true;
        default:
            sender.sendMessage(commandUsage);
            sender.sendMessage(commandUsage);
            return false;
        }
    }

}
