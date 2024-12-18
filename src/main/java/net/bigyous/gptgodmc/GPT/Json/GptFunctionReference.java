package net.bigyous.gptgodmc.GPT.Json;

import java.util.Map;

public class GptFunctionReference {
    private String type;
    private Map<String, String> function;

    public GptFunctionReference(GptFunction function) {
        this.type = "function";
        this.function = Map.of("name", function.getName());
    }

    public GptFunctionReference(String name) {
        this.type = "function";
        this.function = Map.of("name", name);
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getFunction() {
        return function;
    }

}
