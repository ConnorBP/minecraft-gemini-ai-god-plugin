package net.bigyous.gptgodmc;

import net.bigyous.gptgodmc.utils.BukkitUtils;
import net.bigyous.gptgodmc.utils.TaskQueue;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.bigyous.gptgodmc.GPT.GoogleFile;
import net.bigyous.gptgodmc.GPT.Transcription;
import net.bigyous.gptgodmc.GPT.TranscriptionRequest;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

@ForgeVoicechatPlugin
public class VoiceMonitorPlugin implements VoicechatPlugin {

    private static ConcurrentHashMap<UUID, PlayerAudioBuffer> buffers;
    private static ConcurrentHashMap<UUID, OpusDecoder> decoders;

    // split queues into three. An upload queue, an accumulator, and the one to call
    // the LLM every so often
    // this ensures that mic spam will not spam the LLM endpoint
    // takes in a players audio buffer and sends it to google
    private static TaskQueue<PlayerAudioBuffer> uploadingQueue;
    // the file uri is then added to this intermediary queue to then be bundled off
    private static Queue<TranscriptionRequest> queue = new LinkedList<>();

    private static JavaPlugin plugin = JavaPlugin.getPlugin(GPTGOD.class);
    private static FileConfiguration config = plugin.getConfig();
    private static int transcriptionRate = config.getInt("transcription-rate") < 1 ? 20
            : config.getInt("transcription-rate");

    private static int bundleTaskId;

    public static Queue<TranscriptionRequest> getQueue() {
        return queue;
    }

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return GPTGOD.PLUGIN_ID;
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {
        GPTGOD.LOGGER.info("voice monitor initialized");
        GPTGOD.VC_SERVER = api;
        buffers = new ConcurrentHashMap<UUID, PlayerAudioBuffer>();
        decoders = new ConcurrentHashMap<UUID, OpusDecoder>();
        uploadingQueue = new TaskQueue<PlayerAudioBuffer>((PlayerAudioBuffer buffer) -> {

            GoogleFile audioFile = new GoogleFile(
                    AudioFileManager.getPlayerFile(buffer.getPlayer(), buffer.getBufferId()));
            boolean uploadSuccess = audioFile.tryUpload();
            // delete the file from disk after a single upload try
            // (if we miss the window on transcoding the file due to an error oh well)
            AudioFileManager.deleteFile(buffer.getPlayer(), buffer.getBufferId());
            // if upload didn't fail we submit the file uri to the transcode queue
            if (uploadSuccess) {
                if (!queue.add(new TranscriptionRequest(audioFile.getUri(), "audio/wav", buffer.getPlayer().getName(),
                        buffer.getTimeStamp()))) {
                    GPTGOD.LOGGER.warn("failed to add " + buffer.getPlayer().getName()
                            + "'s audio file to the transcription queue!");
                }
            }
        });

        // setup the transcription request bundler
        BukkitTask task = GPTGOD.SERVER.getScheduler().runTaskTimerAsynchronously(plugin,
                new TranscriptionBundlerTask(), BukkitUtils.secondsToTicks(30),
                BukkitUtils.secondsToTicks(transcriptionRate));
        bundleTaskId = task.getTaskId();
    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnect);
        registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        VoicechatConnection senderConnection = event.getSenderConnection();
        byte[] encodedData = event.getPacket().getOpusEncodedData();
        if (senderConnection == null) {
            return;
        }
        if (!(senderConnection.getPlayer().getPlayer() instanceof Player player)) {
            // GPTGOD.LOGGER.warn("Received microphone packets from non-player");
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // GPTGOD.LOGGER.info(String.format("Player: %s Sent packet of length: %d",
        // player.getName(), encodedData.length));
        if (!decoders.containsKey(player.getUniqueId())) {
            decoders.put(player.getUniqueId(), event.getVoicechat().createDecoder());
            // GPTGOD.LOGGER.info(String.format("opusDecoder created for UUID: %s",
            // player.getUniqueId().toString()));
        }
        OpusDecoder decoder = decoders.get(player.getUniqueId());
        short[] decoded = decoder.decode(event.getPacket().getOpusEncodedData());

        if (encodedData.length > 0) {
            if (!buffers.containsKey(player.getUniqueId())) {
                PlayerAudioBuffer buffer = new PlayerAudioBuffer(decoded, player, event.getVoicechat());
                buffers.put(player.getUniqueId(), buffer);
                // GPTGOD.LOGGER.info(String.format("AudioBuffer #%d created for UUID: %s",
                // buffer.getBufferId(), player.getUniqueId().toString()));
            } else {
                buffers.get(player.getUniqueId()).addSamples(decoded);
            }
        } else {
            // GPTGOD.LOGGER.info(String.format("decoders: %s, buffers: %s",
            // decoders.toString(), buffers.toString()));
            PlayerAudioBuffer toBeProcessed = buffers.get(player.getUniqueId());
            toBeProcessed.createWAV();
            uploadingQueue.insert(toBeProcessed);
            buffers.remove(player.getUniqueId());
            decoder.resetState();
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectedEvent event) {
        cleanUpPlayer(event.getPlayerUuid(), event.getVoicechat());
    }

    private void onServerStopped(VoicechatServerStoppedEvent event) {
        decoders.forEach((key, value) -> cleanUpPlayer(key, event.getVoicechat()));
    }

    private void cleanUpPlayer(UUID uuid, VoicechatServerApi vc) {

        AudioFileManager.deletePlayerData(uuid);
        if (!decoders.containsKey(uuid)) {
            GPTGOD.LOGGER.info(
                    String.format("Cleaned up data for UUID: %s, there was no decoder to clean", uuid.toString()));
            return;
        }
        decoders.get(uuid).close();
        decoders.remove(uuid);
        GPTGOD.LOGGER.info(String.format("Cleaned up data for UUID: %s", uuid.toString()));
    }

    // should be called before plugin is unregistered
    public void stop() {
        // stop the bundler task
        GPTGOD.SERVER.getScheduler().cancelTask(bundleTaskId);
    }

    private static class TranscriptionBundlerTask implements Runnable {
        // once every interval (unless empty) the audio queue is sent to the bundled
        // queue to be transcribed
        private static TaskQueue<TranscriptionRequest[]> bundledTranscriptionQueue;

        public TranscriptionBundlerTask() {
            bundledTranscriptionQueue = new TaskQueue<TranscriptionRequest[]>((TranscriptionRequest[] buffer) -> {
                Transcription.TranscribeAndSubmitMany(buffer);
            });
        }

        @Override
        public void run() {

            if (VoiceMonitorPlugin.getQueue().isEmpty()) {
                return;
            }

            GPTGOD.LOGGER.info("bundling audio files for transcription");

            ArrayList<TranscriptionRequest> bundle = new ArrayList<>();

            // poll until empty
            TranscriptionRequest req = VoiceMonitorPlugin.getQueue().poll();
            while (req != null) {

                bundle.add(req);

                // poll the next one
                req = VoiceMonitorPlugin.getQueue().poll();
            }

            // convert to array and submit
            TranscriptionRequest[] bunArray = new TranscriptionRequest[bundle.size()];
            bundle.toArray(bunArray);
            bundledTranscriptionQueue.insert(bunArray);
        }

    }

}
