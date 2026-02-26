package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunCommandTest {

    @Mock
    private ProcessService processService;

    @Mock
    private InstanceService instanceService;

    @TempDir
    Path tempDir;

    @Test
    void testAlreadyRunning() {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.4.0.100000");

        assertEquals(1, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testVersionNotFound() {
        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.isInstalled("99.99.99")).thenReturn(false);
        when(instanceService.getInstalledByVersion("99.99.99")).thenReturn(List.of());

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("99.99.99");

        assertEquals(1, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testStartSuccess() throws Exception {
        // Create token file so setup is skipped
        Files.writeString(tempDir.resolve("token"), "squ_test_token");

        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(true);
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(tempDir);

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913");

        assertEquals(0, exitCode);
        verify(processService).startInstance("10.3.0.82913", 0, true);
    }

    @Test
    void testStartFails() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(false);

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913");

        assertEquals(1, exitCode);
    }

    @Test
    void testIndexBasedStart() throws Exception {
        Files.writeString(tempDir.resolve("token"), "squ_test_token");

        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(true);
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(tempDir);

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("1");

        assertEquals(0, exitCode);
        verify(processService).startInstance("10.3.0.82913", 0, true);
    }

    @Test
    void testPartialVersionStart() throws Exception {
        Files.writeString(tempDir.resolve("token"), "squ_test_token");

        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.isInstalled("10.3")).thenReturn(false);
        when(instanceService.getInstalledByVersion("10.3")).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(true);
        when(instanceService.getInstancePath("10.3.0.82913")).thenReturn(tempDir);

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3");

        assertEquals(0, exitCode);
    }

    @Test
    void testStartThrowsException() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn(null);
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(processService.startInstance("10.3.0.82913", 0, true))
            .thenThrow(new IOException("start failed"));

        RunCommand cmd = new RunCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913");

        assertEquals(1, exitCode);
    }
}
