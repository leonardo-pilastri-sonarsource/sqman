package com.sqman.commands;

import com.sqman.service.InstanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallPluginCommandTest {

    @Mock
    private InstanceService instanceService;

    @Test
    void testPluginFileNotFound() {
        InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute("/nonexistent/plugin.jar");

        assertEquals(1, exitCode);
    }

    @Test
    void testPluginNotJar(@TempDir Path tempDir) throws IOException {
        Path notJar = tempDir.resolve("plugin.txt");
        Files.writeString(notJar, "not a jar");

        InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute(notJar.toString());

        assertEquals(1, exitCode);
    }

    @Test
    void testPluginIsDirectory(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("plugin.jar");
        Files.createDirectories(dir);

        InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute(dir.toString());

        assertEquals(1, exitCode);
    }

    @Test
    void testNoInstancesInstalled(@TempDir Path tempDir) throws IOException {
        Path jarFile = tempDir.resolve("sonar-java-plugin-8.22.0.jar");
        Files.writeString(jarFile, "fake jar");

        when(instanceService.listInstalled()).thenReturn(Collections.emptyList());

        InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute(jarFile.toString());

        assertEquals(1, exitCode);
    }

    @Test
    void testInstallNewPlugin(@TempDir Path tempDir) throws IOException {
        Path jarFile = tempDir.resolve("sonar-java-plugin-8.22.0.41895.jar");
        Files.writeString(jarFile, "fake jar content");

        Path instanceDir = tempDir.resolve("instance");
        Path extensionsDir = instanceDir.resolve("lib/extensions");
        Files.createDirectories(extensionsDir);

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(instanceDir);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        try {
            InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute(jarFile.toString());

            assertEquals(0, exitCode);
            assertTrue(Files.exists(extensionsDir.resolve("sonar-java-plugin-8.22.0.41895.jar")));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testInstallOverExistingPlugin(@TempDir Path tempDir) throws IOException {
        Path jarFile = tempDir.resolve("sonar-java-plugin-9.0.0.jar");
        Files.writeString(jarFile, "new version");

        Path instanceDir = tempDir.resolve("instance");
        Path extensionsDir = instanceDir.resolve("lib/extensions");
        Files.createDirectories(extensionsDir);
        Path existingPlugin = extensionsDir.resolve("sonar-java-plugin-8.22.0.41895.jar");
        Files.writeString(existingPlugin, "old version");

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(instanceDir);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        try {
            InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute(jarFile.toString());

            assertEquals(0, exitCode);
            assertTrue(Files.exists(extensionsDir.resolve("sonar-java-plugin-9.0.0.jar")));
            assertFalse(Files.exists(existingPlugin));
            assertTrue(Files.exists(instanceDir.resolve("original-plugins")));
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testInstallShowsRestartHintWhenRunning(@TempDir Path tempDir) throws IOException {
        Path jarFile = tempDir.resolve("sonar-java-plugin-8.22.0.jar");
        Files.writeString(jarFile, "fake jar content");

        Path instanceDir = tempDir.resolve("instance");
        Path extensionsDir = instanceDir.resolve("lib/extensions");
        Files.createDirectories(extensionsDir);

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(instanceDir);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        try {
            InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute(jarFile.toString());

            assertEquals(0, exitCode);
            verify(instanceService).isInstanceRunning("10.3.0.82913");
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testUserCancelsSelection(@TempDir Path tempDir) throws IOException {
        Path jarFile = tempDir.resolve("sonar-java-plugin-8.22.0.jar");
        Files.writeString(jarFile, "fake jar content");

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("0\n".getBytes()));
        try {
            InstallPluginCommand cmd = new InstallPluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute(jarFile.toString());

            assertEquals(1, exitCode);
        } finally {
            System.setIn(originalIn);
        }
    }
}
