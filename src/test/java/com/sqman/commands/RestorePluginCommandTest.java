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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestorePluginCommandTest {

    @Mock
    private InstanceService instanceService;

    @Test
    void testNoInstancesInstalled() {
        when(instanceService.listInstalled()).thenReturn(Collections.emptyList());

        RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
    }

    @Test
    void testNoBackupDirectory(@TempDir Path tempDir) {
        Path instanceDir = tempDir.resolve("instance");

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(instanceDir);

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        try {
            RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute();

            assertEquals(0, exitCode);
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testNoBackedUpPlugins(@TempDir Path tempDir) throws IOException {
        Path instanceDir = tempDir.resolve("instance");
        Path backupDir = instanceDir.resolve("original-plugins");
        Files.createDirectories(backupDir);

        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(instanceDir);

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));
        try {
            RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute();

            assertEquals(0, exitCode);
        } finally {
            System.setIn(originalIn);
        }
    }

    @Test
    void testRestorePluginSuccess(@TempDir Path tempDir) throws Exception {
        Path instanceDir = tempDir.resolve("instance");

        // Create extensions directory with current plugin
        Path extensionsDir = instanceDir.resolve("lib/extensions");
        Files.createDirectories(extensionsDir);
        Path currentPlugin = extensionsDir.resolve("sonar-java-plugin-9.0.0.jar");
        Files.writeString(currentPlugin, "new version");

        // Create backup directory with backed-up plugin
        Path backupDir = instanceDir.resolve("original-plugins");
        Files.createDirectories(backupDir);
        Path backupPlugin = backupDir.resolve("sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar");
        Files.writeString(backupPlugin, "original version");

        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);

        // Test the restorePlugin method directly via reflection to bypass interactive prompts
        RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
        Method restorePlugin = RestorePluginCommand.class.getDeclaredMethod(
            "restorePlugin", Path.class, Path.class, String.class);
        restorePlugin.setAccessible(true);
        int result = (int) restorePlugin.invoke(cmd, instanceDir, backupPlugin, "10.3.0.82913");

        assertEquals(0, result);
        // Original plugin restored
        assertTrue(Files.exists(extensionsDir.resolve("sonar-java-plugin-8.22.0.41895.jar")));
        // Current plugin removed
        assertFalse(Files.exists(currentPlugin));
        // Backup file deleted
        assertFalse(Files.exists(backupPlugin));
    }

    @Test
    void testRestorePluginWhenInstanceRunning(@TempDir Path tempDir) throws Exception {
        Path instanceDir = tempDir.resolve("instance");

        Path extensionsDir = instanceDir.resolve("lib/extensions");
        Files.createDirectories(extensionsDir);

        Path backupDir = instanceDir.resolve("original-plugins");
        Files.createDirectories(backupDir);
        Path backupPlugin = backupDir.resolve("sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar");
        Files.writeString(backupPlugin, "original version");

        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);

        RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
        Method restorePlugin = RestorePluginCommand.class.getDeclaredMethod(
            "restorePlugin", Path.class, Path.class, String.class);
        restorePlugin.setAccessible(true);
        int result = (int) restorePlugin.invoke(cmd, instanceDir, backupPlugin, "10.3.0.82913");

        assertEquals(0, result);
        // Plugin restored
        assertTrue(Files.exists(extensionsDir.resolve("sonar-java-plugin-8.22.0.41895.jar")));
        // Instance running status was checked (for restart hint)
        verify(instanceService).isInstanceRunning("10.3.0.82913");
    }

    @Test
    void testRestorePluginNoExtensionsDir(@TempDir Path tempDir) throws Exception {
        Path instanceDir = tempDir.resolve("instance");
        Files.createDirectories(instanceDir);

        Path backupDir = instanceDir.resolve("original-plugins");
        Files.createDirectories(backupDir);
        Path backupPlugin = backupDir.resolve("sonar-java-plugin-8.22.0-backup-20260219-153045.jar");
        Files.writeString(backupPlugin, "original version");

        RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
        Method restorePlugin = RestorePluginCommand.class.getDeclaredMethod(
            "restorePlugin", Path.class, Path.class, String.class);
        restorePlugin.setAccessible(true);
        int result = (int) restorePlugin.invoke(cmd, instanceDir, backupPlugin, "10.3.0.82913");

        assertEquals(1, result);
    }

    @Test
    void testUserCancelsInstanceSelection() {
        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));

        InputStream originalIn = System.in;
        System.setIn(new ByteArrayInputStream("0\n".getBytes()));
        try {
            RestorePluginCommand cmd = new RestorePluginCommand(instanceService);
            int exitCode = new CommandLine(cmd).execute();

            assertEquals(1, exitCode);
        } finally {
            System.setIn(originalIn);
        }
    }
}
