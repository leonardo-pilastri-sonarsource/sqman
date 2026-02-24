package com.sqman.service;

import com.sqman.model.Edition;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service to download and extract SonarQube distributions.
 */
public class DownloadService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private static final int BUFFER_SIZE = 8192;
    private static final String SQMAN_HOME = System.getProperty("user.home") + "/.sqman";

    private final OkHttpClient httpClient;

    public DownloadService() {
        this.httpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();
    }

    /**
     * Download and install a specific SonarQube version.
     *
     * @param version Version to download (e.g., "10.3.0.82913" or "26.2.0.119303")
     * @param edition Edition to download
     * @param targetDirectory Target directory for installation (if null, uses ~/.sqman)
     * @return Path to the extracted installation
     */
    public Path downloadAndExtract(String version, Edition edition, String targetDirectory) throws IOException {
        // Determine installation directory
        Path installDir = targetDirectory != null
            ? Paths.get(targetDirectory)
            : Paths.get(SQMAN_HOME);

        Files.createDirectories(installDir);

        // Build download URL
        String fileName = buildFileName(version, edition);
        String downloadUrl = edition.getBinariesUrl() + fileName;

        logger.info("Downloading {} from {}", fileName, downloadUrl);
        System.out.println("Downloading SonarQube " + edition.getDisplayName() + " " + version + "...");
        System.out.println("URL: " + downloadUrl);

        // Download file
        Path zipFile = installDir.resolve(fileName);
        downloadFile(downloadUrl, zipFile);

        // Extract and rename
        System.out.println("\nExtracting archive...");
        Path extractedPath = extractZip(zipFile, installDir, version);

        // Clean up ZIP file
        Files.deleteIfExists(zipFile);
        logger.info("Installation complete: {}", extractedPath);

        return extractedPath;
    }

    /**
     * Build the SonarQube ZIP file name based on version and edition.
     *
     * Community edition:
     *   - Old: sonar-3.x.zip
     *   - Modern: sonarqube-10.7.0.96327.zip or sonarqube-26.2.0.119303.zip
     *
     * Commercial editions (Developer, Enterprise, DataCenter):
     *   - Modern: sonarqube-developer-10.7.0.96327.zip
     *   - Year-based: sonarqube-developer-2026.2.0.119303.zip (note: 26.x becomes 2026.x)
     */
    private String buildFileName(String version, Edition edition) {
        int majorVersion = extractMajorVersion(version);

        // Build base prefix (sonar vs sonarqube)
        String prefix = majorVersion < 4 ? "sonar" : "sonarqube";

        // Add edition suffix for commercial editions
        if (edition.isCommercial()) {
            prefix = prefix + "-" + edition.getFilenameSuffix();
        }

        // Convert version format for commercial editions with year-based versioning
        String fileVersion = version;
        if (edition.isCommercial() && isYearBasedVersion(majorVersion)) {
            // Convert 26.2.0.119303 to 2026.2.0.119303
            fileVersion = convertToCommercialVersionFormat(version);
        }

        return prefix + "-" + fileVersion + ".zip";
    }

    /**
     * Extract major version number from version string.
     */
    private int extractMajorVersion(String version) {
        try {
            String majorStr = version.split("\\.")[0];
            return Integer.parseInt(majorStr);
        } catch (Exception e) {
            logger.warn("Could not parse major version from {}, assuming >= 4.0", version);
            return 4; // Default to modern naming
        }
    }

    /**
     * Check if this is a year-based version (24, 25, 26, etc.).
     * Year-based versions are typically in the 20-99 range.
     */
    private boolean isYearBasedVersion(int majorVersion) {
        return majorVersion >= 24 && majorVersion <= 99;
    }

    /**
     * Convert year-based version to commercial format.
     * Example: 26.2.0.119303 -> 2026.2.0.119303
     */
    private String convertToCommercialVersionFormat(String version) {
        String[] parts = version.split("\\.", 2); // Split at first dot
        if (parts.length == 2) {
            try {
                int majorVersion = Integer.parseInt(parts[0]);
                if (isYearBasedVersion(majorVersion)) {
                    // Add "20" prefix to make it a full year
                    return "20" + version;
                }
            } catch (NumberFormatException e) {
                logger.warn("Could not parse major version from {}", version);
            }
        }
        return version; // Return unchanged if can't parse
    }

    /**
     * Download file with progress indication.
     */
    private void downloadFile(String url, Path destination) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: HTTP " + response.code() + " - " + response.message());
            }

            long contentLength = response.body().contentLength();

            try (InputStream input = response.body().byteStream();
                 FileOutputStream output = new FileOutputStream(destination.toFile())) {

                byte[] buffer = new byte[BUFFER_SIZE];
                long totalBytesRead = 0;
                int bytesRead;
                int lastProgress = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Update progress
                    if (contentLength > 0) {
                        int progress = (int) ((totalBytesRead * 100) / contentLength);
                        if (progress != lastProgress && progress % 5 == 0) {
                            printProgress(progress, totalBytesRead, contentLength);
                            lastProgress = progress;
                        }
                    }
                }

                if (contentLength > 0) {
                    printProgress(100, contentLength, contentLength);
                }
                System.out.println(); // New line after progress
            }
        }
    }

    /**
     * Print download progress bar.
     */
    private void printProgress(int percentage, long downloaded, long total) {
        int barLength = 50;
        int filled = (percentage * barLength) / 100;

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "=" : " ");
        }
        bar.append("]");

        String downloadedMB = String.format("%.2f", downloaded / (1024.0 * 1024.0));
        String totalMB = String.format("%.2f", total / (1024.0 * 1024.0));

        System.out.print("\r" + bar + " " + percentage + "% (" + downloadedMB + " / " + totalMB + " MB)");
    }

    /**
     * Extract ZIP file and rename to version identifier.
     */
    private Path extractZip(Path zipFile, Path targetDir, String version) throws IOException {
        String rootFolderName = null;

        // Extract ZIP
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                // Get root folder name from first entry
                if (rootFolderName == null) {
                    String entryName = entry.getName();
                    int firstSlash = entryName.indexOf('/');
                    if (firstSlash > 0) {
                        rootFolderName = entryName.substring(0, firstSlash);
                    }
                }

                Path entryPath = targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // Create parent directories if needed
                    Files.createDirectories(entryPath.getParent());

                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(entryPath.toFile())) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        // Rename extracted folder to version identifier
        if (rootFolderName == null) {
            throw new IOException("Could not determine root folder from ZIP archive");
        }

        Path extractedFolder = targetDir.resolve(rootFolderName);
        Path versionFolder = targetDir.resolve("sonarqube-" + version);

        // Check if extracted folder exists
        if (!Files.exists(extractedFolder)) {
            throw new IOException("Extracted folder not found: " + extractedFolder);
        }

        // If already extracted with the correct name, just return it
        if (extractedFolder.equals(versionFolder)) {
            System.out.println("Extracted to: " + versionFolder);
            return versionFolder;
        }

        // If target folder already exists, remove it first
        if (Files.exists(versionFolder)) {
            logger.warn("Version folder already exists, will be replaced: {}", versionFolder);
            deleteDirectory(versionFolder.toFile());
        }

        // Rename to version folder
        Files.move(extractedFolder, versionFolder);
        System.out.println("Extracted to: " + versionFolder);
        return versionFolder;
    }

    /**
     * Recursively delete a directory.
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete: " + directory);
        }
    }

    /**
     * Get the default SQMan home directory.
     */
    public static String getSqmanHome() {
        return SQMAN_HOME;
    }
}