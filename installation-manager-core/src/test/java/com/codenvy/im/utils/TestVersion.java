/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
    @Test(dataProvider = "testValidVersion")
    public void testValidVersion(String version) throws Exception {
        assertTrue("Version is invalid: " + version, Version.isValidVersion(version));
    }

    @DataProvider(name = "testValidVersion")
    public static Object[][] testValidVersion() {
        return new Object[][]{{"0.0.1"},
                              {"1.0.1"},
                              {"10.3.0"},
                              {"10.3.0.0"},
                              {"10.3.0.1"},
                              {"10.3.0.1-RC"},
                              {"0.9.0"},
                              {"1.0.0"},
                              {"1.0.10"},
                              {"1.0.10-RC"},
                              {"1.0.1-SNAPSHOT"},
                              {"1.0.1-RC-SNAPSHOT"},
                              {"1.0.1.0-SNAPSHOT"},
                              {"1.0.1-M1"},
                              {"1.0.1.1-M1"},
                              {"1.0.1-M1-SNAPSHOT"},
                              {"1.0.1.2-M1-SNAPSHOT"},
                              {"1.0.1.2-M1-RC-SNAPSHOT"}};
    }

    @Test(dataProvider = "testInvalidVersion")
    public void testInvalidVersion(String version) throws Exception {
        assertFalse("Version is valid: " + version, Version.isValidVersion(version));
    }

    @DataProvider(name = "testInvalidVersion")
    public static Object[][] testInvalidVersion() {
        return new Object[][]{{"1"},
                              {"00.1.1"},
                              {"1.1"},
                              {"1.1."},
                              {"1.1.1."},
                              {"1.01.1"},
                              {"01.1.1"},
                              {"1.1.01"},
                              {"1.0.1-"},
                              {"1.0.1-M"},
                              {"1.0.1-M0"},
                              {"1.0.1-M-SNAPSHOT"},
                              {"1.0.1-M0-SNAPSHOT"},
                              {"1.0.1--SNAPSHOT"},
                              {"1.0.1-beta"},
                              {"1.0.1-SNAPSHOT-RC"}};
    }

    @Test(dataProvider = "testParseValidVersion")
    public void testParseValidVersion(String str, int major, int minor, int bugFix, int hotFix, int milestone, boolean rc, boolean snapshot)
            throws Exception {
        assertEquals(Version.valueOf(str), new Version(major, minor, bugFix, hotFix, milestone, rc, snapshot));
    }


    @DataProvider(name = "testParseValidVersion")
    public Object[][] testParseValidVersion() {
        return new Object[][]{
                {"1.0.1-RC", 1, 0, 1, 0, 0, true, false},
                {"1.0.1.0", 1, 0, 1, 0, 0, false, false},
                {"10.150.200.1", 10, 150, 200, 1, 0, false, false},
                {"10.150.200.24-SNAPSHOT", 10, 150, 200, 24, 0, false, true},
                {"10.150.200-M20", 10, 150, 200, 0, 20, false, false},
                {"10.150.200-M20-RC", 10, 150, 200, 0, 20, true, false},
                {"10.150.200-M20-SNAPSHOT", 10, 150, 200, 0, 20, false, true},
                {"10.150.200-M20-RC-SNAPSHOT", 10, 150, 200, 0, 20, true, true},
        };
    }


    @Test(expectedExceptions = IllegalVersionException.class)
    public void testParseInvalidVersion() throws Exception {
        Version.valueOf("01.1.1");
    }

    @Test(dataProvider = "testToString")
    public void testToString(String str, String result) throws Exception {
        assertEquals(Version.valueOf(str).toString(), result);
    }

    @DataProvider(name = "testToString")
    public Object[][] testToString() {
        return new Object[][]{
                {"10.150.200", "10.150.200"},
                {"10.150.200.0", "10.150.200"},
                {"10.150.200.1", "10.150.200.1"},
                {"10.150.200-M20-SNAPSHOT", "10.150.200-M20-SNAPSHOT"},
                {"10.150.200.20-M20-SNAPSHOT", "10.150.200.20-M20-SNAPSHOT"},
                {"10.150.200-M20", "10.150.200-M20"},
                {"10.150.200-SNAPSHOT", "10.150.200-SNAPSHOT"},
                {"10.150.200-RC-SNAPSHOT", "10.150.200-RC-SNAPSHOT"},
        };
    }


    @Test(dataProvider = "testCompareTo")
    public void testCompareTo(String version1, String version2, int result) throws Exception {
        assertEquals(version1 + " vs " + version2, Version.valueOf(version1).compareTo(Version.valueOf(version2)), result);
    }

    @DataProvider(name = "testCompareTo")
    public Object[][] testCompareTo() {
        return new Object[][]{
                {"1.0.1", "1.0.1", 0},
                {"1.0.1", "1.0.1.0", 0},
                {"1.0.2-M20", "1.0.2-M20", 0},
                {"1.0.2-M20-SNAPSHOT", "1.0.2-M20-SNAPSHOT", 0},
                {"1.0.2.4-M20-SNAPSHOT", "1.0.2.4-M20-SNAPSHOT", 0},
                {"1.0.2.4-M20-RC-SNAPSHOT", "1.0.2.4-M20-RC-SNAPSHOT", 0},
                {"1.0.2-SNAPSHOT", "1.0.2-SNAPSHOT", 0},

                {"2.0.1", "1.0.1", 1},
                {"1.1.1", "1.0.1", 1},
                {"1.0.1.1", "1.0.1", 1},
                {"1.0.1.1", "1.0.1.0", 1},
                {"1.0.2", "1.0.1", 1},
                {"1.0.2", "1.0.1-RC", 1},
                {"1.0.2", "1.0.1-M20", 1},
                {"1.0.2", "1.0.2-SNAPSHOT", 1},
                {"1.0.2-M20", "1.0.2", 1},
                {"1.0.2-M20", "1.0.2-M19", 1},
                {"1.0.2-M20", "1.0.2-M20-SNAPSHOT", 1},
                {"1.0.2-M20", "1.0.2-M20-RC", 1},

                {"1.0.1", "2.0.1", -1},
                {"1.0.1", "1.1.1", -1},
                {"1.1.1.0", "1.1.1.1", -1},
                {"1.0.1", "1.0.2", -1},
                {"1.0.1-SNAPSHOT", "1.0.1", -1},
        };
    }

    @Test(dataProvider = "testIsSuitedFor")
    public void testIsSuitedFor(String version, String pattern, boolean expected) throws Exception {
        boolean actual = Version.valueOf(version).isSuitedFor(pattern);

        assertEquals(expected, actual);
    }

    @DataProvider(name = "testIsSuitedFor")
    public static Object[][] testIsSuitedFor() {
        return new Object[][]{
                {"1.0.1", "1\\.0\\.1", true},
                {"1.0.1", "1\\.0\\.(.*)", true},
                {"1.0.1", "1\\.(.*)\\.1", true},
                {"1.0.1", "(.*)\\.0\\.1", true},
                {"1.0.1", "(.*)\\.(.*)\\.1", true},
                {"1.0.1", "(.*)\\.(.*)\\.(.*)", true},
                {"1.0.1", "1\\.0\\.2", false},
                {"1.0.1", "1\\.1\\.(.*)", false},
        };
    }
}
