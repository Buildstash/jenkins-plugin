package com.buildstash;

import java.util.List;

/**
 * Data model for the initial upload request response from Buildstash.
 * Contains upload URLs and metadata for file uploads.
 */
public class UploadRequestResponse {

    private String pendingUploadId;
    private FileUploadInfo primaryFile;
    private List<FileUploadInfo> expansionFiles;

    // Default constructor for JSON deserialization
    public UploadRequestResponse() {}

    public UploadRequestResponse(String pendingUploadId, FileUploadInfo primaryFile, List<FileUploadInfo> expansionFiles) {
        this.pendingUploadId = pendingUploadId;
        this.primaryFile = primaryFile;
        this.expansionFiles = expansionFiles;
    }

    // Getters and Setters
    public String getPendingUploadId() { return pendingUploadId; }
    public void setPendingUploadId(String pendingUploadId) { this.pendingUploadId = pendingUploadId; }

    public FileUploadInfo getPrimaryFile() { return primaryFile; }
    public void setPrimaryFile(FileUploadInfo primaryFile) { this.primaryFile = primaryFile; }

    public List<FileUploadInfo> getExpansionFiles() { return expansionFiles; }
    public void setExpansionFiles(List<FileUploadInfo> expansionFiles) { this.expansionFiles = expansionFiles; }
} 