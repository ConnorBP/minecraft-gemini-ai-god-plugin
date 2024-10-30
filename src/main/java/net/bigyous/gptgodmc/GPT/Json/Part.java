package net.bigyous.gptgodmc.GPT.Json;

import com.google.gson.annotations.SerializedName;

import net.bigyous.gptgodmc.utils.GPTUtils;

// a message data part. Can only be one of the types
// https://ai.google.dev/api/caching#Part
public class Part {
    @SerializedName("text")
    String text;

    // @SerializedName("inlineData")
    // private Blob inlineData;

    @SerializedName("functionCall")
    private FunctionCall functionCall;

    // @SerializedName("functionResponse")
    // private FunctionResponse functionResponse;

    @SerializedName("fileData")
    private FileData fileData;

    // @SerializedName("executableCode")
    // private ExecutableCode executableCode;

    // @SerializedName("codeExecutionResult")
    // private CodeExecutionResult codeExecutionResult;

    public String getText() {
        return text;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    // constructors for the various union types
    public Part(String text) {
        this.text = text;
    }

    public Part(FileData fileData) {
        this.fileData = fileData;
    }

    public Part(FunctionCall function) {
        this.functionCall = function;
    }

    // calculates and returns the token count of this part
    public int countTokens() {
        if (text != null) {
            return GPTUtils.countTokens(text);
        } else if (functionCall != null) {
            return functionCall.calculateFunctionTokens();
        }
        return 0;
    }
}

class FileData {
    private String mimeType;
    private String fileUri;

    // used
    private transient int tokenCount;

    public FileData(String mimeType, String fileUri) {
        this.mimeType = mimeType;
        this.fileUri = fileUri;
    }

    // https://ai.google.dev/gemini-api/docs/tokens?lang=python#multimodal-tokens
    public int countTokens() {
        if (this.mimeType.startsWith("image")) {
            // images have a fixed token count of 258 on gemini
            return 258;
        }

        // if all else fails return 256 just in case
        return 256;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileUri() {
        return fileUri;
    }

    public int getTokenCount() {
        return tokenCount;
    }

}