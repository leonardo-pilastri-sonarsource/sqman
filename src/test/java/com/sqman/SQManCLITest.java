package com.sqman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class SQManCLITest {

    @Test
    void testCLICreation() {
        SQManCLI cli = new SQManCLI();
        assertNotNull(cli);
    }

    @Test
    void testHelpOption() {
        SQManCLI cli = new SQManCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void testVersionOption() {
        SQManCLI cli = new SQManCLI();
        CommandLine cmd = new CommandLine(cli);
        int exitCode = cmd.execute("--version");
        assertEquals(0, exitCode);
    }
}
