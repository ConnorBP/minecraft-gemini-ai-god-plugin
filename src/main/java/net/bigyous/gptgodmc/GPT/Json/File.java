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

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMineType() {
        return mineType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public String getExpirationTime() {
        return expirationTime;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public State getState() {
        return state;
    }

    public String getError() {
        return error;
    }

    public String getMetadata() {
        return metadata;
    }

}
