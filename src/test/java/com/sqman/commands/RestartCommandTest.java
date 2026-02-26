package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestartCommandTest {

    @Mock
    private ProcessService processService;

    @Mock
    private InstanceService instanceService;

    @Test
    void testNoRunningInstance() {
        when(instanceService.getRunningInstance()).thenReturn(null);

        RestartCommand cmd = new RestartCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
        verifyNoInteractions(processService);
    }

    @Test
    void testRestartSuccess() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(true);

        RestartCommand cmd = new RestartCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(0, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
        verify(processService).startInstance("10.3.0.82913", 0, true);
    }

    @Test
    void testStopFails() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(false);

        RestartCommand cmd = new RestartCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
        verify(processService).stopInstance("10.3.0.82913", false);
        verify(processService, never()).startInstance(anyString(), anyInt(), anyBoolean());
    }

    @Test
    void testStartFails() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false)).thenReturn(true);
        when(processService.startInstance("10.3.0.82913", 0, true)).thenReturn(false);

        RestartCommand cmd = new RestartCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
    }

    @Test
    void testStopThrowsException() throws Exception {
        when(instanceService.getRunningInstance()).thenReturn("10.3.0.82913");
        when(processService.stopInstance("10.3.0.82913", false))
            .thenThrow(new java.io.IOException("stop failed"));

        RestartCommand cmd = new RestartCommand(processService, instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(1, exitCode);
    }
}
