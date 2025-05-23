package net.bigyous.gptgodmc.GPT.Json;

public class TTSRequest {
    private String model;
    private String input;
    private String voice;
    private String response_format;

    public TTSRequest(String model, String input, String voice, String format) {
        this.model = model;
        this.input = input;
        this.voice = voice;
        this.response_format = format;
    }

    public String getModel() {
        return model;
    }

    public String getInput() {
        return input;
    }

    public String getVoice() {
        return voice;
    }

    public String getResponse_format() {
        return response_format;
    }

}
