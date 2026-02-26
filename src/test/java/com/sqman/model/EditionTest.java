package com.sqman.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditionTest {

    @Test
    void testFromStringAllEditions() {
        assertEquals(Edition.COMMUNITY, Edition.fromString("community"));
        assertEquals(Edition.DEVELOPER, Edition.fromString("developer"));
        assertEquals(Edition.ENTERPRISE, Edition.fromString("enterprise"));
        assertEquals(Edition.DATACENTER, Edition.fromString("datacenter"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(Edition.COMMUNITY, Edition.fromString("Community"));
        assertEquals(Edition.COMMUNITY, Edition.fromString("COMMUNITY"));
        assertEquals(Edition.DEVELOPER, Edition.fromString("Developer"));
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Edition.fromString("invalid"));
        assertThrows(IllegalArgumentException.class, () -> Edition.fromString(""));
    }

    @Test
    void testIsCommercial() {
        assertFalse(Edition.COMMUNITY.isCommercial());
        assertTrue(Edition.DEVELOPER.isCommercial());
        assertTrue(Edition.ENTERPRISE.isCommercial());
        assertTrue(Edition.DATACENTER.isCommercial());
    }

    @Test
    void testGetFilenameSuffix() {
        assertNull(Edition.COMMUNITY.getFilenameSuffix());
        assertEquals("developer", Edition.DEVELOPER.getFilenameSuffix());
        assertEquals("enterprise", Edition.ENTERPRISE.getFilenameSuffix());
        assertEquals("datacenter", Edition.DATACENTER.getFilenameSuffix());
    }

    @Test
    void testProperties() {
        assertEquals("community", Edition.COMMUNITY.getName());
        assertEquals("Community", Edition.COMMUNITY.getDisplayName());
        assertTrue(Edition.COMMUNITY.getBinariesUrl().contains("binaries.sonarsource.com"));
        assertEquals("community", Edition.COMMUNITY.toString());

        assertEquals("developer", Edition.DEVELOPER.getName());
        assertEquals("Developer", Edition.DEVELOPER.getDisplayName());
        assertTrue(Edition.DEVELOPER.getBinariesUrl().contains("CommercialDistribution"));

        assertEquals("enterprise", Edition.ENTERPRISE.getName());
        assertEquals("Enterprise", Edition.ENTERPRISE.getDisplayName());

        assertEquals("datacenter", Edition.DATACENTER.getName());
        assertEquals("Data Center", Edition.DATACENTER.getDisplayName());
    }
}
