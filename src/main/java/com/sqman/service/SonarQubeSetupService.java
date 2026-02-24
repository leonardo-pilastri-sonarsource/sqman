package com.sqman.service;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle initial SonarQube setup (password change, token generation)
 */
public class SonarQubeSetupService {

    private static final Logger logger = LoggerFactory.getLogger(SonarQubeSetupService.class);
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String NEW_ADMIN_PASSWORD = "SqmanPsw123!";
    private static final String TOKEN_NAME = "sqman-global-analysis-token";
    private static final int MAX_WAIT_SECONDS = 300; // 5 minutes max wait
    private static final int POLL_INTERVAL_SECONDS = 2;

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public SonarQubeSetupService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .authenticator((route, response) -> {
                // Basic auth for SonarQube API
                String credential = Credentials.basic(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
                return response.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
            })
            .build();
    }

    /**
     * Check if setup is needed (token file doesn't exist)
     */
    public boolean isSetupNeeded(Path instancePath) {
        Path tokenFile = instancePath.resolve("token");
        return !Files.exists(tokenFile);
    }

    /**
     * Wait for SonarQube to be fully operational
     */
    public boolean waitForSonarQubeReady() throws InterruptedException {
        System.out.println("Waiting for SonarQube to be fully operational...");

        int attempts = 0;
        int maxAttempts = MAX_WAIT_SECONDS / POLL_INTERVAL_SECONDS;

        while (attempts < maxAttempts) {
            try {
                Request request = new Request.Builder()
                    .url(baseUrl + "/api/system/status")
                    .get()
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        // Check if status is "UP"
                        if (body.contains("\"status\":\"UP\"")) {
                            System.out.println("✓ SonarQube is ready!");
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                // Connection refused or other error - SonarQube not ready yet
                logger.debug("SonarQube not ready yet: {}", e.getMessage());
            }

            // Show progress every 10 seconds
            if (attempts % 5 == 0 && attempts > 0) {
                System.out.println("  Still waiting... (" + (attempts * POLL_INTERVAL_SECONDS) + "s elapsed)");
            }

            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            attempts++;
        }

        System.err.println("✗ Timeout waiting for SonarQube to be ready");
        return false;
    }

    /**
     * Perform automatic setup: change password and generate token
     */
    public String performAutomaticSetup(Path instancePath) throws IOException {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  AUTOMATIC SETUP");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("SQMan will now perform automatic configuration:");
        System.out.println("  • Change admin password to: " + NEW_ADMIN_PASSWORD);
        System.out.println("  • Generate a global analysis token");
        System.out.println();
        System.out.println("Please wait...");
        System.out.println();

        // Step 1: Change admin password
        System.out.println("[1/2] Changing admin password...");
        boolean passwordChanged = changeAdminPassword();
        if (!passwordChanged) {
            throw new IOException("Failed to change admin password");
        }
        System.out.println("      ✓ Admin password changed successfully");
        System.out.println();

        // Step 2: Generate analysis token
        System.out.println("[2/2] Generating global analysis token...");
        String token = generateAnalysisToken();
        if (token == null || token.trim().isEmpty()) {
            throw new IOException("Failed to generate analysis token");
        }
        System.out.println("      ✓ Token generated successfully");
        System.out.println();

        // Step 3: Save token to instance folder
        Path tokenFile = instancePath.resolve("token");
        Files.writeString(tokenFile, token, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("Token saved to: {}", tokenFile);

        // Display summary
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  SETUP COMPLETE!");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Credentials:");
        System.out.println("  Username: admin");
        System.out.println("  Password: " + NEW_ADMIN_PASSWORD);
        System.out.println();
        System.out.println("Global Analysis Token:");
        System.out.println("  " + token);
        System.out.println();
        System.out.println("Token saved to: " + tokenFile);
        System.out.println();
        System.out.println("Use this token for analysis with:");
        System.out.println("  mvn sonar:sonar -Dsonar.token=" + token);
        System.out.println();

        return token;
    }

    /**
     * Change admin password from default to new password
     */
    private boolean changeAdminPassword() throws IOException {
        RequestBody formBody = new FormBody.Builder()
            .add("login", DEFAULT_ADMIN_USERNAME)
            .add("password", NEW_ADMIN_PASSWORD)
            .add("previousPassword", DEFAULT_ADMIN_PASSWORD)
            .build();

        String credential = Credentials.basic(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
        Request request = new Request.Builder()
            .url(baseUrl + "/api/users/change_password")
            .post(formBody)
            .header("Authorization", credential)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return true;
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                logger.error("Failed to change password. Status: {}, Body: {}", response.code(), errorBody);
                return false;
            }
        }
    }

    /**
     * Generate a global analysis token with no expiration
     */
    private String generateAnalysisToken() throws IOException {
        RequestBody formBody = new FormBody.Builder()
            .add("name", TOKEN_NAME)
            .add("type", "GLOBAL_ANALYSIS_TOKEN")
            // No expirationDate = token never expires
            .build();

        // Use new credentials after password change
        String credential = Credentials.basic(DEFAULT_ADMIN_USERNAME, NEW_ADMIN_PASSWORD);
        Request request = new Request.Builder()
            .url(baseUrl + "/api/user_tokens/generate")
            .post(formBody)
            .header("Authorization", credential)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                // Parse JSON response to extract token
                // Response format: {"login":"admin","name":"token-name","token":"squ_...","createdAt":"..."}
                String token = extractTokenFromJson(responseBody);
                if (token != null) {
                    return token;
                } else {
                    logger.error("Could not extract token from response: {}", responseBody);
                    return null;
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                logger.error("Failed to generate token. Status: {}, Body: {}", response.code(), errorBody);
                return null;
            }
        }
    }

    /**
     * Simple JSON token extraction (avoid adding Jackson dependency just for this)
     */
    private String extractTokenFromJson(String json) {
        // Look for "token":"..." pattern
        int tokenStart = json.indexOf("\"token\":\"");
        if (tokenStart == -1) {
            return null;
        }
        tokenStart += 9; // Length of "token":"

        int tokenEnd = json.indexOf("\"", tokenStart);
        if (tokenEnd == -1) {
            return null;
        }

        return json.substring(tokenStart, tokenEnd);
    }
}
