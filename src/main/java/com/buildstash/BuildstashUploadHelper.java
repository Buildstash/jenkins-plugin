package com.buildstash;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class containing shared logic for Buildstash upload operations.
 * Used by both BuildstashBuilder and BuildstashStepExecution to avoid code duplication.
 */
public class BuildstashUploadHelper {

    /**
     * Expands environment variables in a string value.
     * Returns null if input is null, otherwise expands variables like ${VAR_NAME}.
     */
    public static String expand(EnvVars env, String value) {
        if (value == null || env == null) {
            return value;
        }
        return env.expand(value);
    }

    /**
     * Validates required parameters for Buildstash upload.
     * @throws IllegalArgumentException if any required parameter is null or blank
     */
    public static void validateParameters(String apiKey, String primaryFilePath, String versionComponent1Major,
                                         String versionComponent2Minor, String versionComponent3Patch,
                                         String platform, String stream) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required");
        }

        if (primaryFilePath == null || primaryFilePath.isBlank()) {
            throw new IllegalArgumentException("Primary file path is required");
        }

        if (versionComponent1Major == null || versionComponent1Major.isBlank()) {
            throw new IllegalArgumentException("Major version component is required");
        }

        if (versionComponent2Minor == null || versionComponent2Minor.isBlank()) {
            throw new IllegalArgumentException("Minor version component is required");
        }

        if (versionComponent3Patch == null || versionComponent3Patch.isBlank()) {
            throw new IllegalArgumentException("Patch version component is required");
        }

        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform is required");
        }

        if (stream == null || stream.isBlank()) {
            throw new IllegalArgumentException("Stream is required");
        }
    }

    /**
     * Creates a BuildstashUploadRequest from the provided parameters.
     * All string parameters should already be expanded (environment variables resolved).
     */
    public static BuildstashUploadRequest createUploadRequest(FilePath workspace, Run<?, ?> build,
                                                             String structure, String primaryFilePath, String expansionFilePath,
                                                             String versionComponent1Major, String versionComponent2Minor,
                                                             String versionComponent3Patch, String versionComponentExtra,
                                                             String versionComponentMeta, String customBuildNumber,
                                                             String labels, String architectures, String platform,
                                                             String stream, String notes, String vcHostType, String vcHost,
                                                             String vcRepoName, String vcRepoUrl, String vcBranch,
                                                             String vcCommitSha, String vcCommitUrl) throws IOException, InterruptedException {
        BuildstashUploadRequest request = new BuildstashUploadRequest();

        // Set basic properties (already expanded)
        request.setStructure(structure);
        request.setPrimaryFilePath(primaryFilePath);
        request.setExpansionFilePath(expansionFilePath);
        request.setVersionComponent1Major(versionComponent1Major);
        request.setVersionComponent2Minor(versionComponent2Minor);
        request.setVersionComponent3Patch(versionComponent3Patch);
        request.setVersionComponentExtra(versionComponentExtra);
        request.setVersionComponentMeta(versionComponentMeta);
        request.setCustomBuildNumber(customBuildNumber);
        request.setPlatform(platform);
        request.setStream(stream);
        request.setNotes(notes);

        // Parse labels and architectures (comma-separated or newline-separated)
        if (labels != null && !labels.isBlank()) {
            List<String> labelsList = Arrays.stream(labels.split("[,\\r\\n]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            request.setLabels(labelsList);
        }

        if (architectures != null && !architectures.isBlank()) {
            List<String> architecturesList = Arrays.stream(architectures.split("[,\\r\\n]+"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            request.setArchitectures(architecturesList);
        }

        // Set CI information automatically from Jenkins context
        request.setCiPipeline(build.getParent().getDisplayName());
        request.setCiRunId(String.valueOf(build.getNumber()));
        request.setCiRunUrl(getBuildUrl(build));
        request.setCiPipelineUrl(getProjectUrl(build));
        request.setCiBuildDuration(formatBuildDuration(getBuildDuration(build)));
        request.setSource("jenkins");

        // Set version control information (manual values first, already expanded)
        request.setVcHostType(vcHostType);
        request.setVcHost(vcHost);
        request.setVcRepoName(vcRepoName);
        request.setVcRepoUrl(vcRepoUrl);
        request.setVcBranch(vcBranch);
        request.setVcCommitSha(vcCommitSha);
        request.setVcCommitUrl(vcCommitUrl);

        // Auto-detect and populate any missing VC fields from Jenkins
        VersionControlDetector.populateVersionControlInfo(build, request);

        // Set workspace for file operations
        request.setWorkspace(workspace);

        return request;
    }

    /**
     * Gets the full URL to the build run status summary.
     */
    public static String getBuildUrl(Run<?, ?> build) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            String rootUrl = jenkins.getRootUrl();
            if (rootUrl != null && !rootUrl.isEmpty()) {
                // Remove trailing slash from root URL if present
                String baseUrl = rootUrl.endsWith("/") ? rootUrl.substring(0, rootUrl.length() - 1) : rootUrl;
                String buildPath = build.getUrl();
                // Ensure build path starts with / if it doesn't already
                if (!buildPath.startsWith("/")) {
                    buildPath = "/" + buildPath;
                }
                return baseUrl + buildPath;
            }
        }
        // Fallback to relative URL if root URL is not available
        return build.getUrl();
    }

    /**
     * Gets the full URL to the Jenkins project/job root.
     */
    public static String getProjectUrl(Run<?, ?> build) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            String rootUrl = jenkins.getRootUrl();
            if (rootUrl != null && !rootUrl.isEmpty()) {
                // Remove trailing slash from root URL if present
                String baseUrl = rootUrl.endsWith("/") ? rootUrl.substring(0, rootUrl.length() - 1) : rootUrl;
                String projectPath = build.getParent().getUrl();
                // Ensure project path starts with / if it doesn't already
                if (!projectPath.startsWith("/")) {
                    projectPath = "/" + projectPath;
                }
                return baseUrl + projectPath;
            }
        }
        // Fallback to relative URL if root URL is not available
        return build.getParent().getUrl();
    }

    /**
     * Gets the build duration in milliseconds.
     * If the build is still running or duration is 0, calculates duration from start time to now.
     * If the build is completed with a valid duration, returns that duration.
     */
    public static long getBuildDuration(Run<?, ?> build) {
        long duration = build.getDuration();
        // If duration is 0 (build still running or not set), calculate from start time
        if (duration == 0) {
            long startTime = build.getStartTimeInMillis();
            long currentTime = System.currentTimeMillis();
            duration = currentTime - startTime;
        }
        return duration;
    }

    /**
     * Formats build duration in milliseconds to HH:mm:ss format.
     */
    public static String formatBuildDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Logs the upload results to the task listener.
     */
    public static void logResults(TaskListener listener, BuildstashUploadResponse response) {
        listener.getLogger().println("Buildstash upload completed successfully!");
        listener.getLogger().println("Build ID: " + response.getBuildId());
        listener.getLogger().println("Build Info URL: " + response.getBuildInfoUrl());
        listener.getLogger().println("Download URL: " + response.getDownloadUrl());
        listener.getLogger().println("Pending Processing: " + response.isPendingProcessing());
    }
}

