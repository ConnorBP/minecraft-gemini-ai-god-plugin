package net.bigyous.gptgodmc.GPT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.GPT.Json.Candidate;
import net.bigyous.gptgodmc.GPT.Json.Content;
import net.bigyous.gptgodmc.GPT.Json.FunctionCall;
import net.bigyous.gptgodmc.GPT.Json.FunctionDeclaration;
import net.bigyous.gptgodmc.GPT.Json.GenerateContentRequest;
import net.bigyous.gptgodmc.GPT.Json.GenerateContentResponse;
import net.bigyous.gptgodmc.GPT.Json.GptModel;
import net.bigyous.gptgodmc.GPT.Json.ModelSerializer;
import net.bigyous.gptgodmc.GPT.Json.ParameterExclusion;
import net.bigyous.gptgodmc.GPT.Json.Part;
import net.bigyous.gptgodmc.GPT.Json.Tool;
import net.bigyous.gptgodmc.GPT.Json.ToolConfig;
import net.bigyous.gptgodmc.utils.GPTUtils;
import net.bigyous.gptgodmc.GPT.Json.Content.Role;
import net.bigyous.gptgodmc.GPT.Json.FunctionCallingConfig.Mode;

public class GptAPI {
    private GsonBuilder gson = new GsonBuilder();

    private GptModel model;
    private GenerateContentRequest body;
    private String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    // keep track of what index each type of context is stored at
    // should never have an index greater than contextHeight
    private Map<String, Integer> messageMap = new HashMap<String, Integer>();
    // the index that instructions end at and rolling context starts
    private int contextHeight = 0;
    // keep track of how many tokens are in total chat history
    private int totalTokens = 0;

    private boolean isSending = false;
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static JavaPlugin plugin = JavaPlugin.getPlugin(GPTGOD.class);

    public GptAPI(GptModel model, double temperature) {
        Tool allTools = GptActions.GetAllTools();
        totalTokens += GPTUtils.calculateToolTokens(allTools);
        this.model = model;
        this.body = new GenerateContentRequest(allTools, temperature);
        gson.registerTypeAdapter(GptModel.class, new ModelSerializer());
        gson.setExclusionStrategies(new ParameterExclusion());

        // fail to startup if gpt model token limit is too small
        if (totalTokens > this.getMaxTokens()) {
            throw new RuntimeException(
                    "system instruction alone is more than gpt-model-token-limit. Please increase it to some value higher than "
                            + this.getMaxTokens());
        }
    }

    public GptAPI(GptModel model) {
        Tool allTools = GptActions.GetAllTools();
        totalTokens += GPTUtils.calculateToolTokens(allTools);
        this.model = model;
        this.body = new GenerateContentRequest(allTools);
        gson.registerTypeAdapter(GptModel.class, new ModelSerializer());
        gson.setExclusionStrategies(new ParameterExclusion());

        if (totalTokens > this.getMaxTokens()) {
            throw new RuntimeException(
                    "system instruction alone is more than gpt-model-token-limit. Please increase it to some value higher than "
                            + this.getMaxTokens());
        }
    }

    public GptAPI(GptModel model, Tool customTools, double tempurature) {
        this.model = model;
        this.body = new GenerateContentRequest(customTools, tempurature);
        totalTokens += GPTUtils.calculateToolTokens(customTools);
        gson.registerTypeAdapter(GptModel.class, new ModelSerializer());
        gson.setExclusionStrategies(new ParameterExclusion());

        if (totalTokens > this.getMaxTokens()) {
            throw new RuntimeException(
                    "system instruction alone is more than gpt-model-token-limit. Please increase it to some value higher than "
                            + this.getMaxTokens());
        }
    }

    public GptAPI(GptModel model, Tool customTools) {
        this.model = model;
        this.body = new GenerateContentRequest(customTools);
        totalTokens += GPTUtils.calculateToolTokens(customTools);
        gson.registerTypeAdapter(GptModel.class, new ModelSerializer());
        gson.setExclusionStrategies(new ParameterExclusion());

        if (totalTokens > this.getMaxTokens()) {
            throw new RuntimeException(
                    "system instruction alone is more than gpt-model-token-limit. Please increase it to some value higher than "
                            + this.getMaxTokens());
        }
    }

    public GptAPI(GptModel model, GenerateContentRequest request) {
        this.model = model;
        this.body = request;
        Tool[] tools = body.getTools();
        if (tools != null && tools.length > 0) {
            for (Tool tool : tools) {
                totalTokens += GPTUtils.calculateToolTokens(tool);
            }
        }
        totalTokens += request.getSystemInstruction().countTokens();
        gson.registerTypeAdapter(GptModel.class, new ModelSerializer());
        gson.setExclusionStrategies(new ParameterExclusion());

        if (totalTokens > this.getMaxTokens()) {
            throw new RuntimeException(
                    "system instruction alone is more than gpt-model-token-limit. Please increase it to some value higher than "
                            + this.getMaxTokens());
        }
    }

    // remove and return the oldest chat history
    // excepting any entires under the contextHeight
    public Content popOldestContent() {
        return this.body.removeMessage(contextHeight);
    }

    // remove and return the oldest chat history until we are within the token limit
    // excepting any entires under the contextHeight
    // if provided, nextTokenLength ensures that there is room for the next token
    // addition
    public void cull(int nextTokenLength) {
        // get the configured token maximum for this model
        // and set the goal to that minus the headroom needed for our next prompt
        int tokenLimit = this.getMaxTokens() - nextTokenLength;

        if (totalTokens > tokenLimit) {
            GPTGOD.LOGGER.info("running cull operation from " + totalTokens + " down to " + tokenLimit);
        }

        while (totalTokens > tokenLimit && this.body.getMessagesSize() > contextHeight) {
            Content oldest = this.popOldestContent();
            totalTokens -= oldest.countTokens();
        }

        if (totalTokens > tokenLimit) {
            GPTGOD.LOGGER.warn("GPT token count " + totalTokens + " is greater than maximum of " + tokenLimit);
        }
    }

    public void cull() {
        cull(0);
    }

    // push a message to the index at the current stack height
    // then increase the context stack height
    // and return the height
    private int pushContextStack(String context) {
        GPTGOD.LOGGER.info("Pushing context stack height up one from " + contextHeight + " to " + (contextHeight + 1));
        // get current stack height then increment
        int insertedAtIndex = contextHeight++;
        // increment token count of message history
        totalTokens += GPTUtils.countTokens(context);
        this.body.addMessage(Content.Role.user, context, insertedAtIndex);
        return insertedAtIndex;
    }

    private int pushContextStack(List<String> context) {
        GPTGOD.LOGGER.info("Pushing context stack height up one from " + contextHeight + " to " + (contextHeight + 1));
        // get current stack height then increment
        int insertedAtIndex = contextHeight++;
        // increment token count of message history
        totalTokens += GPTUtils.countTokens(context);
        this.body.addMessage(Content.Role.user, context, insertedAtIndex);
        return insertedAtIndex;
    }

    // replace message content at index and update total running token count
    private void replaceMessage(int index, String message) {
        int oldMsgTokens = this.body.getMessage(index).countTokens();
        int newMessageTokens = GPTUtils.countTokens(message);
        // replace the message
        this.body.replaceMessage(index, message);
        // update the token total with the difference between the old and new message
        totalTokens += (newMessageTokens - oldMsgTokens);
    }

    // replace message content at index and update total running token count
    private void replaceMessage(int index, List<String> message) {
        int oldMsgTokens = this.body.getMessage(index).countTokens();
        int newMessageTokens = GPTUtils.countTokens(message);
        // replace the message
        this.body.replaceMessage(index, message);
        // update the token total with the difference between the old and new message
        totalTokens += (newMessageTokens - oldMsgTokens);
    }

    public GptAPI addContext(String context, String name) {
        if (this.messageMap.containsKey(name)) {
            this.replaceMessage(messageMap.get(name), context);
            return this;
        }
        // push message to context stack then add its index to the message map
        // also increments totalTokens
        this.messageMap.put(name, pushContextStack(context));
        return this;
    }

    public GptAPI addFileWithContext(String context, String fileMimeType, String fileUri) {
        this.body.addFileWithPrompt(context, fileMimeType, fileUri);
        totalTokens += GPTUtils.countTokens(context);
        return this;
    }

    // sets the system direction parameter
    public GptAPI setSystemContext(String context) {
        Content oldInstruction = this.body.getSystemInstruction();
        int newTokenCount = GPTUtils.countTokens(context);
        if (oldInstruction == null) {
            this.totalTokens += newTokenCount;
        } else {
            int oldCount = oldInstruction.countTokens();
            this.totalTokens += (newTokenCount - oldCount);
        }
        this.body.setSystemInstruction(context);
        return this;
    }

    public GptAPI setSystemContext(String[] context) {
        Content oldInstruction = this.body.getSystemInstruction();
        int newTokenCount = GPTUtils.countTokens(context);
        if (oldInstruction == null) {
            this.totalTokens += newTokenCount;
        } else {
            int oldCount = oldInstruction.countTokens();
            this.totalTokens += (newTokenCount - oldCount);
        }
        this.body.setSystemInstruction(context);
        return this;
    }

    public GptAPI setTools(Tool tools) {
        Tool[] oldTools = this.body.getTools();
        int newTokenCount = GPTUtils.calculateToolTokens(tools);
        if (oldTools == null) {
            this.totalTokens += newTokenCount;
        } else {
            int oldCount = 0;
            for (Tool oldTool : oldTools) {
                oldCount += GPTUtils.calculateToolTokens(oldTool);
            }
            this.totalTokens += (newTokenCount - oldCount);
        }
        this.body.setTools(tools);
        return this;
    }

    // adds server logs context
    // same as addContext but takes in a list of events
    public GptAPI addLogs(List<String> Logs, String name) {
        if (this.messageMap.containsKey(name)) {
            this.replaceMessage(messageMap.get(name), Logs);
            return this;
        }
        this.messageMap.put(name, pushContextStack(Logs));
        return this;
    }

    // this is just an alias really
    public GptAPI addLogs(String Logs, String name) {
        this.addContext(Logs, name);
        return this;
    }

    public GptAPI addResponse(Content responseContent) {
        GPTGOD.LOGGER.info("Adding response " + gson.create().toJson(responseContent));
        this.body.addMessage(responseContent);
        this.totalTokens += responseContent.countTokens();
        return this;
    }

    public GptAPI addMessage(String message) {
        GPTGOD.LOGGER.info("Adding prompt to get response: " + message);
        this.body.addMessage(Role.user, message);
        this.totalTokens += GPTUtils.countTokens(message);
        return this;
    }

    public GptAPI addMessages(String[] messages) {
        // GPTGOD.LOGGER.info("Adding prompt to get response: " + String.join("\n",
        // messages) );
        this.body.addMessage(Role.user, messages);
        this.totalTokens += GPTUtils.countTokens(messages);
        return this;
    }

    public GptAPI setToolChoice(String tool_choice) {
        this.body.setToolConfig(new ToolConfig(new String[] { tool_choice }));
        return this;
    }

    public GptAPI setToolOnlyAllTools() {
        ArrayList<String> toolNames = new ArrayList<>();
        // add all toolNames to required tools
        for (Tool tool : this.body.getTools()) {
            for (FunctionDeclaration func : tool.getFunctions()) {
                toolNames.add(func.getName());
            }
        }
        this.body.setToolConfig(new ToolConfig(Mode.ANY, toolNames.toArray(new String[toolNames.size()])));
        return this;
    }

    // public void removeLastMessage() {
    // this.body.removeLastMessage();
    // }

    public int getMaxTokens() {
        return model.getTokenLimit();
    }

    public String getModelName() {
        return model.getName();
    }

    public boolean isLatestMessageFromModel() {
        return this.body.isLatestMessageFromModel();
    }

    public void send() {
        CloseableHttpClient client = HttpClientBuilder.create().build();

        this.isSending = true;
        pool.execute(() -> {
            FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();
            StringEntity data = new StringEntity(gson.create().toJson(body), ContentType.APPLICATION_JSON);
            GPTGOD.LOGGER.info("POSTING " + gson.setPrettyPrinting().create().toJson(body));
            HttpPost post = new HttpPost(
                    BASE_URL + model.getName() + ":generateContent" + "?key=" + config.getString("geminiKey"));
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            GPTGOD.LOGGER.info("Making POST request");
            post.setEntity(data);
            try {
                HttpResponse response = client.execute(post);
                String out = new String(response.getEntity().getContent().readAllBytes());
                EntityUtils.consume(response.getEntity());
                GPTGOD.LOGGER.info("recieved response from Gemini: " + out);
                if (response.getStatusLine().getStatusCode() != 200) {
                    GPTGOD.LOGGER.warn("API call failed");
                    this.isSending = false;
                }
                processResponse(out);
                client.close();
                // after everything finishes executing, the request is finished
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GPTGOD.class), () -> {
                    this.isSending = false;
                }, 10);
            } catch (IOException e) {
                GPTGOD.LOGGER.error("There was an error making a request to GPT", e);
                this.isSending = false;
            }
        });
    }

    public void send(Map<String, FunctionDeclaration> functions) {
        CloseableHttpClient client = HttpClientBuilder.create().build();

        this.isSending = true;
        pool.execute(() -> {
            FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();
            StringEntity data = new StringEntity(gson.create().toJson(body), ContentType.APPLICATION_JSON);
            GPTGOD.LOGGER.info("POSTING " + gson.setPrettyPrinting().create().toJson(body));
            HttpPost post = new HttpPost(
                    BASE_URL + model.getName() + ":generateContent" + "?key=" + config.getString("geminiKey"));
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            GPTGOD.LOGGER.info("Making POST request");
            post.setEntity(data);
            try {
                HttpResponse response = client.execute(post);
                String out = new String(response.getEntity().getContent().readAllBytes());
                EntityUtils.consume(response.getEntity());
                GPTGOD.LOGGER.info("recieved response from Gemini: " + out);
                if (response.getStatusLine().getStatusCode() != 200) {
                    GPTGOD.LOGGER.warn("API call failed");
                    this.isSending = false;
                }
                processResponse(out, functions);
                client.close();
                // after everything finishes, executing the request is finished
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(GPTGOD.class), () -> {
                    this.isSending = false;
                }, 20);
            } catch (IOException e) {
                GPTGOD.LOGGER.error("There was an error making a request to GPT", e);
                this.isSending = false;
            }
        });
    }

    public boolean isSending() {
        return isSending;
    }

    // DEBUG method
    public void checkRequestBody() {
        GPTGOD.LOGGER.info("POSTING " + gson.setPrettyPrinting().create().toJson(body));
    }

    private void processResponse(String response) {
        // shadow gson builder with gson
        Gson gson = this.gson.create();

        GenerateContentResponse responseObject = gson.fromJson(response, GenerateContentResponse.class);

        if (responseObject.isError()) {
            GPTGOD.LOGGER.error(responseObject.getError().toString());
            if (responseObject.getError().getStatus() == "INVALID_ARGUMENT") {
                GPTGOD.LOGGER.info(
                        "GEMINI API RETURNED INVALID ARGUMENT! Suggestion: Double check that the gemini model names are correct.");
            }
            return;
        }

        // overwrite our rough guess of a total with the actual token total from the
        // last request
        int promptTokenCount = responseObject.getUsageMetadata().getPromptTokenCount();
        if (promptTokenCount > 0) {
            GPTGOD.LOGGER.info("setting GPT token count to " + promptTokenCount);
            this.totalTokens = promptTokenCount;
        }

        // run all candidates for now if their parts are not null
        for (Candidate choice : responseObject.getCandidates()) {
            ArrayList<Part> parts = choice.getContent().getParts();
            if (parts == null) {
                continue;
            }

            // add non null candidates to response history for multi-turn
            this.addResponse(choice.getContent());

            for (Part call : parts) {
                FunctionCall func = call.getFunctionCall();
                if (func == null) {
                    continue;
                }
                System.out
                        .println("Trying to execute function " + func.getName() + " with args: " + func.getArguments());
                GptActions.run(func.getName(), func.getArguments());
            }
        }
    }

    private void processResponse(String response, Map<String, FunctionDeclaration> functions) {
        // shadow gson builder with gson
        Gson gson = this.gson.create();

        GenerateContentResponse responseObject = gson.fromJson(response, GenerateContentResponse.class);

        if (responseObject.isError()) {
            GPTGOD.LOGGER.error("error loading gemini response: " + responseObject.getError().toString());
        }

        // overwrite our rough guess of a total with the actual token total from the
        // last request
        int promptTokenCount = responseObject.getUsageMetadata().getPromptTokenCount();
        if (promptTokenCount > 0) {
            GPTGOD.LOGGER.info("setting GPT token count to " + promptTokenCount);
            this.totalTokens = promptTokenCount;
        }

        for (Candidate cand : responseObject.getCandidates()) {
            ArrayList<Part> parts = cand.getContent().getParts();
            if (parts == null) {
                continue;
            }

            // add non null candidates to response history for multi-turn
            this.addResponse(cand.getContent());

            for (Part call : parts) {
                FunctionCall func = call.getFunctionCall();
                if (func == null) {
                    continue;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    System.out.println("Trying to execute function " + func.getName() + " from map with args: "
                            + func.getArguments());
                    functions.get(func.getName()).runFunction(func.getArguments());
                });
            }
        }
    }
}
