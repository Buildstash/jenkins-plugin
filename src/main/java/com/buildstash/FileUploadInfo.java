package com.buildstash;

/**
 * Data model for file upload information from Buildstash.
 * Contains metadata about how to upload a file (direct or chunked).
 */
public class FileUploadInfo {

    private boolean chunkedUpload;
    private int chunkedNumberParts;
    private int chunkedPartSizeMb;
    private PresignedData presignedData;

    // Default constructor for JSON deserialization
    public FileUploadInfo() {}

    public FileUploadInfo(boolean chunkedUpload, int chunkedNumberParts, int chunkedPartSizeMb, PresignedData presignedData) {
        this.chunkedUpload = chunkedUpload;
        this.chunkedNumberParts = chunkedNumberParts;
        this.chunkedPartSizeMb = chunkedPartSizeMb;
        this.presignedData = presignedData;
    }

    // Getters and Setters
    public boolean isChunkedUpload() { return chunkedUpload; }
    public void setChunkedUpload(boolean chunkedUpload) { this.chunkedUpload = chunkedUpload; }

    public int getChunkedNumberParts() { return chunkedNumberParts; }
    public void setChunkedNumberParts(int chunkedNumberParts) { this.chunkedNumberParts = chunkedNumberParts; }

    public int getChunkedPartSizeMb() { return chunkedPartSizeMb; }
    public void setChunkedPartSizeMb(int chunkedPartSizeMb) { this.chunkedPartSizeMb = chunkedPartSizeMb; }

    public PresignedData getPresignedData() { return presignedData; }
    public void setPresignedData(PresignedData presignedData) { this.presignedData = presignedData; }
} 