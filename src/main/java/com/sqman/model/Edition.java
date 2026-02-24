package com.sqman.model;

/**
 * SonarQube editions available for download
 */
public enum Edition {
    COMMUNITY("community", 
              "Community", 
              "https://binaries.sonarsource.com/Distribution/sonarqube/"),
    
    DEVELOPER("developer", 
              "Developer", 
              "https://binaries.sonarsource.com/CommercialDistribution/sonarqube-developer/"),
    
    ENTERPRISE("enterprise", 
               "Enterprise", 
               "https://binaries.sonarsource.com/CommercialDistribution/sonarqube-enterprise/"),
    
    DATACENTER("datacenter", 
               "Data Center", 
               "https://binaries.sonarsource.com/CommercialDistribution/sonarqube-datacenter/");

    private final String name;
    private final String displayName;
    private final String binariesUrl;

    Edition(String name, String displayName, String binariesUrl) {
        this.name = name;
        this.displayName = displayName;
        this.binariesUrl = binariesUrl;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBinariesUrl() {
        return binariesUrl;
    }

    /**
     * Check if this is a commercial edition (Developer, Enterprise, DataCenter).
     * Community edition is not commercial.
     */
    public boolean isCommercial() {
        return this != COMMUNITY;
    }

    /**
     * Get the edition suffix for filename.
     * Commercial editions include the edition name in the filename.
     * Example: "developer", "enterprise", "datacenter"
     * Community edition returns null (no suffix).
     */
    public String getFilenameSuffix() {
        return isCommercial() ? name : null;
    }

    /**
     * Get Edition from string (case-insensitive)
     */
    public static Edition fromString(String text) {
        for (Edition edition : Edition.values()) {
            if (edition.name.equalsIgnoreCase(text)) {
                return edition;
            }
        }
        throw new IllegalArgumentException("Unknown edition: " + text);
    }

    @Override
    public String toString() {
        return name;
    }
}
