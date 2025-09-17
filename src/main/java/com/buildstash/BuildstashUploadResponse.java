package com.buildstash;

/**
 * Data model for Buildstash upload response.
 * Represents the response from the Buildstash API after a successful upload.
 */
public class BuildstashUploadResponse {

    private String buildId;
    private boolean pendingProcessing;
    private String buildInfoUrl;
    private String downloadUrl;

    // Default constructor for JSON deserialization
    public BuildstashUploadResponse() {}

    public BuildstashUploadResponse(String buildId, boolean pendingProcessing, String buildInfoUrl, String downloadUrl) {
        this.buildId = buildId;
        this.pendingProcessing = pendingProcessing;
        this.buildInfoUrl = buildInfoUrl;
        this.downloadUrl = downloadUrl;
    }

    // Getters and Setters
    public String getBuildId() { return buildId; }
    public void setBuildId(String buildId) { this.buildId = buildId; }

    public boolean isPendingProcessing() { return pendingProcessing; }
    public void setPendingProcessing(boolean pendingProcessing) { this.pendingProcessing = pendingProcessing; }

    public String getBuildInfoUrl() { return buildInfoUrl; }
    public void setBuildInfoUrl(String buildInfoUrl) { this.buildInfoUrl = buildInfoUrl; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
} 