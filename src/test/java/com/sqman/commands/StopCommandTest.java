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
class StopCommandTest {

    @Mock
    private ProcessService processService;

    @Mock
    private InstanceService instanceService;

    @Test
    void testNoVersionNoRunningInstance() {
        when(instanceService.getRunningInstance()).thenReturn(null);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(0, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testNoVersionAutoDetectStopSuccess() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
    }

    @Test
    void testNoVersionAutoDetectStopFails() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(false);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
    }

    @Test
    void testExactVersionStopSuccess() throws Exception {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913");

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
    }

    @Test
    void testVersionNotFound() {
        when(instanceService.isInstalled("99.99.99")).thenReturn(false);
        when(instanceService.getInstalledByVersion("99.99.99")).thenReturn(List.of());

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("99.99.99");

        assertEquals(1, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testVersionNotRunning() {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(false);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913");

        assertEquals(1, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testForceStop() throws Exception {
        when(instanceService.isInstalled("10.3.0.82913")).thenReturn(true);
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);
        when(processService.stopInstance("10.3.0.82913", true)).thenReturn(true);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3.0.82913", "--force");

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", true);
    }

    @Test
    void testIndexBasedStop() throws Exception {
        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("1");

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
    }

    @Test
    void testIndexOutOfRange() {
        when(instanceService.listInstalled()).thenReturn(List.of("sonarqube-10.3.0.82913"));

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("5");

        assertEquals(1, exitCode);
    }

    @Test
    void testPartialVersionMatch() throws Exception {
        when(instanceService.isInstalled("10.3")).thenReturn(false);
        when(instanceService.getInstalledByVersion("10.3")).thenReturn(List.of("sonarqube-10.3.0.82913"));
        when(instanceService.isInstanceRunning("10.3.0.82913")).thenReturn(true);
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.3");

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
    }

    @Test
    void testMultiplePartialMatches() {
        when(instanceService.isInstalled("10.x")).thenReturn(false);
        when(instanceService.getInstalledByVersion("10.x")).thenReturn(
            List.of("sonarqube-10.3.0.82913", "sonarqube-10.4.0.100000"));

        StopCommand cmd = new StopCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute("10.x");

        assertEquals(1, exitCode);
    }
}
