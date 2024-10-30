package net.bigyous.gptgodmc.GPT.Json;

public class FunctionCallingConfig {
    public enum Mode {
        MODE_UNSPECIFIED, // Unspecified function calling mode. This value should not be used.
        AUTO, // Default model behavior, model decides to predict either a function call or a
              // natural language response.
        ANY, // Model is constrained to always predicting a function call only. If
             // "allowedFunctionNames" are set, the predicted function call will be limited
             // to any one of "allowedFunctionNames", else the predicted function call will
             // be any one of the provided "functionDeclarations".
        NONE // Model will not predict any function call. Model behavior is same as when not
             // passing any function declarations.
    }

    private Mode mode;
    private String[] allowedFunctionNames;

    public FunctionCallingConfig() {
        // default to mode "any" (function responses only) with any function
        this.mode = Mode.ANY;
    }

    public FunctionCallingConfig(String[] allowedFunctionNames) {
        // default to mode "any" (function responses only) with any function
        this.mode = Mode.ANY;
        this.allowedFunctionNames = allowedFunctionNames;
    }

    public FunctionCallingConfig(Mode mode, String[] allowedFunctionNames) {
        this.mode = mode;
        this.allowedFunctionNames = allowedFunctionNames;
    }

    public Mode getMode() {
        return mode;
    }

    public String[] getAllowedFunctionNames() {
        return allowedFunctionNames;
    }

}
