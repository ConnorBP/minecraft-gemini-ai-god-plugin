package net.bigyous.gptgodmc.GPT.Json;

import com.google.gson.JsonObject;

import net.bigyous.gptgodmc.utils.GPTUtils;

public class FunctionCall {
    private String name;
    private JsonObject args;

    public FunctionCall(String name, JsonObject args) {
        this.name = name;
        this.args = args;
    }

    public JsonObject getArguments() {
        return args;
    }

    public String getName() {
        return name;
    }

    public int calculateFunctionTokens() {
        // todo: this calculates the arguments in json form at the moment so it might be
        // over estimating the token count
        return GPTUtils.countTokens(name) + GPTUtils.countTokens(args.toString());
    }
}
