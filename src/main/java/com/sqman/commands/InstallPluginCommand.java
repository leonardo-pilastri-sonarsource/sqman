package com.sqman.commands;

import com.sqman.service.InstanceService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to install a plugin JAR into a SonarQube instance
 */
@Command(
    name = "install-plugin",
    description = "Install a plugin JAR into a SonarQube instance",
    mixinStandardHelpOptions = true
)
public class InstallPluginCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "Path to the plugin JAR file to install"
    )
    private String pluginPath;

    private final InstanceService instanceService;

    public InstallPluginCommand() {
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public InstallPluginCommand(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        try {
            // Validate plugin file exists
            Path pluginFile = Paths.get(pluginPath);
            if (!Files.exists(pluginFile)) {
                System.err.println("Error: Plugin file not found: " + pluginPath);
                return 1;
            }

            if (!Files.isRegularFile(pluginFile)) {
                System.err.println("Error: Path is not a file: " + pluginPath);
                return 1;
            }

            if (!pluginFile.getFileName().toString().endsWith(".jar")) {
                System.err.println("Error: File must be a JAR file (.jar extension)");
                return 1;
            }

            // Prompt user to select target instance
            String targetVersion = promptForInstanceSelection();
            if (targetVersion == null) {
                return 1; // User cancelled or error
            }

            // Install the plugin
            return installPlugin(pluginFile, targetVersion);

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error installing plugin: " + e.getMessage());
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
            System.out.println("No SonarQube instances installed.");
            System.out.println();
            System.out.println("Download an instance with:");
            System.out.println("  sqman download latest");
            return null;
        }

        // Display instances
        System.out.println("Select target SonarQube instance:");
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
     * Install the plugin into the target instance.
     * Handles backup of existing plugin if present (only backs up once - the original).
     */
    private int installPlugin(Path pluginFile, String version) {
        try {
            Path instancePath = instanceService.getInstancePath(version);
            Path extensionsDir = instancePath.resolve("lib/extensions");

            // Verify extensions directory exists
            if (!Files.exists(extensionsDir)) {
                System.err.println("Error: Extensions directory not found: " + extensionsDir);
                System.err.println("Is this a valid SonarQube instance?");
                return 1;
            }

            String newPluginFileName = pluginFile.getFileName().toString();
            String basePluginName = extractBasePluginName(newPluginFileName);

            // Find existing plugin with the same base name (e.g., sonar-java-plugin)
            Path existingPlugin = findExistingPlugin(extensionsDir, basePluginName);

            if (existingPlugin != null) {
                String existingFileName = existingPlugin.getFileName().toString();
                System.out.println("Found existing plugin: " + existingFileName);

                // Backup only if this is the first time (original plugin from distribution)
                boolean backedUp = backupOriginalPluginIfNeeded(instancePath, existingPlugin, basePluginName);

                // Remove the existing plugin
                System.out.println("Removing existing plugin...");
                Files.delete(existingPlugin);
            }

            // Copy the new plugin
            Path targetPluginPath = extensionsDir.resolve(newPluginFileName);
            System.out.println("Installing plugin to: " + targetPluginPath);
            Files.copy(pluginFile, targetPluginPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println();
            System.out.println("✓ Plugin installed successfully!");
            System.out.println();
            System.out.println("Instance: " + version);
            System.out.println("Plugin: " + newPluginFileName);
            System.out.println("Location: " + targetPluginPath);

            // Remind user to restart if instance is running
            if (instanceService.isInstanceRunning(version)) {
                System.out.println();
                System.out.println("⚠ Note: Instance is currently running.");
                System.out.println("Restart the instance for changes to take effect:");
                System.out.println("  sqman stop");
                System.out.println("  sqman run " + version);
            }

            return 0;

        } catch (IOException e) {
            System.err.println();
            System.err.println("Error installing plugin: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Extract base plugin name from JAR filename.
     * Examples:
     *   sonar-java-plugin-8.22.0.41895.jar -> sonar-java-plugin
     *   sonar-java-plugin-SNAPSHOT.jar -> sonar-java-plugin
     *   my-custom-plugin-1.0.0.jar -> my-custom-plugin
     */
    private String extractBasePluginName(String pluginFileName) {
        // Remove .jar extension
        String nameWithoutExt = pluginFileName.replaceFirst("\\.jar$", "");

        // Remove version pattern (matches numbers, dots, dashes, and "SNAPSHOT")
        // Pattern: -X.Y.Z.BUILD or -SNAPSHOT or -X.Y.Z-SNAPSHOT
        String baseName = nameWithoutExt.replaceFirst("-[0-9].*$", "")
                                        .replaceFirst("-SNAPSHOT$", "");

        return baseName;
    }

    /**
     * Find existing plugin in extensions directory with the same base name.
     * Returns the path to the existing plugin, or null if not found.
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

    /**
     * Backup an existing plugin ONLY if it's the original (no backup exists yet).
     * This ensures we only backup the plugin that came with the SonarQube distribution,
     * not subsequent user-installed versions.
     *
     * Returns true if backup was created, false if backup already exists (skipped).
     */
    private boolean backupOriginalPluginIfNeeded(Path instancePath, Path existingPluginPath, String basePluginName)
            throws IOException {

        // Create original-plugins directory if it doesn't exist
        Path backupDir = instancePath.resolve("original-plugins");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
            System.out.println("Created backup directory: " + backupDir);
        }

        // Check if a backup already exists for this plugin base name
        boolean backupExists = false;
        try (var stream = Files.list(backupDir)) {
            backupExists = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".jar"))
                .anyMatch(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith(basePluginName + "-");
                });
        }

        if (backupExists) {
            System.out.println("Backup already exists for this plugin (original already saved)");
            System.out.println("Skipping backup...");
            System.out.println();
            return false;
        }

        // No backup exists - this is the original plugin, so back it up
        String existingFileName = existingPluginPath.getFileName().toString();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String backupFileName = existingFileName.replace(".jar", "") + "-backup-" + timestamp + ".jar";
        Path backupPath = backupDir.resolve(backupFileName);

        // Copy existing plugin to backup
        System.out.println("Backing up ORIGINAL plugin to: " + backupPath.getFileName());
        Files.copy(existingPluginPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✓ Original plugin backed up");
        System.out.println();

        return true;
    }
}
