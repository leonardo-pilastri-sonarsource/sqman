package com.sqman.commands;

import com.sqman.service.InstanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {

    @Mock
    private InstanceService instanceService;

    @Test
    void testNoInstances() {
        when(instanceService.listInstalled()).thenReturn(Collections.emptyList());

        ListCommand cmd = new ListCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(0, exitCode);
    }

    @Test
    void testWithInstances() {
        when(instanceService.listInstalled()).thenReturn(
            List.of("sonarqube-10.3.0.82913", "sonarqube-10.4.0.100000"));

        ListCommand cmd = new ListCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute();

        assertEquals(0, exitCode);
    }

    @Test
    void testWithRunningFlag() {
        when(instanceService.listInstalled()).thenReturn(
            List.of("sonarqube-10.3.0.82913"));

        ListCommand cmd = new ListCommand(instanceService);
        int exitCode = new CommandLine(cmd).execute("--running");

        assertEquals(0, exitCode);
    }
}
