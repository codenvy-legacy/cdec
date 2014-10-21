/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class TestVersion {

    @Test(dataProvider = "getValidVersions")
    public void testValidVersion(String version) throws Exception {
        assertTrue("Version is invalid: " + version, Version.isValidVersion(version));
    }

    @DataProvider(name = "getValidVersions")
    public static Object[][] getValidVersions() {
        return new Object[][]{{"1.0.1"},
                              {"10.3.0"},
                              {"1.0.0"},
                              {"1.0.10"},
                              {"1.0.1-SNAPSHOT"},
                              {"1.0.1-M1"},
                              {"1.0.1-M1-SNAPSHOT"}};
    }


    @Test(dataProvider = "getInvalidVersions")
    public void testInvalidVersion(String version) throws Exception {
        assertFalse("Version is valid: " + version, Version.isValidVersion(version));
    }

    @DataProvider(name = "getInvalidVersions")
    public static Object[][] getInvalidVersions() {
        return new Object[][]{{"0.0.1"},
                              {"1"},
                              {"1.1"},
                              {"1.1."},
                              {"1.01.1"},
                              {"01.1.1"},
                              {"1.1.01"},
                              {"1.0.1-"},
                              {"1.0.1-M"},
                              {"1.0.1-M0"},
                              {"1.0.1-M-SNAPSHOT"},
                              {"1.0.1-M0-SNAPSHOT"},
                              {"1.0.1--SNAPSHOT"},
                              {"1.0.1-beta"}};
    }

    @Test
    public void testParseValidVersion() throws Exception {
        assertEquals(Version.valueOf("1.0.1"), new Version(1, 0, 1, 0, false));
        assertEquals(Version.valueOf("10.150.200"), new Version(10, 150, 200, 0, false));
        assertEquals(Version.valueOf("10.150.200-SNAPSHOT"), new Version(10, 150, 200, 0, true));
        assertEquals(Version.valueOf("10.150.200-M20"), new Version(10, 150, 200, 20, false));
        assertEquals(Version.valueOf("10.150.200-M20-SNAPSHOT"), new Version(10, 150, 200, 20, true));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalidVersion() throws Exception {
        Version.valueOf("0.1.1");
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(Version.valueOf("10.150.200").getAsString(), "10.150.200");
        assertEquals(Version.valueOf("10.150.200-M20-SNAPSHOT").getAsString(), "10.150.200-M20-SNAPSHOT");
        assertEquals(Version.valueOf("10.150.200-M20").getAsString(), "10.150.200-M20");
        assertEquals(Version.valueOf("10.150.200-SNAPSHOT").getAsString(), "10.150.200-SNAPSHOT");
    }

    @Test
    public void testCompare() throws Exception {
        assertEquals(Version.compare("1.0.1", "1.0.1"), 0);
        assertEquals(Version.compare("1.0.2-M20", "1.0.2-M20"), 0);
        assertEquals(Version.compare("1.0.2-M20-SNAPSHOT", "1.0.2-M20-SNAPSHOT"), 0);
        assertEquals(Version.compare("1.0.2-SNAPSHOT", "1.0.2-SNAPSHOT"), 0);

        assertEquals(Version.compare("2.0.1", "1.0.1"), 1);
        assertEquals(Version.compare("1.1.1", "1.0.1"), 1);
        assertEquals(Version.compare("1.0.2", "1.0.1"), 1);
        assertEquals(Version.compare("1.0.2", "1.0.1-M20"), 1);
        assertEquals(Version.compare("1.0.2", "1.0.2-SNAPSHOT"), 1);
        assertEquals(Version.compare("1.0.2-M20", "1.0.2"), 1);
        assertEquals(Version.compare("1.0.2-M20", "1.0.2-M19"), 1);
        assertEquals(Version.compare("1.0.2-M20", "1.0.2-M20-SNAPSHOT"), 1);

        assertEquals(Version.compare("1.0.1", "2.0.1"), -1);
        assertEquals(Version.compare("1.0.1", "1.1.1"), -1);
        assertEquals(Version.compare("1.0.1", "1.0.2"), -1);
        assertEquals(Version.compare("1.0.1-SNAPSHOT", "1.0.1"), -1);
    }

}
