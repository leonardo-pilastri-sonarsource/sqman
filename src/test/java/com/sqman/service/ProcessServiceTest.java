package com.sqman.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProcessServiceTest {

    @Test
    void testDetectPlatform() {
        ProcessService service = new ProcessService();
        ProcessService.Platform platform = service.detectPlatform();
        assertNotNull(platform);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            assertEquals(ProcessService.Platform.MACOS, platform);
        } else if (os.contains("win")) {
            assertEquals(ProcessService.Platform.WINDOWS, platform);
        } else {
            assertEquals(ProcessService.Platform.LINUX, platform);
        }
    }

    @Test
    void testPlatformProperties() {
        assertEquals("windows-x86-64", ProcessService.Platform.WINDOWS.getBinDir());
        assertEquals("StartSonar.bat", ProcessService.Platform.WINDOWS.getScriptName());
        assertEquals("linux-x86-64", ProcessService.Platform.LINUX.getBinDir());
        assertEquals("sonar.sh", ProcessService.Platform.LINUX.getScriptName());
        assertEquals("macosx-universal-64", ProcessService.Platform.MACOS.getBinDir());
        assertEquals("sonar.sh", ProcessService.Platform.MACOS.getScriptName());
    }

    @Test
    void testIsInstanceRunningNoPidFile(@TempDir Path tempDir) {
        ProcessService service = new ProcessService();
        assertFalse(service.isInstanceRunning(tempDir));
    }

    @Test
    void testIsInstanceRunningInvalidPidFile(@TempDir Path tempDir) throws IOException {
        ProcessService service = new ProcessService();
        ProcessService.Platform platform = service.detectPlatform();

        Path pidDir = tempDir.resolve("bin").resolve(platform.getBinDir());
        Files.createDirectories(pidDir);
        Files.writeString(pidDir.resolve("SonarQube.pid"), "");

        assertFalse(service.isInstanceRunning(tempDir));
    }

    @Test
    void testIsInstanceRunningWithStalePid(@TempDir Path tempDir) throws IOException {
        ProcessService service = new ProcessService();
        ProcessService.Platform platform = service.detectPlatform();

        Path pidDir = tempDir.resolve("bin").resolve(platform.getBinDir());
        Files.createDirectories(pidDir);
        // Use a PID that's very unlikely to be running
        Files.writeString(pidDir.resolve("SonarQube.pid"), "999999999");

        assertFalse(service.isInstanceRunning(tempDir));
    }

    @Test
    void testStartInstanceNotFound() {
        ProcessService service = new ProcessService();
        assertThrows(IOException.class, () ->
            service.startInstance("nonexistent-version-99.99.99", 0, true));
    }

    @Test
    void testStopInstanceNotFound() {
        ProcessService service = new ProcessService();
        assertThrows(IOException.class, () ->
            service.stopInstance("nonexistent-version-99.99.99", false));
    }
}
