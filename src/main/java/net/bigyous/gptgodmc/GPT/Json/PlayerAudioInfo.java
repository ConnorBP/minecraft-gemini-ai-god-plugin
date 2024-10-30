package net.bigyous.gptgodmc.GPT.Json;

// data to describe an audio clip to the LLM
public class PlayerAudioInfo {
    // a time stamp of the world time in minecraft that this audio happened at
    private String minecraftTime;
    // the name of the player this clip pertains to
    private String playerName;

    public PlayerAudioInfo(String timeStamp, String playerName) {
        this.minecraftTime = timeStamp;
        this.playerName = playerName;
    }

    public String getMinecraftTime() {
        return minecraftTime;
    }

    public String getPlayerName() {
        return playerName;
    }

}
