package com.sqman.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage SonarQube processes (start/stop).
 */
public class ProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);
    private static final String SQMAN_HOME = System.getProperty("user.home") + "/.sqman";
    private static final String SONARQUBE_PID_FILE = "SonarQube.pid";

    public enum Platform {
        WINDOWS("windows-x86-64", "StartSonar.bat"),
        LINUX("linux-x86-64", "sonar.sh"),
        MACOS("macosx-universal-64", "sonar.sh");

        private final String binDir;
        private final String scriptName;

        Platform(String binDir, String scriptName) {
            this.binDir = binDir;
            this.scriptName = scriptName;
        }

        public String getBinDir() {
            return binDir;
        }

        public String getScriptName() {
            return scriptName;
        }
    }

    /**
     * Detect the current operating system platform.
     */
    public Platform detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return Platform.WINDOWS;
        } else if (os.contains("mac")) {
            return Platform.MACOS;
        } else {
            return Platform.LINUX;
        }
    }

    /**
     * Start a SonarQube instance on the default port (9000).
     *
     * @param version Version identifier
     * @param port Unused parameter (kept for compatibility, always uses 9000)
     * @param detached Run in background mode
     * @return true if started successfully
     */
    public boolean startInstance(String version, int port, boolean detached) throws IOException, InterruptedException {
        Path instancePath = Paths.get(SQMAN_HOME, "sonarqube-" + version);

        if (!Files.exists(instancePath)) {
            throw new IOException("Instance not found: " + instancePath);
        }

        // Check if already running
        if (isInstanceRunning(instancePath)) {
            System.out.println("Instance is already running!");
            return false;
        }

        Platform platform = detectPlatform();
        Path scriptPath = instancePath.resolve("bin")
            .resolve(platform.getBinDir())
            .resolve(platform.getScriptName());

        if (!Files.exists(scriptPath)) {
            throw new IOException("Startup script not found: " + scriptPath);
        }

        System.out.println("Starting SonarQube " + version + "...");
        System.out.println("Platform: " + platform.name());
        System.out.println("Script: " + scriptPath);
        System.out.println();

        // Build command based on platform
        ProcessBuilder pb;
        if (platform == Platform.WINDOWS) {
            pb = new ProcessBuilder("cmd", "/c", scriptPath.toString());
        } else {
            // Make script executable
            scriptPath.toFile().setExecutable(true);

            if (detached) {
                // Use start command for background execution
                pb = new ProcessBuilder(scriptPath.toString(), "start");
            } else {
                // Use console command for foreground execution
                pb = new ProcessBuilder(scriptPath.toString(), "console");
            }
        }

        pb.directory(instancePath.toFile());
        pb.redirectErrorStream(true);

        if (detached) {
            // For detached mode, redirect output to log file
            Path logFile = instancePath.resolve("sqman-startup.log");
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));

            Process process = pb.start();
            System.out.println("Started in background mode.");
            System.out.println("Waiting for PID file...");

            // Wait for PID file to be created in bin/{platform}/SonarQube.pid
            Path pidFile = instancePath.resolve("bin")
                .resolve(platform.getBinDir())
                .resolve(SONARQUBE_PID_FILE);
            for (int i = 0; i < 30; i++) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (Files.exists(pidFile)) {
                    String pid = readPidFile(pidFile);
                    System.out.println("✓ SonarQube started with PID: " + pid);
                    System.out.println("✓ Check logs: " + logFile);
                    System.out.println();
                    System.out.println("SonarQube is starting up...");
                    System.out.println("Web UI will be available at: http://localhost:9000");
                    return true;
                }
            }

            System.out.println("⚠ PID file not created yet, but process started.");
            System.out.println("Check logs: " + logFile);
            return true;

        } else {
            // For foreground mode, inherit I/O
            pb.inheritIO();
            System.out.println("Starting in console mode (press Ctrl+C to stop)...");
            System.out.println();

            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        }
    }

    /**
     * Stop a SonarQube instance.
     *
     * @param version Version identifier
     * @param forceKill If true, force kill the process
     * @return true if stopped successfully
     */
    public boolean stopInstance(String version, boolean forceKill) throws IOException, InterruptedException {
        Path instancePath = Paths.get(SQMAN_HOME, "sonarqube-" + version);

        if (!Files.exists(instancePath)) {
            throw new IOException("Instance not found: " + instancePath);
        }

        // PID file is in bin/{platform}/SonarQube.pid
        Platform platform = detectPlatform();
        Path pidFile = instancePath.resolve("bin")
            .resolve(platform.getBinDir())
            .resolve(SONARQUBE_PID_FILE);

        if (!Files.exists(pidFile)) {
            System.out.println("No PID file found. Instance may not be running.");
            return false;
        }

        String pidStr = readPidFile(pidFile);
        if (pidStr == null || pidStr.trim().isEmpty()) {
            System.out.println("Invalid PID file. Cannot stop instance.");
            Files.deleteIfExists(pidFile);
            return false;
        }

        long pid = Long.parseLong(pidStr.trim());
        System.out.println("Stopping SonarQube instance (PID: " + pid + ")...");

        if (platform == Platform.WINDOWS) {
            // On Windows, use taskkill
            return stopWindowsProcess(pid, forceKill);
        } else {
            // On Unix-like systems, try using the sonar.sh stop command first
            if (!forceKill) {
                boolean stopped = stopWithScript(instancePath);
                if (stopped) {
                    // Clean up PID file
                    Files.deleteIfExists(pidFile);
                    return true;
                }
            }

            // Fallback to kill command
            return stopUnixProcess(pid, forceKill, pidFile);
        }
    }

    /**
     * Check if an instance is currently running.
     */
    public boolean isInstanceRunning(Path instancePath) {
        // PID file is in bin/{platform}/SonarQube.pid
        Platform platform = detectPlatform();
        Path pidFile = instancePath.resolve("bin")
            .resolve(platform.getBinDir())
            .resolve(SONARQUBE_PID_FILE);

        if (!Files.exists(pidFile)) {
            return false;
        }

        try {
            String pidStr = readPidFile(pidFile);
            if (pidStr == null || pidStr.trim().isEmpty()) {
                return false;
            }

            long pid = Long.parseLong(pidStr.trim());
            return isProcessRunning(pid);
        } catch (Exception e) {
            logger.warn("Error checking if instance is running", e);
            return false;
        }
    }

    /**
     * Check if a process with given PID is running.
     */
    private boolean isProcessRunning(long pid) {
        Platform platform = detectPlatform();

        try {
            ProcessBuilder pb;
            if (platform == Platform.WINDOWS) {
                pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid, "/NH");
            } else {
                pb = new ProcessBuilder("ps", "-p", String.valueOf(pid));
            }

            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.warn("Error checking process status", e);
            return false;
        }
    }

    /**
     * Read PID from PID file.
     */
    private String readPidFile(Path pidFile) {
        try {
            return Files.readString(pidFile).trim();
        } catch (IOException e) {
            logger.error("Error reading PID file: " + pidFile, e);
            return null;
        }
    }

    /**
     * Stop instance using sonar.sh stop command.
     */
    private boolean stopWithScript(Path instancePath) {
        Platform platform = detectPlatform();
        Path scriptPath = instancePath.resolve("bin")
            .resolve(platform.getBinDir())
            .resolve(platform.getScriptName());

        if (!Files.exists(scriptPath)) {
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(scriptPath.toString(), "stop");
            pb.directory(instancePath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ SonarQube stopped gracefully");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to stop using script", e);
        }

        return false;
    }

    /**
     * Stop Windows process using taskkill.
     */
    private boolean stopWindowsProcess(long pid, boolean force) throws IOException, InterruptedException {
        ProcessBuilder pb;
        if (force) {
            pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
        } else {
            pb = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid));
        }

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("✓ SonarQube stopped");
            return true;
        } else {
            System.err.println("Failed to stop process");
            return false;
        }
    }

    /**
     * Stop Unix process using kill command.
     */
    private boolean stopUnixProcess(long pid, boolean force, Path pidFile) throws IOException, InterruptedException {
        ProcessBuilder pb;
        if (force) {
            System.out.println("Force killing process...");
            pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
        } else {
            System.out.println("Sending SIGTERM...");
            pb = new ProcessBuilder("kill", String.valueOf(pid));
        }

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("✓ SonarQube stopped");
            Files.deleteIfExists(pidFile);
            return true;
        } else {
            System.err.println("Failed to stop process");
            return false;
        }
    }
}
