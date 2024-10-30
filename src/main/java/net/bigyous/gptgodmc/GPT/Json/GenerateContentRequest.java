package net.bigyous.gptgodmc.GPT.Json;

import java.util.ArrayList;
import java.util.List;

import net.bigyous.gptgodmc.GPT.Json.Content.Role;
// import net.bigyous.gptgodmc.GPT.Json.SafetySetting.HarmBlockThreshold;
// import net.bigyous.gptgodmc.GPT.Json.SafetySetting.HarmCategory;

// Class to represent the structure of the JSON request to gemini
// for generating content responses
// https://ai.google.dev/api/generate-content#method:-models.generatecontent
public class GenerateContentRequest {
    // Required. The content of the current conversation with the model.
    // For single-turn queries, this is a single instance.
    // For multi-turn queries like chat, this is a repeated field that contains the
    // conversation history and the latest request.
    private ArrayList<Content> contents = new ArrayList<Content>();

    // tools Optional. A list of Tools the Model may use to generate the next
    // response. A Tool is a piece of code that enables the system to interact with
    // external systems to perform an action, or set of actions, outside of
    // knowledge and scope of the Model. Supported Tools are Function and
    // codeExecution. Refer to the Function calling and the Code execution guides to
    // learn more.
    private Tool[] tools;

    // tool config Optional. Tool configuration for any Tool specified in the
    // request. Refer to the Function calling guide for a usage example.
    private ToolConfig toolConfig;

    // safety settings Optional. A list of unique SafetySetting instances for
    // blocking unsafe content.
    // This will be enforced on the GenerateContentRequest.contents and
    // GenerateContentResponse.candidates.
    // There should not be more than one setting for each SafetyCategory type.
    // The API will block any contents and responses that fail to meet the
    // thresholds set by these settings.
    // This list overrides the default settings for each SafetyCategory specified in
    // the safetySettings.
    // If there is no SafetySetting for a given SafetyCategory provided in the list,
    // the API will use the default safety setting for that category.
    // Harm categories HARM_CATEGORY_HATE_SPEECH, HARM_CATEGORY_SEXUALLY_EXPLICIT,
    // HARM_CATEGORY_DANGEROUS_CONTENT, HARM_CATEGORY_HARASSMENT are supported.
    // Refer to the guide for detailed information on available safety settings.
    // Also refer to the Safety guidance to learn how to incorporate safety
    // considerations in your AI applications.
    // private SafetySetting[] safetySettings = new SafetySetting[] {
    // new SafetySetting(HarmCategory.HARM_CATEGORY_UNSPECIFIED,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_DEROGATORY,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_TOXICITY,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_VIOLENCE,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_SEXUAL, HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_MEDICAL,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_DANGEROUS,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_HARASSMENT,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_HATE_SPEECH,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
    // HarmBlockThreshold.OFF),
    // new SafetySetting(HarmCategory.HARM_CATEGORY_CIVIC_INTEGRITY,
    // HarmBlockThreshold.OFF)
    // }; // disable the safety settings

    // system instruction Optional. Developer set system instruction(s). Currently,
    // text only.
    // generation config Optional. Configuration options for model generation and
    // outputs.
    // cached content Optional. The name of the content cached to use as context to
    // serve the prediction. Format: cachedContents/{cachedContent}
    private Content systemInstruction;

    // generation config Optional. Configuration options for model generation and
    // outputs.
    private GenerationConfig generationConfig;

    // cached content Optional. The name of the content cached to use as context to
    // serve the prediction. Format: cachedContents/{cachedContent}
    // private CachedContent cachedContent;

    public GenerateContentRequest() {
        this.generationConfig = new GenerationConfig(1.0);
    }

    public GenerateContentRequest(Tool tools) {
        this.generationConfig = new GenerationConfig(1.0);
        this.tools = new Tool[] { tools };
    }

    public GenerateContentRequest(double temp) {
        this.generationConfig = new GenerationConfig(temp);
    }

    public GenerateContentRequest(Tool tools, double temp) {
        this.generationConfig = new GenerationConfig(temp);
        this.tools = new Tool[] { tools };
    }

    public Content getSystemInstruction() {
        return systemInstruction;
    }

    public Tool[] getTools() {
        return tools;
    }

    public GenerateContentRequest setSystemInstruction(String systemInstruction) {
        this.systemInstruction = new Content(systemInstruction);
        return this;
    }

    public GenerateContentRequest setSystemInstruction(String[] systemInstructions) {
        this.systemInstruction = new Content(systemInstructions);
        return this;
    }

    // shortcut to append a prompt and file to message contents
    public GenerateContentRequest addFileWithPrompt(String text, String mimeType, String fileUri) {
        Part textPart = new Part(text);
        Part fileDataPart = new Part(new FileData(mimeType, fileUri));
        ArrayList<Part> parts = new ArrayList<Part>();
        parts.add(textPart);
        parts.add(fileDataPart);
        contents.add(new Content(parts));
        return this;
    }

    public GenerateContentRequest setTools(Tool tools) {
        this.tools = new Tool[] { tools };
        return this;
    }

    public GenerateContentRequest setToolConfig(ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
        return this;
    }

    // removes an item from the context history and returns its value
    public Content removeMessage(int index) {
        return this.contents.remove(index);
    }

    // returns the message content at a given index
    public Content getMessage(int index) {
        return this.contents.get(index);
    }

    public void replaceMessage(int index, String message) {
        this.contents.set(index, new Content(this.contents.get(index).getRole(), message));
    }

    public void replaceMessage(int index, List<String> message) {
        this.contents.set(index, new Content(this.contents.get(index).getRole(), message));
    }

    public void replaceMessage(int index, Content message) {
        this.contents.set(index, message);
    }

    public void addMessage(Content.Role role, String content) {
        this.contents.add(new Content(role, content));
    }

    public void addMessage(Content content) {
        this.contents.add(content);
    }

    public void addMessage(Content.Role role, String[] content) {
        this.contents.add(new Content(role, content));
    }

    public void addMessage(Content.Role role, List<String> content) {
        this.contents.add(new Content(role, content));
    }

    public void addMessage(Content.Role role, ArrayList<String> content) {
        this.contents.add(new Content(role, content));
    }

    public void addMessage(Content.Role role, String content, int index) {
        this.contents.add(index, new Content(role, content));
    }

    public void addMessage(Content.Role role, List<String> content, int index) {
        this.contents.add(index, new Content(role, content));
    }

    public int getMessagesSize() {
        return this.contents.size();
    }

    public boolean isLatestMessageFromModel() {
        return contents.getLast().getRole() == Role.model;
    }

    public boolean isEmpty() {
        return this.contents.size() < 1;
    }

    public ArrayList<Content> getContents() {
        return contents;
    }

    public ToolConfig getToolConfig() {
        return toolConfig;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

}