package com.sqman.commands;

import com.sqman.service.InstanceService;
import com.sqman.service.ProcessService;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Command to restart the running SonarQube instance
 */
@Command(
    name = "restart",
    description = "Restart the running SonarQube instance",
    mixinStandardHelpOptions = true
)
public class RestartCommand implements Callable<Integer> {

    private final ProcessService processService;
    private final InstanceService instanceService;

    public RestartCommand() {
        this.processService = new ProcessService();
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public RestartCommand(ProcessService processService, InstanceService instanceService) {
        this.processService = processService;
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        try {
            // Find the running instance
            String runningVersion = instanceService.getRunningInstance();

            if (runningVersion == null) {
                System.out.println("No instance is currently running.");
                System.out.println();
                System.out.println("Start an instance with:");
                System.out.println("  sqman run");
                return 1;
            }

            System.out.println("Restarting SonarQube " + runningVersion);
            System.out.println();

            // Stop the instance gracefully
            System.out.println("Stopping instance...");
            boolean stopped = processService.stopInstance(runningVersion, false);

            if (!stopped) {
                System.err.println();
                System.err.println("Failed to stop instance. Try stopping manually with:");
                System.err.println("  sqman stop --force");
                return 1;
            }

            System.out.println();

            // Start the instance again
            System.out.println("Starting instance...");
            boolean started = processService.startInstance(runningVersion, 0, true);

            if (!started) {
                System.err.println();
                System.err.println("Failed to start instance. Try starting manually with:");
                System.err.println("  sqman run " + runningVersion);
                return 1;
            }

            return 0;

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error restarting instance: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
