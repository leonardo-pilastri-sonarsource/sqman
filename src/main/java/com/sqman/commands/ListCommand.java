package com.sqman.commands;

import com.sqman.service.InstanceService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command to list SonarQube instances
 */
@Command(
    name = "list",
    description = "List installed SonarQube instances",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Option(
        names = {"-r", "--running"},
        description = "Show only running instances"
    )
    private boolean runningOnly;

    private final InstanceService instanceService;

    public ListCommand() {
        this.instanceService = new InstanceService();
    }

    // Constructor for testing
    public ListCommand(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    @Override
    public Integer call() {
        System.out.println("Installed SonarQube instances:");
        System.out.println("Location: " + InstanceService.getSqmanHome());
        System.out.println();

        List<String> instances = instanceService.listInstalled();

        if (instances.isEmpty()) {
            System.out.println("No instances found.");
            System.out.println();
            System.out.println("Download an instance with:");
            System.out.println("  sqman download latest");
            return 0;
        }

        System.out.println("Found " + instances.size() + " instance(s):");
        System.out.println();

        for (int i = 0; i < instances.size(); i++) {
            String instance = instances.get(i);
            // Extract version from directory name (sonarqube-X.Y.Z.BUILD)
            String version = instance.replace("sonarqube-", "");
            System.out.println("  " + (i + 1) + ". " + version);
        }

        if (runningOnly) {
            System.out.println();
            System.out.println("[Running instances detection not yet implemented]");
        }

        return 0;
    }
}
