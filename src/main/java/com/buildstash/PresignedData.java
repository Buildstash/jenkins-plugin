package com.buildstash;

import java.util.Map;

/**
 * Data model for presigned URL data from Buildstash.
 * Contains the URL and headers needed for direct file uploads.
 */
public class PresignedData {

    private String url;
    private Map<String, String> headers;

    // Default constructor for JSON deserialization
    public PresignedData() {}

    public PresignedData(String url, Map<String, String> headers) {
        this.url = url;
        this.headers = headers;
    }

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
} 