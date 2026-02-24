package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to delete a local SonarQube installation
 */
@Command(
    name = "delete",
    description = "Delete a local SonarQube installation",
    mixinStandardHelpOptions = true
)
public class DeleteCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Version to delete (e.g., 10.3.0.82913, partial like 10.3, or index like 1). If omitted, shows interactive selection.",
        arity = "0..1"
    )
    private String version;

    @Option(
        names = {"-f", "--force"},
        description = "Force deletion even if instance is running (stops it first)"
    )
    private boolean force;

    @Option(
        names = {"-y", "--yes"},
        description = "Skip confirmation prompt"
    )
    private boolean skipConfirmation;

    private final InstanceService instanceService;
    private final ProcessService processService;

    public DeleteCommand() {
        this.instanceService = new InstanceService();
        this.processService = new ProcessService();
    }

    // Constructor for testing
    public DeleteCommand(InstanceService instanceService, ProcessService processService) {
        this.instanceService = instanceService;
        this.processService = processService;
    }

    @Override
    public Integer call() {
        try {
            String resolvedVersion;

            // If no version specified, use interactive selection
            if (version == null || version.trim().isEmpty()) {
                List<String> installed = instanceService.listInstalled();

                if (installed.isEmpty()) {
                    System.out.println("No instances installed.");
                    return 0;
                }

                resolvedVersion = promptForInstanceSelection(installed);
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

            // Check if instance is running
            boolean isRunning = instanceService.isInstanceRunning(resolvedVersion);
            if (isRunning && !force) {
                System.err.println("Error: Instance " + resolvedVersion + " is currently running.");
                System.err.println();
                System.err.println("Stop it first with:");
                System.err.println("  sqman stop");
                System.err.println();
                System.err.println("Or use --force to stop and delete:");
                System.err.println("  sqman delete " + resolvedVersion + " --force");
                return 1;
            }

            // Show what will be deleted
            System.out.println("Instance to delete: " + resolvedVersion);
            System.out.println("Location: " + instanceService.getInstancePath(resolvedVersion));
            if (isRunning) {
                System.out.println("Status: RUNNING (will be stopped)");
            }
            System.out.println();

            // Confirm deletion
            if (!skipConfirmation) {
                System.out.print("Are you sure you want to delete this instance? (yes/no): ");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String confirmation = reader.readLine();

                if (!confirmation.equalsIgnoreCase("yes") && !confirmation.equalsIgnoreCase("y")) {
                    System.out.println("Deletion cancelled.");
                    return 0;
                }
            }

            // Stop instance if running
            if (isRunning) {
                System.out.println("Stopping instance...");
                processService.stopInstance(resolvedVersion, true);
            }

            // Delete the instance
            System.out.println("Deleting instance...");
            boolean deleted = instanceService.deleteInstance(resolvedVersion);

            if (deleted) {
                System.out.println();
                System.out.println("Successfully deleted instance: " + resolvedVersion);
                return 0;
            } else {
                System.err.println();
                System.err.println("Failed to delete instance.");
                return 1;
            }

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error deleting instance: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Show list of installed instances and prompt user to select one to delete.
     * Returns the selected version or null if cancelled/error.
     */
    private String promptForInstanceSelection(List<String> installed) {
        // Display installed instances
        System.out.println("Installed SonarQube instances:");
        System.out.println();
        for (int i = 0; i < installed.size(); i++) {
            String instance = installed.get(i);
            String version = instance.replace("sonarqube-", "");
            boolean isRunning = instanceService.isInstanceRunning(version);
            String status = isRunning ? " (RUNNING)" : "";
            System.out.printf("  %d. %s%s%n", (i + 1), version, status);
        }

        // Prompt for selection
        System.out.println();
        System.out.print("Select instance to delete (1-" + installed.size() + ", or 0 to cancel): ");

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

            // Return selected version (remove sonarqube- prefix)
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
