package com.sqman.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SonarQubeSetupServiceTest {

    @Test
    void testIsSetupNeededWhenNoTokenFile(@TempDir Path tempDir) {
        SonarQubeSetupService service = new SonarQubeSetupService("http://localhost:9000");
        assertTrue(service.isSetupNeeded(tempDir));
    }

    @Test
    void testIsSetupNotNeededWhenTokenExists(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("token"), "squ_test_token_12345");
        SonarQubeSetupService service = new SonarQubeSetupService("http://localhost:9000");
        assertFalse(service.isSetupNeeded(tempDir));
    }
}
