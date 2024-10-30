package net.bigyous.gptgodmc.GPT;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.GPT.Json.GptModel;

public class GPTModels {
    private static FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();
    public static final GptModel GPT_4o = new GptModel("gpt-4o", 100000);
    public static final GptModel GPT_4o_mini = new GptModel("gpt-4o-mini", 85000);

    public static GptModel getMainModel() {
        String modelName;
        if (config.isSet("model-name")) {
            modelName = config.getString("model-name");
        } else if (config.isSet("use-full-model")) {
            // for passivity
            modelName = config.getBoolean("use-full-model") ? "gemini-1.5-pro" : "gemini-1.5-flash";
        } else {
            throw new RuntimeException("Please set a value for model-name or use-full-model.");
        }

        int tokenLimit;

        if (config.isSet("gpt-model-token-limit")) {
            tokenLimit = config.getInt("gpt-model-token-limit");
        } else {
            tokenLimit = switch (modelName) {
                case "gemini-1.5-pro", "gemini-1.5-pro-002" -> 2000000;
                case "gemini-1.5-flash" -> 850000;
                default -> throw new RuntimeException(String.format(
                        "Could not automatically determine token limit for %s. Please set gpt-model-token-limit in the config.",
                        modelName));
            };
        }
        return new GptModel(modelName, tokenLimit);
    }

    public static GptModel getSecondaryModel() {
        String modelName;
        if (config.isSet("secondary-model-name")) {
            modelName = config.getString("secondary-model-name");
        } else {
            // for passivity
            modelName = "gemini-1.5-flash-8b";
        }

        int tokenLimit;

        if (config.isSet("gpt-secondary-token-limit")) {
            tokenLimit = config.getInt("gpt-secondary-token-limit");
        } else {
            tokenLimit = switch (modelName) {
                case "gemini-1.5-pro", "gemini-1.5-pro-002" -> 2000000;
                case "gemini-1.5-flash" -> 850000;
                case "gemini-1.5-flash-8b" -> 127500; // model 8b is remarkably cheap at less than 128k prompt length
                default -> throw new RuntimeException(String.format(
                        "Could not automatically determine token limit for %s. Please set gpt-secondary-token-limit in the config.",
                        modelName));
            };
        }
        return new GptModel(modelName, tokenLimit);
    }
}
