package com.sqman;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import com.sqman.commands.*;

/**
 * Main entry point for SQMan CLI
 */
@Command(
    name = "sqman",
    description = "CLI utility tool for managing SonarQube instances locally",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        VersionsCommand.class,
        DownloadCommand.class,
        RunCommand.class,
        StopCommand.class,
        ListCommand.class,
        DeleteCommand.class,
        ConfigCommand.class,
        InstallPluginCommand.class,
        RestorePluginCommand.class
    }
)
public class SQManCLI implements Runnable {

    @Override
    public void run() {
        // Show help when no subcommand is provided
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SQManCLI()).execute(args);
        System.exit(exitCode);
    }
}
