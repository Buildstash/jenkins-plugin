package com.buildstash;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service class for uploading files to Buildstash.
 * Handles HTTP communication with the Buildstash API.
 */
public class BuildstashUploadService {

    private static final String API_BASE_URL = "https://app.buildstash.com/api/v1";
    private static final String UPLOAD_REQUEST_ENDPOINT = API_BASE_URL + "/upload/request";
    private static final String UPLOAD_VERIFY_ENDPOINT = API_BASE_URL + "/upload/verify";
    private static final String MULTIPART_REQUEST_ENDPOINT = API_BASE_URL + "/upload/request/multipart";
    private static final String MULTIPART_EXPANSION_ENDPOINT = API_BASE_URL + "/upload/request/multipart/expansion";

    private final String apiKey;
    private final TaskListener listener;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public BuildstashUploadService(String apiKey, TaskListener listener) {
        this.apiKey = apiKey;
        this.listener = listener;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();
    }

    public BuildstashUploadResponse upload(BuildstashUploadRequest request) throws Exception {
        // Step 1: Request upload URLs
        listener.getLogger().println("Requesting upload URLs from Buildstash...");
        UploadRequestResponse uploadRequestResponse = requestUploadUrls(request);

        // Step 2: Upload files
        listener.getLogger().println("Uploading files to Buildstash...");
        List<MultipartChunk> primaryFileParts = null;
        List<MultipartChunk> expansionFileParts = null;

        // Upload primary file
        if (uploadRequestResponse.getPrimaryFile().isChunkedUpload()) {
            listener.getLogger().println("Uploading primary file using chunked upload...");
            primaryFileParts = uploadChunkedFile(
                request.getWorkspace().child(request.getPrimaryFilePath()),
                uploadRequestResponse.getPendingUploadId(),
                uploadRequestResponse.getPrimaryFile(),
                false
            );
        } else {
            listener.getLogger().println("Uploading primary file using direct upload...");
            uploadDirectFile(
                request.getWorkspace().child(request.getPrimaryFilePath()),
                uploadRequestResponse.getPrimaryFile().getPresignedData()
            );
        }

        // Upload expansion file if present
        if (request.getExpansionFilePath() != null && uploadRequestResponse.getExpansionFiles() != null && !uploadRequestResponse.getExpansionFiles().isEmpty()) {
            FileUploadInfo expansionFile = uploadRequestResponse.getExpansionFiles().get(0);
            if (expansionFile.isChunkedUpload()) {
                listener.getLogger().println("Uploading expansion file using chunked upload...");
                expansionFileParts = uploadChunkedFile(
                    request.getWorkspace().child(request.getExpansionFilePath()),
                    uploadRequestResponse.getPendingUploadId(),
                    expansionFile,
                    true
                );
            } else {
                listener.getLogger().println("Uploading expansion file using direct upload...");
                uploadDirectFile(
                    request.getWorkspace().child(request.getExpansionFilePath()),
                    expansionFile.getPresignedData()
                );
            }
        }

        // Step 3: Verify upload
        listener.getLogger().println("Verifying upload...");
        return verifyUpload(uploadRequestResponse.getPendingUploadId(), primaryFileParts, expansionFileParts);
    }

    private UploadRequestResponse requestUploadUrls(BuildstashUploadRequest request) throws Exception {
        HttpPost httpPost = new HttpPost(UPLOAD_REQUEST_ENDPOINT);
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        httpPost.setHeader("Content-Type", "application/json");

        // Build request payload
        Map<String, Object> payload = request.toMap();
        String jsonPayload = objectMapper.writeValueAsString(payload);
        httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed to request upload URLs: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
            }

            return objectMapper.readValue(responseBody, UploadRequestResponse.class);
        }
    }

    private List<MultipartChunk> uploadChunkedFile(FilePath filePath, String pendingUploadId, FileUploadInfo fileInfo, boolean isExpansion) throws Exception {
        String endpoint = isExpansion ? MULTIPART_EXPANSION_ENDPOINT : MULTIPART_REQUEST_ENDPOINT;
        long fileSize = filePath.length();
        int chunkSize = fileInfo.getChunkedPartSizeMb() * 1024 * 1024;
        int numberOfParts = fileInfo.getChunkedNumberParts();

        for (int i = 0; i < numberOfParts; i++) {
            int partNumber = i + 1;
            long chunkStart = i * chunkSize;
            long chunkEnd = Math.min((i + 1) * chunkSize - 1, fileSize - 1);
            long contentLength = chunkEnd - chunkStart + 1;

            listener.getLogger().println("Uploading chunked upload, part: " + partNumber + " of " + numberOfParts);

            // Request presigned URL for this part
            PresignedUrlResponse presignedResponse = requestPresignedUrl(endpoint, pendingUploadId, partNumber, contentLength);

            // Upload chunk via presigned URL
            uploadChunk(filePath, presignedResponse.getPartPresignedUrl(), chunkStart, chunkEnd, contentLength);
        }

        // For simplicity, we'll return null for parts array in this implementation
        // In a full implementation, you'd collect ETags from each upload
        return null;
    }

    private PresignedUrlResponse requestPresignedUrl(String endpoint, String pendingUploadId, int partNumber, long contentLength) throws Exception {
        HttpPost httpPost = new HttpPost(endpoint);
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        httpPost.setHeader("Content-Type", "application/json");

        Map<String, Object> payload = Map.of(
            "pending_upload_id", pendingUploadId,
            "part_number", partNumber,
            "content_length", contentLength
        );

        String jsonPayload = objectMapper.writeValueAsString(payload);
        httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed to get presigned URL: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
            }

            return objectMapper.readValue(responseBody, PresignedUrlResponse.class);
        }
    }

    private void uploadChunk(FilePath filePath, String presignedUrl, long start, long end, long contentLength) throws Exception {
        HttpPut httpPut = new HttpPut(presignedUrl);
        httpPut.setHeader("Content-Type", "application/octet-stream");
        httpPut.setHeader("Content-Length", String.valueOf(contentLength));

        // Create input stream for the chunk
        try (InputStream inputStream = filePath.read()) {
            // Skip to start position
            long skipped = inputStream.skip(start);
            if (skipped != start) {
                throw new IOException("Failed to skip to position " + start + ", only skipped " + skipped + " bytes");
            }
            
            // Create entity with limited input stream
            HttpEntity entity = new org.apache.http.entity.InputStreamEntity(inputStream, contentLength, ContentType.APPLICATION_OCTET_STREAM);
            httpPut.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Failed to upload chunk: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
                }
            }
        }
    }

    private void uploadDirectFile(FilePath filePath, PresignedData presignedData) throws Exception {
        HttpPut httpPut = new HttpPut(presignedData.getUrl());
        
        // Set headers from presigned data
        for (Map.Entry<String, String> header : presignedData.getHeaders().entrySet()) {
            httpPut.setHeader(header.getKey(), header.getValue());
        }

        // Create entity with file content
        try (InputStream inputStream = filePath.read()) {
            HttpEntity entity = new org.apache.http.entity.InputStreamEntity(inputStream, ContentType.APPLICATION_OCTET_STREAM);
            httpPut.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Failed to upload file: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
                }
            }
        }
    }

    private BuildstashUploadResponse verifyUpload(String pendingUploadId, List<MultipartChunk> primaryFileParts, List<MultipartChunk> expansionFileParts) throws Exception {
        HttpPost httpPost = new HttpPost(UPLOAD_VERIFY_ENDPOINT);
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        httpPost.setHeader("Content-Type", "application/json");

        // Build verify payload
        Map<String, Object> payload = Map.of("pending_upload_id", pendingUploadId);
        String jsonPayload = objectMapper.writeValueAsString(payload);
        httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed to verify upload: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
            }

            return objectMapper.readValue(responseBody, BuildstashUploadResponse.class);
        }
    }

    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
} 