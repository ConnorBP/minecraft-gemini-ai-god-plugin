package net.bigyous.gptgodmc.GPT;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.github.mizosoft.methanol.Methanol;

import java.nio.file.Path;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.bigyous.gptgodmc.EventLogger;
import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.GPT.Json.FunctionDeclaration;
import net.bigyous.gptgodmc.GPT.Json.GenerateContentRequest;
import net.bigyous.gptgodmc.GPT.Json.GenerateContentResponse;
import net.bigyous.gptgodmc.GPT.Json.GptModel;
import net.bigyous.gptgodmc.GPT.Json.Schema;
import net.bigyous.gptgodmc.GPT.Json.Tool;
import net.bigyous.gptgodmc.interfaces.Function;
import net.bigyous.gptgodmc.loggables.ChatLoggable;

public class Transcription {

    private static String BASE_URL = "https://generativelanguage.googleapis.com";
    private static Gson gson = new Gson();
    private static Methanol client = Methanol.create();
    private static FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();

    private class TranscriptionResult {
        String playerName;
        String minecraftTime;
        String transcribedMessage;

    }

    private static Function<JsonObject> submitTranscriptions = (JsonObject args) -> {
        TranscriptionResult[] messages = gson.fromJson(args.get("transcriptionResults"), TranscriptionResult[].class);

        for (TranscriptionResult message : messages) {
            GPTGOD.LOGGER.info(String.format("transcribed %s %s said: %s", message.minecraftTime, message.playerName,
                    message.transcribedMessage));
            EventLogger.addLoggable(
                    new ChatLoggable(message.playerName, message.minecraftTime, message.transcribedMessage));
        }
    };

    private static Map<String, FunctionDeclaration> functionMap = Map.of("submitTranscriptions",
            new FunctionDeclaration(
                    "submitTranscriptions",
                    "receives a list of transcription results for each player voice chat which was decoded.",
                    new Schema(Map.of(
                            "transcriptionResults",
                            new Schema(Schema.Type.ARRAY, "", Map.of(
                                    "playerName", new Schema(Schema.Type.STRING),
                                    "minecraftTime", new Schema(Schema.Type.STRING),
                                    "transcribedMessage", new Schema(Schema.Type.STRING))))),
                    submitTranscriptions));
    private static Tool tools = GptActions.wrapFunctions(functionMap);

    public static void TranscribeAndSubmitMany(TranscriptionRequest[] playerAudioData) {
        if (config.getString("geminiKey").isBlank()) {
            GPTGOD.LOGGER.warn("No API Key");
            return;
        }

        GptAPI gpt = new GptAPI(GPTModels.getSecondaryModel(), tools)
                .setSystemContext(
                        """
                                you are a voice chat transcriber. You will receive a series of voice chat inputs from various players in series with a minecraft 24hr time marker ([HH:MM]).
                                Transcribe what words you hear from the input audio as closly as possible to what you think people are saying.
                                If no words are heard then you may return descriptions of what you hear inbetween of astrix cahracters e.x. *birds chirping* or *nothing*.
                                DO NOT make stuff up, rather prefer an empty transcription over made up sounds if no words are heard.
                                Try to group voice chat results from the same user together if no other player spoke inbetween them.
                                Return a list of transcribed messages of what each player said in sequence
                                for example: [23:06] John said: how are you doing? [23:10] Jane said: i'm doing okay.
                                """)
                .setTools(tools)
                .setToolChoice("submitTranscriptions");

        for (TranscriptionRequest req : playerAudioData) {
            gpt.addFileWithContext(String.format("audio fragment from player %s at time %s in the minecraft world",
                    req.getPlayerName(), req.getTimeStamp()), req.getFileMimeType(), req.getUri());
        }

        // send off the request without waiting for it.
        gpt.send(functionMap);
    }

    public static String Transcribe(Path audioPath) {
        if (config.getString("geminiKey").isBlank()) {
            GPTGOD.LOGGER.warn("No API Key");
            return "something";
        }

        String apiKey = config.getString("geminiKey");
        GptModel model = GPTModels.getSecondaryModel();

        try {
            GoogleFile file = new GoogleFile(audioPath);

            if (!file.tryUpload()) {
                GPTGOD.LOGGER.error("Transcription failed during upload");
                return "something";
            }

            // Create the content request object
            GenerateContentRequest contentRequest = new GenerateContentRequest()
                    .setSystemInstruction(
                            """
                                    you are a voice chat transcriber.
                                    Try to transcribe what you hear from the input audio as closly as possible to what you think people are saying.
                                    If no words are heard then you may return descriptions of what you hear inbetween of astrix cahracters e.x. *birds chirping* or *nothing*.
                                    DO NOT make stuff up, rather prefer an empty transcription over made up sounds if no words are heard.
                                    """)
                    .addFileWithPrompt("transcribe this audio clip", "audio/wav", file.getUri());
            String jsonBody = gson.toJson(contentRequest);
            System.out.println("jsonBody");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1beta/models/" + model.getName()
                            + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            System.out.println("Received transcription response:\n" + response.body());

            return gson.fromJson(response.body(), GenerateContentResponse.class).getText();
        } catch (FileNotFoundException e) {
            GPTGOD.LOGGER.error("Attempted to Transcribe non-existant file", e);
            e.printStackTrace();
        } catch (IOException e) {
            GPTGOD.LOGGER.error("An error occured during Transcription", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            GPTGOD.LOGGER.error("Transcription Interrupted", e);
        }
        return "something";
    }

}
