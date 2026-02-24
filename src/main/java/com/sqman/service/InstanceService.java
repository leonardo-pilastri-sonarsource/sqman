package com.sqman.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service to manage SonarQube instances.
 */
public class InstanceService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceService.class);
    private static final String SQMAN_HOME = System.getProperty("user.home") + "/.sqman";

    /**
     * List all installed SonarQube instances.
     *
     * @return List of instance directory names
     */
    public List<String> listInstalled() {
        Path sqmanDir = Paths.get(SQMAN_HOME);

        if (!Files.exists(sqmanDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> paths = Files.list(sqmanDir)) {
            return paths
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("sonarqube-"))
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing installed instances", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get installed instances matching a version prefix.
     *
     * @param versionPrefix Version prefix to match (e.g., "10.3")
     * @return List of matching instance names
     */
    public List<String> getInstalledByVersion(String versionPrefix) {
        return listInstalled().stream()
            .filter(name -> name.contains(versionPrefix))
            .collect(Collectors.toList());
    }

    /**
     * Check if a specific version is installed.
     *
     * @param version Version to check
     * @return true if installed, false otherwise
     */
    public boolean isInstalled(String version) {
        Path instancePath = Paths.get(SQMAN_HOME, "sonarqube-" + version);
        return Files.exists(instancePath) && Files.isDirectory(instancePath);
    }

    /**
     * Get the path to a specific instance.
     *
     * @param version Version identifier
     * @return Path to instance directory
     */
    public Path getInstancePath(String version) {
        return Paths.get(SQMAN_HOME, "sonarqube-" + version);
    }

    /**
     * Delete a SonarQube instance.
     *
     * @param version Version identifier
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteInstance(String version) {
        Path instancePath = getInstancePath(version);

        if (!Files.exists(instancePath)) {
            logger.warn("Instance not found: {}", version);
            return false;
        }

        try {
            deleteDirectory(instancePath);
            logger.info("Deleted instance: {}", version);
            return true;
        } catch (IOException e) {
            logger.error("Error deleting instance: {}", version, e);
            return false;
        }
    }

    /**
     * Recursively delete a directory and all its contents.
     *
     * @param directory Path to directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((p1, p2) -> -p1.compareTo(p2)) // Reverse order to delete children first
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.error("Failed to delete: {}", path, e);
                        throw new RuntimeException("Failed to delete: " + path, e);
                    }
                });
        }
    }

    /**
     * Check if any SonarQube instance is currently running.
     *
     * @return Version of the running instance, or null if none running
     */
    public String getRunningInstance() {
        ProcessService processService = new ProcessService();

        for (String dirName : listInstalled()) {
            String version = dirName.replace("sonarqube-", "");
            Path instancePath = getInstancePath(version);

            if (processService.isInstanceRunning(instancePath)) {
                return version;
            }
        }

        return null;
    }

    /**
     * Check if a specific version is currently running.
     *
     * @param version Version identifier
     * @return true if running, false otherwise
     */
    public boolean isInstanceRunning(String version) {
        ProcessService processService = new ProcessService();
        Path instancePath = getInstancePath(version);
        return processService.isInstanceRunning(instancePath);
    }

    /**
     * Get SQMan home directory.
     */
    public static String getSqmanHome() {
        return SQMAN_HOME;
    }
}
