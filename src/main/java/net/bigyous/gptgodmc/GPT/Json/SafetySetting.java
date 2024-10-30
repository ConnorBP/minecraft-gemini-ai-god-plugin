package net.bigyous.gptgodmc.GPT.Json;

public class SafetySetting {
    public enum HarmCategory {
        HARM_CATEGORY_UNSPECIFIED,
        HARM_CATEGORY_DEROGATORY,
        HARM_CATEGORY_TOXICITY,
        HARM_CATEGORY_VIOLENCE,
        HARM_CATEGORY_SEXUAL,
        HARM_CATEGORY_MEDICAL,
        HARM_CATEGORY_DANGEROUS,
        HARM_CATEGORY_HARASSMENT,
        HARM_CATEGORY_HATE_SPEECH,
        HARM_CATEGORY_SEXUALLY_EXPLICIT,
        HARM_CATEGORY_DANGEROUS_CONTENT,
        HARM_CATEGORY_CIVIC_INTEGRITY
    }

    public enum HarmBlockThreshold {
        HARM_BLOCK_THRESHOLD_UNSPECIFIED,
        BLOCK_LOW_AND_ABOVE,
        BLOCK_MEDIUM_AND_ABOVE,
        BLOCK_ONLY_HIGH,
        BLOCK_NONE,
        OFF
    }

    private HarmCategory category;
    private HarmBlockThreshold threshold;

    public SafetySetting(HarmCategory category, HarmBlockThreshold threshold) {
        this.category = category;
        this.threshold = threshold;
    }

    public HarmCategory getCategory() {
        return category;
    }

    public HarmBlockThreshold getThreshold() {
        return threshold;
    }

}
