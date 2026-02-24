package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to stop the running SonarQube instance
 */
@Command(
    name = "stop",
    description = "Stop the running SonarQube instance",
    mixinStandardHelpOptions = true
)
public class StopCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Version to stop (e.g., 10.3.0.82913, partial like 10.3, or index like 1). If omitted, stops the running instance.",
        arity = "0..1"
    )
    private String version;

    @Option(
        names = {"-f", "--force"},
        description = "Force kill the process (use if graceful shutdown fails)"
    )
    private boolean force;

    private final ProcessService processService;
    private final InstanceService instanceService;

    public StopCommand() {
        this.processService = new ProcessService();
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public StopCommand(ProcessService processService, InstanceService instanceService) {
        this.processService = processService;
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        try {
            String resolvedVersion;

            // If no version specified, find the running instance
            if (version == null || version.trim().isEmpty()) {
                String runningVersion = instanceService.getRunningInstance();

                if (runningVersion == null) {
                    System.out.println("No instance is currently running.");
                    return 0;
                }

                resolvedVersion = runningVersion;
                System.out.println("Stopping the running instance: " + resolvedVersion);
            } else {
                // Find matching installed version (supports version string or numeric index)
                resolvedVersion = resolveVersionOrIndex(version);
                if (resolvedVersion == null) {
                    System.err.println("Error: No installed instance found matching: " + version);
                    System.err.println();
                    System.err.println("List installed instances with:");
                    System.err.println("  sqman list");
                    return 1;
                }

                // Check if this specific version is running
                if (!instanceService.isInstanceRunning(resolvedVersion)) {
                    System.err.println("Error: Instance " + resolvedVersion + " is not running.");
                    return 1;
                }
            }

            System.out.println("Stopping SonarQube " + resolvedVersion);
            if (force) {
                System.out.println("Mode: Force kill");
            }
            System.out.println();

            // Stop the instance
            boolean stopped = processService.stopInstance(resolvedVersion, force);

            if (stopped) {
                return 0;
            } else {
                System.err.println();
                System.err.println("Failed to stop instance. Try with --force flag:");
                System.err.println("  sqman stop --force");
                return 1;
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error stopping instance: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Resolve version input - handles both version strings and numeric indices.
     * If input is a number, treats it as an index from the list command (1-based).
     */
    private String resolveVersionOrIndex(String versionInput) {
        // Check if input is a numeric index
        try {
            int index = Integer.parseInt(versionInput);
            return resolveByIndex(index);
        } catch (NumberFormatException e) {
            // Not a number, treat as version string
            return resolveVersion(versionInput);
        }
    }

    /**
     * Resolve version by index from installed instances list.
     * Index is 1-based (matching the output of sqman list).
     */
    private String resolveByIndex(int index) {
        if (index < 1) {
            System.err.println("Error: Index must be >= 1");
            return null;
        }

        List<String> installed = instanceService.listInstalled();
        if (installed.isEmpty()) {
            System.err.println("Error: No instances installed");
            return null;
        }

        if (index > installed.size()) {
            System.err.println("Error: Index " + index + " out of range (max: " + installed.size() + ")");
            return null;
        }

        // Convert 1-based index to 0-based
        String dirName = installed.get(index - 1);
        return dirName.replace("sonarqube-", "");
    }

    /**
     * Resolve version string to an exact installed version.
     * Supports full version (10.3.0.82913) or partial (10.3).
     */
    private String resolveVersion(String versionInput) {
        // First check if exact version is installed
        if (instanceService.isInstalled(versionInput)) {
            return versionInput;
        }

        // Search for matching versions
        List<String> matches = instanceService.getInstalledByVersion(versionInput);

        if (matches.isEmpty()) {
            return null;
        }

        if (matches.size() == 1) {
            // Extract version from directory name (sonarqube-X.Y.Z)
            return matches.get(0).replace("sonarqube-", "");
        }

        // Multiple matches - show options
        System.err.println("Multiple instances match '" + versionInput + "':");
        for (String match : matches) {
            String ver = match.replace("sonarqube-", "");
            System.err.println("  - " + ver);
        }
        System.err.println();
        System.err.println("Please specify a more precise version.");
        return null;
    }
}
