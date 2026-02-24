package com.sqman.model;

/**
 * Represents a SonarQube version with its type (SQCB or SQS).
 */
public class SonarQubeVersion implements Comparable<SonarQubeVersion> {

    public enum Type {
        SQCB("SQCB", "Community Build"),
        SQS("SQS", "Server");

        private final String code;
        private final String displayName;

        Type(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String version;
    private final Type type;

    public SonarQubeVersion(String version, Type type) {
        this.version = version;
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public Type getType() {
        return type;
    }

    public boolean isCommunityBuild() {
        return type == Type.SQCB;
    }

    public boolean isServer() {
        return type == Type.SQS;
    }

    @Override
    public int compareTo(SonarQubeVersion other) {
        return compareVersions(this.version, other.version);
    }

    /**
     * Compare two version strings (e.g., "10.3.0.82913" vs "9.9.0.65466")
     * Returns positive if v1 > v2, negative if v1 < v2, zero if equal
     */
    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return type.getCode() + " " + version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SonarQubeVersion that = (SonarQubeVersion) o;
        return version.equals(that.version) && type == that.type;
    }

    @Override
    public int hashCode() {
        return version.hashCode() * 31 + type.hashCode();
    }
}
