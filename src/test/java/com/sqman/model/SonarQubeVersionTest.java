package com.sqman.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SonarQubeVersionTest {

    @Test
    void testConstructorAndGetters() {
        SonarQubeVersion version = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertEquals("10.3.0.82913", version.getVersion());
        assertEquals(SonarQubeVersion.Type.SQCB, version.getType());
    }

    @Test
    void testIsCommunityBuild() {
        SonarQubeVersion sqcb = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertTrue(sqcb.isCommunityBuild());
        assertFalse(sqcb.isServer());
    }

    @Test
    void testIsServer() {
        SonarQubeVersion sqs = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQS);
        assertTrue(sqs.isServer());
        assertFalse(sqs.isCommunityBuild());
    }

    @Test
    void testCompareToGreater() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.4.0.100000", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertTrue(v1.compareTo(v2) > 0);
    }

    @Test
    void testCompareToLess() {
        SonarQubeVersion v1 = new SonarQubeVersion("9.9.0.65466", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertTrue(v1.compareTo(v2) < 0);
    }

    @Test
    void testCompareToEqual() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertEquals(0, v1.compareTo(v2));
    }

    @Test
    void testCompareToDifferentLength() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0", SonarQubeVersion.Type.SQCB);
        assertTrue(v1.compareTo(v2) > 0); // 82913 > 0 (implicit)
    }

    @Test
    void testSorting() {
        List<SonarQubeVersion> versions = new ArrayList<>();
        versions.add(new SonarQubeVersion("9.9.0.65466", SonarQubeVersion.Type.SQCB));
        versions.add(new SonarQubeVersion("26.2.0.119303", SonarQubeVersion.Type.SQS));
        versions.add(new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB));

        versions.sort((v1, v2) -> v2.compareTo(v1)); // Descending

        assertEquals("26.2.0.119303", versions.get(0).getVersion());
        assertEquals("10.3.0.82913", versions.get(1).getVersion());
        assertEquals("9.9.0.65466", versions.get(2).getVersion());
    }

    @Test
    void testEquals() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertEquals(v1, v2);
    }

    @Test
    void testNotEqualsDifferentVersion() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.4.0.100000", SonarQubeVersion.Type.SQCB);
        assertNotEquals(v1, v2);
    }

    @Test
    void testNotEqualsDifferentType() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQS);
        assertNotEquals(v1, v2);
    }

    @Test
    void testNotEqualsNull() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertNotEquals(null, v1);
    }

    @Test
    void testNotEqualsDifferentClass() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertNotEquals("10.3.0.82913", v1);
    }

    @Test
    void testHashCodeConsistent() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testHashCodeDifferent() {
        SonarQubeVersion v1 = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        SonarQubeVersion v2 = new SonarQubeVersion("10.4.0.100000", SonarQubeVersion.Type.SQCB);
        assertNotEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testToString() {
        SonarQubeVersion sqcb = new SonarQubeVersion("10.3.0.82913", SonarQubeVersion.Type.SQCB);
        assertEquals("SQCB 10.3.0.82913", sqcb.toString());

        SonarQubeVersion sqs = new SonarQubeVersion("26.2.0.119303", SonarQubeVersion.Type.SQS);
        assertEquals("SQS 26.2.0.119303", sqs.toString());
    }

    @Test
    void testTypeProperties() {
        assertEquals("SQCB", SonarQubeVersion.Type.SQCB.getCode());
        assertEquals("Community Build", SonarQubeVersion.Type.SQCB.getDisplayName());
        assertEquals("SQS", SonarQubeVersion.Type.SQS.getCode());
        assertEquals("Server", SonarQubeVersion.Type.SQS.getDisplayName());
    }
}
