package com.sqman.service;

import com.sqman.model.Edition;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sqman.model.SonarQubeVersion;

/**
 * Service to fetch available SonarQube versions from SonarSource binaries website.
 * Fetches version information by querying the S3 API endpoint for each edition.
 * Community edition versions are classified as SQCB (Community Build).
 * Commercial edition versions are classified as SQS (Server).
 */
public class SonarQubeVersionService {

    private static final Logger logger = LoggerFactory.getLogger(SonarQubeVersionService.class);
    private static final String S3_API_BASE_URL = "https://binaries.sonarsource.com/s3api";

    private final OkHttpClient httpClient;

    // Pattern to extract version from ZIP filenames
    // Community: sonarqube-10.7.0.96327.zip
    // Commercial (old format): sonarqube-developer-10.7.0.96327.zip
    // Commercial (year format): sonarqube-developer-2026.2.0.119303.zip
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "sonarqube(?:-[a-z]+)?-(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.zip"
    );

    public SonarQubeVersionService() {
        this.httpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();
    }

    /**
     * Fetch available SonarQube versions from SonarSource binaries website.
     * Returns versions by querying the S3 API for the specified edition.
     * Community edition versions are classified as SQCB (Community Build).
     * Commercial editions are classified as SQS (Server).
     *
     * @param edition The edition to fetch versions for
     * @return List of SonarQubeVersion objects sorted in descending order (newest first)
     * @throws IOException if the API request fails
     */
    public List<SonarQubeVersion> fetchAvailableVersions(Edition edition) throws IOException {
        logger.info("Fetching SonarQube {} versions from binaries website", edition.getDisplayName());

        // Build S3 API URL for this edition
        String s3Prefix = getS3PrefixForEdition(edition);
        String url = S3_API_BASE_URL + "?delimiter=/&prefix=" + urlEncode(s3Prefix);

        logger.debug("Querying S3 API: {}", url);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch versions from binaries website: HTTP " + response.code());
            }

            String xmlBody = response.body().string();
            List<SonarQubeVersion> versions = parseVersionsFromXml(xmlBody, edition);

            // Sort all versions in descending order (newest first)
            versions.sort((v1, v2) -> v2.compareTo(v1));

            logger.info("Found {} versions for {} edition",
                    versions.size(),
                    edition.getDisplayName());

            return versions;
        }
    }

    /**
     * Get the S3 prefix path for an edition.
     * Examples:
     * - Community: Distribution/sonarqube/
     * - Developer: CommercialDistribution/sonarqube-developer/
     */
    private String getS3PrefixForEdition(Edition edition) {
        String binariesUrl = edition.getBinariesUrl();
        // Extract the path after binaries.sonarsource.com/
        String prefix = binariesUrl.replace("https://binaries.sonarsource.com/", "");
        return prefix;
    }

    /**
     * URL encode a string for use in query parameters.
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse versions from S3 API XML response.
     * Extracts version numbers from ZIP filenames listed in the XML.
     * Supports both old versioning (10.7.0.96327) and year-based versioning (2026.2.0.119303).
     *
     * Example XML structure:
     * <ListBucketResult>
     *   <Contents>
     *     <Key>Distribution/sonarqube/sonarqube-10.7.0.96327.zip</Key>
     *   </Contents>
     * </ListBucketResult>
     */
    private List<SonarQubeVersion> parseVersionsFromXml(String xmlResponse, Edition edition) {
        List<SonarQubeVersion> versions = new ArrayList<>();

        // Determine the type based on edition
        // Community edition = SQCB (Community Build)
        // Commercial editions = SQS (Server)
        SonarQubeVersion.Type type = edition == Edition.COMMUNITY
            ? SonarQubeVersion.Type.SQCB
            : SonarQubeVersion.Type.SQS;

        // Extract all <Key>...</Key> entries from the XML
        Pattern keyPattern = Pattern.compile("<Key>([^<]+)</Key>");
        Matcher keyMatcher = keyPattern.matcher(xmlResponse);

        while (keyMatcher.find()) {
            String key = keyMatcher.group(1);

            // Only process ZIP files
            if (!key.endsWith(".zip")) {
                continue;
            }

            // Extract the filename from the full path
            String filename = key.substring(key.lastIndexOf('/') + 1);

            // Extract version from filename
            Matcher versionMatcher = VERSION_PATTERN.matcher(filename);
            if (versionMatcher.find()) {
                String version = versionMatcher.group(1);

                // For commercial editions with year-based versions, convert back to standard format
                // Example: 2026.2.0.119303 -> 26.2.0.119303
                if (edition.isCommercial()) {
                    version = convertFromCommercialVersionFormat(version);
                }

                versions.add(new SonarQubeVersion(version, type));
                logger.debug("Found version: {} ({})", version, type.getCode());
            }
        }

        return versions;
    }

    /**
     * Convert commercial year-based version format back to standard format.
     * Example: 2026.2.0.119303 -> 26.2.0.119303
     * Non-year versions (10.x, etc.) are returned unchanged.
     */
    private String convertFromCommercialVersionFormat(String version) {
        String[] parts = version.split("\\.", 2);
        if (parts.length == 2) {
            try {
                int firstPart = Integer.parseInt(parts[0]);
                // If it starts with 20XX (year format), strip the "20" prefix
                if (firstPart >= 2024 && firstPart <= 2099) {
                    return (firstPart - 2000) + "." + parts[1];
                }
            } catch (NumberFormatException e) {
                logger.debug("Could not parse version number from {}", version);
            }
        }
        return version;
    }
}
