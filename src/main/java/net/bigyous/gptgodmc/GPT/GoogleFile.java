package net.bigyous.gptgodmc.GPT;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.mizosoft.methanol.Methanol;
import com.google.gson.Gson;

import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.GPT.Json.FileUploadResponse;

// helper for gemini file api
// https://ai.google.dev/gemini-api/docs/audio?lang=rest
// https://ai.google.dev/api/files#files_create_image-SHELL
public class GoogleFile {
    private static String BASE_URL = "https://generativelanguage.googleapis.com";
    private static Gson gson = new Gson();
    private static Methanol client = Methanol.create();
    private static FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();
    private static String apiKey = config.getString("geminiKey");

    private String displayName = "AUDIO";
    private Path filePath;
    private String uriResult;

    public String getUri() {
        return this.uriResult;
    }

    public GoogleFile(Path filePath) {
        this.filePath = filePath;
    }

    public GoogleFile(Path filePath, String displayName) {
        this.filePath = filePath;
        this.displayName = displayName;
    }

    public boolean tryUpload() {
        if (!Files.exists(filePath)) {
            GPTGOD.LOGGER.error("Attempted to Upload non-existant file",
                    new FileNotFoundException("No such file " + filePath));
            return false;
        }

        try {

            // firstly get file metadata
            String mimeType = Files.probeContentType(filePath);
            long numBytes = Files.size(filePath);
            String metadataJson = "{\"file\": {\"display_name\": \"" + displayName + "\"}}";

            HttpRequest metadataRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/upload/v1beta/files" + "?key=" + apiKey))
                    .header("X-Goog-Upload-Protocol", "resumable")
                    .header("X-Goog-Upload-Command", "start")
                    .header("X-Goog-Upload-Header-Content-Length", String.valueOf(numBytes))
                    .header("X-Goog-Upload-Header-Content-Type", mimeType)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(metadataJson))
                    .build();
            HttpResponse<String> metadataResponse = client.send(metadataRequest, BodyHandlers.ofString());

            int code = metadataResponse.statusCode();
            if (code != 200) {
                throw new RuntimeException("Initializing upload with metadata failed with code " + code);
            }

            String uploadUrl = metadataResponse.headers()
                    .firstValue("x-goog-upload-url")
                    .orElseThrow(() -> new RuntimeException("No upload URL found in metadata response"));

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    // .header("Content-Length", String.valueOf(numBytes))
                    .header("X-Goog-Upload-Offset", "0")
                    .header("X-Goog-Upload-Command", "upload, finalize")
                    .header("Content-Type", mimeType)
                    .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                    .build();

            HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

            code = uploadResponse.statusCode();
            if (code != 200) {
                throw new RuntimeException("Uploading data failed with code " + code);
            }

            // store the resultant uri result upon success
            this.uriResult = gson.fromJson(uploadResponse.body(), FileUploadResponse.class).getUri();
        } catch (FileNotFoundException e) {
            GPTGOD.LOGGER.error("Attempted to Upload non-existant file", e);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            GPTGOD.LOGGER.error("An error occured during Google upload", e);
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            GPTGOD.LOGGER.error("Upload Interrupted", e);
            return false;
        } catch (RuntimeException e) {
            GPTGOD.LOGGER.error("Upload failed with exception", e);
            return false;
        }

        return true;
    }

}
