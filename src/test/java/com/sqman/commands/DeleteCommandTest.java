package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCommandTest {

    @Mock
    private InstanceService instanceService;

    @Mock
    private ProcessService processService;

    @Test
    void testVersionNotFound() {
        when(instanceService.isInstalled("99.99.99")).thenReturn(false);
        when(instanceService.getInstalledByVersion("99.99.99")).thenReturn(List.of());

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("99.99.99", "-y");

        assertEquals(1, exitCode);
    }

    @Test
    void testRunningInstanceNoForce() {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913", "-y");

        assertEquals(1, exitCode);
        verify(instanceService, never()).deleteInstance(anyString());
    }

    @Test
    void testDeleteSuccessWithSkipConfirmation() {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);
        when(instanceService.getInstancePath("10.3.0.82913"))
            .thenReturn(java.nio.file.Paths.get("/tmp/sonarqube-10.3.0.82913"));
        when(instanceService.deleteInstance("10.3.0.82913")).thenReturn(true);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913", "-y");

        assertEquals(0, exitCode);
        verify(instanceService).deleteInstance("10.3.0.82913");
    }

    @Test
    void testDeleteFails() {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);
        when(instanceService.getInstancePath("10.3.0.82913"))
            .thenReturn(java.nio.file.Paths.get("/tmp/sonarqube-10.3.0.82913"));
        when(instanceService.deleteInstance("10.3.0.82913")).thenReturn(false);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913", "-y");

        assertEquals(1, exitCode);
    }

    @Test
    void testForceDeleteRunningInstance() throws Exception {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);
        when(instanceService.getInstancePath("10.3.0.82913"))
            .thenReturn(java.nio.file.Paths.get("/tmp/sonarqube-10.3.0.82913"));
        when(processService.stopInstance("10.3.0.82913", true)).thenReturn(true);
        when(instanceService.deleteInstance("10.3.0.82913")).thenReturn(true);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913", "--force", "-y");

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", true);
        verify(instanceService).deleteInstance("10.3.0.82913");
    }

    @Test
    void testIndexBasedDelete() {
        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);
        when(instanceService.getInstancePath("10.3.0.82913"))
            .thenReturn(java.nio.file.Paths.get("/tmp/sonarqube-10.3.0.82913"));
        when(instanceService.deleteInstance("10.3.0.82913")).thenReturn(true);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("1", "-y");

        assertEquals(0, exitCode);
        verify(instanceService).deleteInstance("10.3.0.82913");
    }

    @Test
    void testPartialVersionDelete() {
        when(instanceService.isInstalled("10.3")).thenReturn(false);
        when(instanceService.getInstalledByVersion("10.3")).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);
        when(instanceService.getInstancePath("10.3.0.82913"))
            .thenReturn(java.nio.file.Paths.get("/tmp/sonarqube-10.3.0.82913"));
        when(instanceService.deleteInstance("10.3.0.82913")).thenReturn(true);

        DeleteCommand cmd = new DeleteCommand(instanceService, processService);
        int exitCode = new CommandLine(cmd).execute("10.3", "-y");

        assertEquals(0, exitCode);
    }
}
