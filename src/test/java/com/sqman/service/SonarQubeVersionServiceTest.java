package com.sqman.service;

import com.sqman.model.Edition;
import com.sqman.model.SonarQubeVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SonarQubeVersionServiceTest {

    @Test
    void testFetchCommunityVersions() throws Exception {
        SonarQubeVersionService service = new SonarQubeVersionService();
        List<SonarQubeVersion> versions = service.fetchAvailableVersions(Edition.COMMUNITY);

        assertNotNull(versions);
        assertFalse(versions.isEmpty(), "Should find at least one version");

        // Verify version format (X.Y.Z or X.Y.Z.BUILD)
        for (SonarQubeVersion version : versions) {
            String versionStr = version.getVersion();
            assertTrue(versionStr.matches("\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?"),
                "Version should match pattern: " + versionStr);
        }

        System.out.println("Found " + versions.size() + " versions");
        System.out.println("Latest: " + versions.get(0));
    }

    @Test
    void testVersionComparison() throws Exception {
        SonarQubeVersionService service = new SonarQubeVersionService();
        List<SonarQubeVersion> versions = service.fetchAvailableVersions(Edition.COMMUNITY);

        // Versions should be sorted in descending order
        if (versions.size() >= 2) {
            SonarQubeVersion first = versions.get(0);
            SonarQubeVersion second = versions.get(1);

            // Parse major version and ensure first is >= second
            int firstMajor = Integer.parseInt(first.getVersion().split("\\.")[0]);
            int secondMajor = Integer.parseInt(second.getVersion().split("\\.")[0]);

            assertTrue(firstMajor >= secondMajor,
                "Versions should be sorted descending: " + first.getVersion() + " vs " + second.getVersion());
        }
    }
}
