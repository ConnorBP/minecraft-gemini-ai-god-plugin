package net.bigyous.gptgodmc.utils;

import net.bigyous.gptgodmc.AudioFileManager;

// little shortcut helpers
public class HackyAudio {
    public static long CountAudioSeconds(long byteCount, long sampleRate, long channelCount, long bitDepth) {

        return byteCount / (sampleRate * channelCount * (bitDepth / 8));
    }

    public static long CountAudioTokens(long byteCount, long sampleRate, long channelCount, long bitDepth) {
        // gemini audio takes up 32 tokens per second of audio

        return 32 * CountAudioSeconds(byteCount, sampleRate, channelCount, bitDepth);
    }

    public static long CountAudioTokens(long byteCount) {
        return CountAudioSeconds(byteCount, AudioFileManager.SAMPLE_RATE, 1, 16);
    }
}
