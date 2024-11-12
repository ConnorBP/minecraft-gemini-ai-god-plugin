package net.bigyous.gptgodmc.GPT.Json;

// get voices request returns an array of this object
public class SpeechifyVoiceInfo {

    public enum VoiceType {
        personal, shared
    }

    public class SpeechifyLanguage {
        String locale;
        String preview_audio;
    }

    public class SpeechifyModel {
        String name;

    }

    String id;
    VoiceType type;
    String display_name;
    SpeechifyModel[] models;

    public String getId() {
        return id;
    }
    public VoiceType getType() {
        return type;
    }
    public String getDisplay_name() {
        return display_name;
    }
    public SpeechifyModel[] getModels() {
        return models;
    }
}
