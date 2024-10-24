package net.bigyous.gptgodmc.GPT.Json;

public class File {
    private String name;
    private String displayName;
    private String mineType;
    private long sizeBytes;
    private String createTime;
    private String updateTime;
    private String expirationTime;
    private String sha256Hash;
    private String uri;
    private State state;
    private String error;
    private String metadata;

    public String getUri() {
        return uri;
    }
}
