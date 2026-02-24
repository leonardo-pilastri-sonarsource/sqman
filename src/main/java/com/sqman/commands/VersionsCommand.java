package com.sqman.commands;

import com.sqman.model.Edition;
import com.sqman.model.SonarQubeVersion;
import com.sqman.service.SonarQubeVersionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command to list available SonarQube versions.
 * Shows SQCB (Community Build) and SQS (Server) versions separately.
 * Build numbers differ between SQCB and SQS.
 */
@Command(
    name = "versions",
    description = "List available SonarQube versions for download",
    mixinStandardHelpOptions = true
)
public class VersionsCommand implements Callable<Integer> {

    @Option(
        names = {"-l", "--limit"},
        description = "Limit number of versions to display (default: 10, 0 for all)",
        defaultValue = "10"
    )
    private int limit;

    @Option(
        names = {"-t", "--type"},
        description = "Filter by type: sqcb (Community Build), sqs (Server), or all (default: all)",
        defaultValue = "all"
    )
    private String type;

    private final SonarQubeVersionService versionService;

    public VersionsCommand() {
        this.versionService = new SonarQubeVersionService();
    }

    @Override
    public Integer call() {
        try {
            // Fetch versions from multiple editions
            // COMMUNITY = SQCB versions, DATACENTER = SQS versions
            Set<SonarQubeVersion> allVersionsSet = new LinkedHashSet<>();

            // Fetch SQCB (Community Build) versions
            List<SonarQubeVersion> communityVersions = versionService.fetchAvailableVersions(Edition.COMMUNITY);
            allVersionsSet.addAll(communityVersions);

            // Fetch SQS (Server) versions from DataCenter edition
            List<SonarQubeVersion> datacenterVersions = versionService.fetchAvailableVersions(Edition.DATACENTER);
            allVersionsSet.addAll(datacenterVersions);

            // Convert back to list and sort
            List<SonarQubeVersion> allVersions = new ArrayList<>(allVersionsSet);
            allVersions.sort((v1, v2) -> v2.compareTo(v1)); // Descending order

            if (allVersions.isEmpty()) {
                System.out.println("No versions found.");
                return 0;
            }

            // Filter by type if specified
            List<SonarQubeVersion> filteredVersions = filterByType(allVersions);

            if (filteredVersions.isEmpty()) {
                System.out.println("No versions found for type: " + type);
                return 0;
            }

            int displayCount = (limit == 0) ? filteredVersions.size() : Math.min(limit, filteredVersions.size());

            // Display header
            System.out.println("Available SonarQube versions (showing " + displayCount + " of " + filteredVersions.size() + "):\n");

            // Display versions with type labels
            for (int i = 0; i < displayCount; i++) {
                SonarQubeVersion version = filteredVersions.get(i);
                String typeLabel = version.isCommunityBuild() ? "[SQCB]" : "[SQS] ";
                System.out.printf("  %3d. %-7s %s%n", (i + 1), typeLabel, version.getVersion());
            }

            if (displayCount < filteredVersions.size()) {
                System.out.println("\n  ... and " + (filteredVersions.size() - displayCount) + " more");
                System.out.println("  (use --limit 0 to show all)");
            }

            // Show statistics
            long sqcbCount = filteredVersions.stream().filter(SonarQubeVersion::isCommunityBuild).count();
            long sqsCount = filteredVersions.stream().filter(SonarQubeVersion::isServer).count();

            System.out.println("\nLegend:");
            System.out.println("  [SQCB] - SonarQube Community Build");
            System.out.println("  [SQS]  - SonarQube Server (commercial editions)");

            System.out.println("\nTotal: " + filteredVersions.size() + " versions (" + sqcbCount + " SQCB, " + sqsCount + " SQS)");

            System.out.println("\nDownload with:");
            System.out.println("  sqman download <version> -e community     # for SQCB");
            System.out.println("  sqman download <version> -e developer     # for SQS (Developer)");
            System.out.println("  sqman download <version> -e enterprise    # for SQS (Enterprise)");
            System.out.println("  sqman download <version> -e datacenter    # for SQS (DataCenter)");

            return 0;
        } catch (Exception e) {
            System.err.println("Error fetching versions: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private List<SonarQubeVersion> filterByType(List<SonarQubeVersion> versions) {
        if ("all".equalsIgnoreCase(type)) {
            return versions;
        } else if ("sqcb".equalsIgnoreCase(type)) {
            return versions.stream()
                    .filter(SonarQubeVersion::isCommunityBuild)
                    .collect(Collectors.toList());
        } else if ("sqs".equalsIgnoreCase(type)) {
            return versions.stream()
                    .filter(SonarQubeVersion::isServer)
                    .collect(Collectors.toList());
        } else {
            System.err.println("Invalid type: " + type + ". Use sqcb, sqs, or all.");
            return versions;
        }
    }
}
