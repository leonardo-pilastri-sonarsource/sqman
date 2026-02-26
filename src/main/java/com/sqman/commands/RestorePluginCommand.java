package com.sqman.commands;

import com.sqman.service.InstanceService;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command to restore a backed-up original plugin
 */
@Command(
    name = "restore",
    description = "Restore a backed-up original plugin",
    mixinStandardHelpOptions = true
)
public class RestorePluginCommand implements Callable<Integer> {

    private final InstanceService instanceService;

    public RestorePluginCommand() {
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public RestorePluginCommand(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        try {
            // Prompt user to select instance
            String targetVersion = promptForInstanceSelection();
            if (targetVersion == null) {
                return 1; // User cancelled or error
            }

            // Get backup directory for selected instance
            Path instancePath = instanceService.getInstancePath(targetVersion);
            Path backupDir = instancePath.resolve("original-plugins");

            // Check if backup directory exists
            if (!Files.exists(backupDir)) {
                System.out.println("No backed-up plugins found for instance: " + targetVersion);
                System.out.println();
                System.out.println("Backup directory does not exist: " + backupDir);
                return 0;
            }

            // List backed-up plugins
            List<Path> backups = listBackedUpPlugins(backupDir);
            if (backups.isEmpty()) {
                System.out.println("No backed-up plugins found for instance: " + targetVersion);
                return 0;
            }

            // Prompt user to select plugin to restore
            Path selectedBackup = promptForBackupSelection(backups);
            if (selectedBackup == null) {
                return 1; // User cancelled or error
            }

            // Restore the plugin
            return restorePlugin(instancePath, selectedBackup, targetVersion);

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error restoring plugin: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Show list of installed instances and prompt user to select one.
     */
    private String promptForInstanceSelection() {
        List<String> installed = instanceService.listInstalled();

        if (installed.isEmpty()) {
            System.out.println("No SonarQube instances installed.");
            System.out.println();
            System.out.println("Download an instance with:");
            System.out.println("  sqman download latest");
            return null;
        }

        // Display instances
        System.out.println("Select SonarQube instance:");
        System.out.println();
        for (int i = 0; i < installed.size(); i++) {
            String dirName = installed.get(i);
            String ver = dirName.replace("sonarqube-", "");
            System.out.printf("  %d. %s%n", (i + 1), ver);
        }

        // Prompt for selection
        System.out.println();
        System.out.print("Select instance (1-" + installed.size() + ", or 0 to cancel): ");

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
     * List all backed-up plugin files in the backup directory.
     */
    private List<Path> listBackedUpPlugins(Path backupDir) throws IOException {
        try (var stream = Files.list(backupDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * Prompt user to select a backed-up plugin to restore.
     */
    private Path promptForBackupSelection(List<Path> backups) {
        System.out.println();
        System.out.println("Available backed-up plugins:");
        System.out.println();

        for (int i = 0; i < backups.size(); i++) {
            String fileName = backups.get(i).getFileName().toString();
            System.out.printf("  %d. %s%n", (i + 1), fileName);
        }

        System.out.println();
        System.out.print("Select plugin to restore (1-" + backups.size() + ", or 0 to cancel): ");

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

            if (selection < 1 || selection > backups.size()) {
                System.err.println("Invalid selection.");
                return null;
            }

            return backups.get(selection - 1);

        } catch (NumberFormatException e) {
            System.err.println("Invalid input. Please enter a number.");
            return null;
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
            return null;
        }
    }

    /**
     * Restore a backed-up plugin to the extensions directory.
     */
    private int restorePlugin(Path instancePath, Path backupPath, String version) {
        try {
            Path extensionsDir = instancePath.resolve("lib/extensions");

            // Verify extensions directory exists
            if (!Files.exists(extensionsDir)) {
                System.err.println("Error: Extensions directory not found: " + extensionsDir);
                System.err.println("Is this a valid SonarQube instance?");
                return 1;
            }

            String backupFileName = backupPath.getFileName().toString();

            // Extract the original plugin name (remove -backup-timestamp.jar suffix)
            String originalPluginName = extractOriginalPluginName(backupFileName);
            String basePluginName = extractBasePluginName(originalPluginName);

            // Find and remove any existing version of this plugin
            Path existingPlugin = findExistingPlugin(extensionsDir, basePluginName);
            if (existingPlugin != null) {
                String existingFileName = existingPlugin.getFileName().toString();
                System.out.println("Found existing plugin: " + existingFileName);
                System.out.println("Removing current plugin...");
                Files.delete(existingPlugin);
            }

            // Restore the backed-up plugin (use the original name without backup suffix)
            Path targetPluginPath = extensionsDir.resolve(originalPluginName);
            System.out.println();
            System.out.println("Restoring plugin to: " + targetPluginPath);
            Files.copy(backupPath, targetPluginPath, StandardCopyOption.REPLACE_EXISTING);

            // Delete the backup file after successful restoration
            System.out.println("Deleting backup file...");
            Files.delete(backupPath);

            System.out.println();
            System.out.println("✓ Plugin restored successfully!");
            System.out.println();
            System.out.println("Instance: " + version);
            System.out.println("Plugin: " + originalPluginName);
            System.out.println("Location: " + targetPluginPath);
            System.out.println();
            System.out.println("Note: Backup file has been deleted.");
            System.out.println("If you install a custom plugin again, a new backup will be created.");

            // Remind user to restart if instance is running
            if (instanceService.isInstanceRunning(version)) {
                System.out.println();
                System.out.println("⚠ Note: Instance is currently running.");
                System.out.println("Restart the instance for changes to take effect:");
                System.out.println("  sqman restart");
            }

            return 0;

        } catch (IOException e) {
            System.err.println();
            System.err.println("Error restoring plugin: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Extract original plugin name from backup filename.
     * Example: sonar-java-plugin-8.22.0.41895-backup-20260219-153045.jar
     *       -> sonar-java-plugin-8.22.0.41895.jar
     */
    private String extractOriginalPluginName(String backupFileName) {
        // Remove -backup-YYYYMMDD-HHMMSS.jar suffix
        return backupFileName.replaceFirst("-backup-\\d{8}-\\d{6}\\.jar$", ".jar");
    }

    /**
     * Extract base plugin name from JAR filename.
     * Examples:
     *   sonar-java-plugin-8.22.0.41895.jar -> sonar-java-plugin
     *   sonar-java-plugin-SNAPSHOT.jar -> sonar-java-plugin
     */
    private String extractBasePluginName(String pluginFileName) {
        String nameWithoutExt = pluginFileName.replaceFirst("\\.jar$", "");
        String baseName = nameWithoutExt.replaceFirst("-[0-9].*$", "")
                                        .replaceFirst("-SNAPSHOT$", "");
        return baseName;
    }

    /**
     * Find existing plugin in extensions directory with the same base name.
     */
    private Path findExistingPlugin(Path extensionsDir, String basePluginName) throws IOException {
        try (var stream = Files.list(extensionsDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    String existingBaseName = extractBasePluginName(fileName);
                    return existingBaseName.equals(basePluginName);
                })
                .findFirst()
                .orElse(null);
        }
    }
}
