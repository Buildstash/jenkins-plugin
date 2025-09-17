package com.buildstash;

/**
 * Data model for presigned URL response from Buildstash.
 * Contains the presigned URL for uploading a specific chunk.
 */
public class PresignedUrlResponse {

    private String partPresignedUrl;

    // Default constructor for JSON deserialization
    public PresignedUrlResponse() {}

    public PresignedUrlResponse(String partPresignedUrl) {
        this.partPresignedUrl = partPresignedUrl;
    }

    // Getters and Setters
    public String getPartPresignedUrl() { return partPresignedUrl; }
    public void setPartPresignedUrl(String partPresignedUrl) { this.partPresignedUrl = partPresignedUrl; }
} 