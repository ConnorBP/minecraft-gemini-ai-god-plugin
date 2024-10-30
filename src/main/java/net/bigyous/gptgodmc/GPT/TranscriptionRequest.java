package net.bigyous.gptgodmc.GPT;

public class TranscriptionRequest {
    private String fileUri;
    private String fileMimeType;
    private String playerName;
    private String timeStamp;

    public TranscriptionRequest(String fileUri, String fileMimeType, String playerName, String timeStamp) {
        this.fileUri = fileUri;
        this.fileMimeType = fileMimeType;
        this.playerName = playerName;
        this.timeStamp = timeStamp;
    }

    public String getUri() {
        return fileUri;
    }

    public String getFileMimeType() {
        return fileMimeType;
    }

    public Object getPlayerName() {
        return playerName;
    }

    public Object getTimeStamp() {
        return timeStamp;
    }

}
