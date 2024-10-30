package net.bigyous.gptgodmc.GPT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import de.maxhenkel.voicechat.api.VoicechatApi;
import net.bigyous.gptgodmc.AudioFileManager;
import net.bigyous.gptgodmc.GPTGOD;
import net.bigyous.gptgodmc.GPT.Json.SpeechifyGenerateRequest;
import net.bigyous.gptgodmc.GPT.Json.SpeechifyGenerateResponse;
import net.bigyous.gptgodmc.utils.QueuedAudio;
import javax.sound.sampled.*;

public class Speechify {
    private static GsonBuilder gson = new GsonBuilder();
    private static ExecutorService pool = Executors.newCachedThreadPool();
    private static String SPEECH_ENDPOINT = "https://api.sws.speechify.com/v1/audio/speech";
    private static VoicechatApi api = GPTGOD.VC_SERVER;
    private static FileConfiguration config = JavaPlugin.getPlugin(GPTGOD.class).getConfig();

    public static byte[] resampleWavToPCM(byte[] wavBytes, int targetSampleRate)
            throws UnsupportedAudioFileException, IOException {
        // Step 1: Read WAV data into an AudioInputStream
        ByteArrayInputStream byteStream = new ByteArrayInputStream(wavBytes);
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(byteStream);

        // Step 2: Define the target format
        AudioFormat originalFormat = originalStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                targetSampleRate, // Target sample rate
                originalFormat.getSampleSizeInBits(),
                originalFormat.getChannels(),
                originalFormat.getChannels() * (originalFormat.getSampleSizeInBits() / 8), // Frame size
                targetSampleRate,
                originalFormat.isBigEndian());

        // Step 3: Convert to the target format
        AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);

        // Step 4: Extract PCM bytes
        ByteArrayOutputStream pcmOutput = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = convertedStream.read(buffer)) != -1) {
            pcmOutput.write(buffer, 0, bytesRead);
        }

        // Clean up
        originalStream.close();
        convertedStream.close();

        return pcmOutput.toByteArray(); // Return the raw PCM data
    }

    public static void makeSpeech(String input, Player player) {
        Collection<? extends Player> online = GPTGOD.SERVER.getOnlinePlayers();
        Player[] players = player == null ? online.toArray(new Player[online.size()]) : new Player[] { player };
        makeTTsRequest(new SpeechifyGenerateRequest(input, config.getString("speechify-voice")), players);
    }

    private static void makeTTsRequest(SpeechifyGenerateRequest body, Player[] players) {
        pool.execute(() -> {
            GPTGOD.LOGGER.info("POSTING " + gson.setPrettyPrinting().create().toJson(body));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SPEECH_ENDPOINT))
                    .header("accept", "*/*")
                    .header("content-type", "application/json")
                    .header("Authorization", "Bearer " + config.getString("speechify-key"))
                    .method("POST", HttpRequest.BodyPublishers.ofString(gson.create().toJson(body)))
                    .build();

            String responseBody = "";

            try {
                HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofString());
                responseBody = response.body();
                SpeechifyGenerateResponse speech = gson.create().fromJson(responseBody,
                        SpeechifyGenerateResponse.class);
                byte[] rawSamples = Base64.getDecoder().decode(speech.getAudio_data());
                short[] pcmBytes = api.getAudioConverter()
                        .bytesToShorts(resampleWavToPCM(rawSamples, AudioFileManager.SAMPLE_RATE));
                QueuedAudio.playAudio(pcmBytes, players);

            } catch (IOException e) {
                GPTGOD.LOGGER.error("There was an error making a request to Speechify", e);
            } catch (InterruptedException e) {
                GPTGOD.LOGGER.error("There was an error making a request to Speechify", e);
            } catch (UnsupportedAudioFileException e) {
                GPTGOD.LOGGER.error("There was an error processing the Speechify audio", e);
            } catch (JsonSyntaxException e) {
                GPTGOD.LOGGER.error("There was an error processing the Speechify response " + responseBody, e);
            } catch (JsonParseException e) {
                GPTGOD.LOGGER.error("There was an error processing the Speechify response " + responseBody, e);
            }
        });
    }

}
