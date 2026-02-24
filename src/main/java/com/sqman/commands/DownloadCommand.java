package com.sqman.commands;

import com.sqman.model.Edition;
import com.sqman.model.SonarQubeVersion;
import com.sqman.service.DownloadService;
import com.sqman.service.SonarQubeVersionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command to download SonarQube distributions
 */
@Command(
    name = "download",
    description = "Download a SonarQube distribution",
    mixinStandardHelpOptions = true
)
public class DownloadCommand implements Callable<Integer> {

    @Parameters(
        index = "0",
        description = "SonarQube version to download (e.g., 10.3.0.82913, latest)",
        defaultValue = "latest"
    )
    private String version;

    @Option(
        names = {"-d", "--directory"},
        description = "Target directory for download (default: ~/.sqman)"
    )
    private String targetDirectory;

    @Option(
        names = {"-e", "--edition"},
        description = "Edition to download: community, developer, enterprise, datacenter (default: community)",
        defaultValue = "community"
    )
    private String edition;

    private final DownloadService downloadService;
    private final SonarQubeVersionService versionService;

    public DownloadCommand() {
        this.downloadService = new DownloadService();
        this.versionService = new SonarQubeVersionService();
    }

    // Constructor for testing
    public DownloadCommand(DownloadService downloadService, SonarQubeVersionService versionService) {
        this.downloadService = downloadService;
        this.versionService = versionService;
    }

    @Override
    public Integer call() {
        try {
            // Validate and parse edition
            Edition ed;
            try {
                ed = Edition.fromString(edition);
            } catch (IllegalArgumentException e) {
                System.err.println("Error: Invalid edition '" + edition + "'");
                System.err.println("Valid editions: community, developer, enterprise, datacenter");
                return 1;
            }

            // Resolve version if "latest" is specified
            String resolvedVersion = version;
            if ("latest".equalsIgnoreCase(version)) {
                System.out.println("Resolving latest version for " + ed.getDisplayName() + " edition...");
                try {
                    List<SonarQubeVersion> allVersions = versionService.fetchAvailableVersions(ed);

                    // Filter by SQCB or SQS based on edition
                    List<SonarQubeVersion> filteredVersions;
                    if (ed == Edition.COMMUNITY) {
                        // Community edition uses SQCB builds
                        filteredVersions = allVersions.stream()
                                .filter(SonarQubeVersion::isCommunityBuild)
                                .collect(Collectors.toList());
                    } else {
                        // Commercial editions use SQS builds
                        filteredVersions = allVersions.stream()
                                .filter(SonarQubeVersion::isServer)
                                .collect(Collectors.toList());
                    }

                    if (filteredVersions.isEmpty()) {
                        System.err.println("Error: No versions found");
                        return 1;
                    }

                    resolvedVersion = filteredVersions.get(0).getVersion(); // First version is the latest
                    String buildType = ed == Edition.COMMUNITY ? "SQCB" : "SQS";
                    System.out.println("Latest " + buildType + " version: " + resolvedVersion);
                } catch (Exception e) {
                    System.err.println("Error fetching versions: " + e.getMessage());
                    return 1;
                }
            }

            System.out.println();
            System.out.println("Edition: " + ed.getDisplayName());
            System.out.println("Version: " + resolvedVersion);

            // Use default directory if not specified
            String downloadTarget = (targetDirectory == null || targetDirectory.trim().isEmpty())
                ? null
                : targetDirectory;

            if (downloadTarget == null) {
                System.out.println("Target: " + DownloadService.getSqmanHome());
            } else {
                System.out.println("Target: " + downloadTarget);
            }

            System.out.println();

            // Download and extract
            Path installedPath = downloadService.downloadAndExtract(
                resolvedVersion,
                ed,
                downloadTarget
            );

            System.out.println();
            System.out.println("✓ Installation complete!");
            System.out.println("Location: " + installedPath);
            System.out.println();
            System.out.println("To run this instance:");
            System.out.println("  sqman run " + resolvedVersion);

            return 0;

        } catch (Exception e) {
            System.err.println();
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
