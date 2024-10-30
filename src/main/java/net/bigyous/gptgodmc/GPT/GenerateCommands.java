package net.bigyous.gptgodmc.GPT;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.bigyous.gptgodmc.EventLogger;
import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.StructureManager;
import net.bigyous.gptgodmc.GPT.Json.FunctionDeclaration;
import net.bigyous.gptgodmc.GPT.Json.Part;
import net.bigyous.gptgodmc.GPT.Json.Schema;
import net.bigyous.gptgodmc.GPT.Json.Tool;
import net.bigyous.gptgodmc.GPT.Json.Content;
import net.bigyous.gptgodmc.GPT.Json.FunctionCall;
import net.bigyous.gptgodmc.GPT.Json.Content.Role;
import net.bigyous.gptgodmc.enums.GptGameMode;
import net.bigyous.gptgodmc.interfaces.Function;
import net.bigyous.gptgodmc.loggables.CommandLoggable;
import net.bigyous.gptgodmc.utils.CommandHelper;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.command.CommandException;

public class GenerateCommands {
        private static Gson gson = new Gson();
        private static Function<JsonObject> inputCommands = (JsonObject args) -> {
                String[] commands = gson.fromJson(args.get("commands"), String[].class);
                try {
                        String output = CommandHelper.executeCommands(commands);
                        EventLogger.addLoggable(new CommandLoggable(output));
                        GenerateCommands.gpt.addMessage("Command resulted in output: " + output);
                } catch (CommandException e) {
                        // give the command generation ai some feedback when it does something wrong
                        String feedback = "encountered error trying to execute commands: " + e.getMessage();
                        EventLogger.addLoggable(new CommandLoggable(e.getMessage()));
                        GenerateCommands.gpt.addMessage(feedback);
                        GPTGOD.LOGGER.error("Command Error Feedback: " + feedback);
                } catch (RuntimeException e) {
                        // give the command generation ai some feedback when it does something wrong
                        String feedback = "encountered runtime error trying to execute commands: " + e.getMessage();
                        EventLogger.addLoggable(new CommandLoggable(e.getMessage()));
                        GenerateCommands.gpt.addMessage(feedback);
                        GPTGOD.LOGGER.error("Command Runtime Error Feedback: " + feedback);
                }
        };

        private static Map<String, FunctionDeclaration> functionMap = Map.of("inputCommands",
                        new FunctionDeclaration("inputCommands", "input the minecraft commands to be executed",
                                        new Schema(Map.of("commands", new Schema(Schema.Type.ARRAY,
                                                        "list of minecraft commands, each entry in the list is an individual command",
                                                        Schema.Type.STRING))),
                                        inputCommands));
        private static Tool tools = GptActions.wrapFunctions(functionMap);
        private static GptAPI gpt = new GptAPI(GPTModels.getSecondaryModel(), tools)
                        .setSystemContext(
                                        """
                                                        You are a helpful assistant that will generate
                                                        one or more minecraft java edition commands based on a prompt inputted by the user,
                                                        even if the prompt seems impossible in minecraft try to approximate it as close as possible
                                                        with functioning minecraft commands. A wrong answer is better than no answer.
                                                        The commands must be compatible with minecraft.
                                                        There must always be at least one command in the response and no other types of responses.
                                                        If the description calls for multiple events then make multiple commands but try not to go far past 24.
                                                        Try to offset dangerous spawns from the exact player position.
                                                        Make sure that title text displays fit in the screen.
                                                        Ensure that positionaly dependent code is executed relative to the specific player.
                                                        Only use a tool call in one json response, other responses will be ignored.
                                                        The response must be valid minecraft command syntax.
                                                        Do not use item frames with books to display text.
                                                        Pay VERY close attention to opening and closing delimeters in the syntax and make sure they match up.
                                                        Prefer the use of 'single quotes' over \\" or " when possible.
                                                        """)
                        .setTools(tools)
                        .addMessages(new String[] {
                                        "write Minecraft commands that: make a fireworks display all around MoistPyro"
                        })
                        // add an example of the correct type of output we are looking for
                        .addResponse(new Content(Role.model,
                                        new Part[] {
                                                        new Part(new FunctionCall("inputCommands", JsonParser
                                                                        .parseString("""
                                                                                                {
                                                                                                        "commands": [
                                                                                                                "execute at MoistPyro run summon firework_rocket ~ ~ ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[15,14,13,12],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~1 ~ ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[1,2,3,4],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~-1 ~ ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[5,6,7,8],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~ ~1 ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[9,10,11,12],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~ ~-1 ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[13,14,15,0],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~2 ~ ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[15,14,13,12],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~-2 ~ ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[1,2,3,4],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~ ~2 ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[5,6,7,8],Flicker:false,Trail:false}]}}",
                                                                                                                "execute at MoistPyro run summon firework_rocket ~ ~-2 ~ {Firework:{Flight:1,Explosions:[{Type:1,Colors:[9,10,11,12],Flicker:false,Trail:false}]}}"
                                                                                                        ]
                                                                                                }
                                                                                        """)
                                                                        .getAsJsonObject()))
                                        }))
                        .setToolChoice("inputCommands");

        public static void generate(String prompt) {
                GPTGOD.LOGGER.info("generating commands with prompt: " + prompt);
                String structures = StructureManager.getDisplayString();

                String teams = String.join(",", GPTGOD.SCOREBOARD.getTeams().stream().map(team -> {
                        return team.getName();
                }).toList());
                if (GPTGOD.gameMode.equals(GptGameMode.DEATHMATCH)) {
                        gpt.addContext(String.format("Teams: %s", teams), "teams");
                }
                gpt.addContext(
                                String.format("Players: %s",
                                                Arrays.toString(
                                                                GPTGOD.SERVER.getOnlinePlayers().stream()
                                                                                .map(player -> player.getName())
                                                                                .toArray())),
                                "PlayerNames")
                                .addContext(String.format("Structures: %s", structures), "structures")
                                .addMessage(String.format("write Minecraft commands that: %s", prompt))
                                .send(functionMap);
        }
}
