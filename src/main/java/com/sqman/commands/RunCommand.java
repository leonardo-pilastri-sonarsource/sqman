package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import com.sqman.service.SonarQubeSetupService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to run a SonarQube instance
 */
@Command(
    name = "run",
    description = "Start a SonarQube instance",
    mixinStandardHelpOptions = true
)
public class RunCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Version to run (e.g., 10.3.0.82913, partial like 10.3, or index like 1). If omitted, shows interactive selection.",
        arity = "0..1"
    )
    private String version;

    private final ProcessService processService;
    private final InstanceService instanceService;

    public RunCommand() {
        this.processService = new ProcessService();
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public RunCommand(ProcessService processService, InstanceService instanceService) {
        this.processService = processService;
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        try {
            // Check if any instance is already running
            String runningVersion = instanceService.getRunningInstance();
            if (runningVersion != null) {
                System.err.println("Error: An instance is already running: " + runningVersion);
                System.err.println();
                System.err.println("Stop it first with:");
                System.err.println("  sqman stop");
                return 1;
            }

            String resolvedVersion;

            // If no version specified, show interactive selection
            if (version == null || version.trim().isEmpty()) {
                resolvedVersion = promptForInstanceSelection();
                if (resolvedVersion == null) {
                    return 1; // User cancelled or error
                }
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
            }

            System.out.println("Running SonarQube " + resolvedVersion);
            System.out.println();

            // Start the instance in background mode on default port (9000)
            boolean started = processService.startInstance(resolvedVersion, 0, true);

            if (!started) {
                return 1;
            }

            // Check if automatic setup is needed (first time running this instance)
            Path instancePath = instanceService.getInstancePath(resolvedVersion);
            SonarQubeSetupService setupService = new SonarQubeSetupService("http://localhost:9000");

            if (setupService.isSetupNeeded(instancePath)) {
                try {
                    // Wait for SonarQube to be fully operational
                    boolean ready = setupService.waitForSonarQubeReady();
                    if (!ready) {
                        System.err.println();
                        System.err.println("⚠ Warning: Could not verify SonarQube is ready.");
                        System.err.println("Automatic setup skipped. You can configure manually at http://localhost:9000");
                        return 0;
                    }

                    // Perform automatic setup
                    setupService.performAutomaticSetup(instancePath);

                } catch (Exception e) {
                    System.err.println();
                    System.err.println("⚠ Warning: Automatic setup failed: " + e.getMessage());
                    System.err.println("You can configure manually at http://localhost:9000");
                    System.err.println("Default credentials: admin/admin");
                    // Don't fail the command - instance is running
                }
            }

            return 0;

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error starting instance: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Show list of installed instances and prompt user to select one.
     * Returns the selected version or null if cancelled/error.
     */
    private String promptForInstanceSelection() {
        List<String> installed = instanceService.listInstalled();

        if (installed.isEmpty()) {
            System.out.println("No instances installed.");
            System.out.println();
            System.out.println("Download an instance with:");
            System.out.println("  sqman download latest");
            return null;
        }

        // Display instances
        System.out.println("Installed SonarQube instances:");
        System.out.println();
        for (int i = 0; i < installed.size(); i++) {
            String dirName = installed.get(i);
            String ver = dirName.replace("sonarqube-", "");
            System.out.printf("  %d. %s%n", (i + 1), ver);
        }

        // Prompt for selection
        System.out.println();
        System.out.print("Select instance to run (1-" + installed.size() + ", or 0 to cancel): ");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine();

            if (input == null || input.trim().isEmpty()) {
                System.out.println("Cancelled.");
                return null;
            }

            int selection = Integer.parseInt(input.trim());

            if (selection == 0) {
                System.out.println("Cancelled.");
                return null;
            }

            if (selection < 1 || selection > installed.size()) {
                System.err.println("Invalid selection.");
                return null;
            }

            // Convert to version
            String dirName = installed.get(selection - 1);
            return dirName.replace("sonarqube-", "");

        } catch (NumberFormatException e) {
            System.err.println("Invalid input. Please enter a number.");
            return null;
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
            return null;
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
