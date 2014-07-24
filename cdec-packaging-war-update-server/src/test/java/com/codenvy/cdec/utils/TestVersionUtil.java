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
package com.codenvy.cdec.utils;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * @author Anatoliy Bazko
 */
public class TestVersionUtil {

    @Test(dataProvider = "getValidVersions")
    public void testValidVersion(String version) throws Exception {
        assertTrue("Version is invalid: " + version, VersionUtil.isValidVersion(version));
    }

    @Test(dataProvider = "getInvalidVersions")
    public void testInvalidVersion(String version) throws Exception {
        assertFalse("Version is valid: " + version, VersionUtil.isValidVersion(version));
    }

    @Test
    public void testParseValidVersion() throws Exception {
        assertEquals(VersionUtil.parse("1.0.1"), new VersionUtil.Version(1, 0, 1));
        assertEquals(VersionUtil.parse("10.150.200"), new VersionUtil.Version(10, 150, 200));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseInvalidVersion() throws Exception {
        VersionUtil.parse("0.1.1");
    }

    @Test
    public void testCompare() throws Exception {
        assertEquals(VersionUtil.compare("1.0.1", "1.0.1"), 0);
        assertEquals(VersionUtil.compare("2.0.1", "1.0.1"), 1);
        assertEquals(VersionUtil.compare("1.1.1", "1.0.1"), 1);
        assertEquals(VersionUtil.compare("1.0.2", "1.0.1"), 1);
        assertEquals(VersionUtil.compare("1.0.1", "2.0.1"), -1);
        assertEquals(VersionUtil.compare("1.0.1", "1.1.1"), -1);
        assertEquals(VersionUtil.compare("1.0.1", "1.0.2"), -1);
    }

    @DataProvider(name = "getValidVersions")
    public static Object[][] getValidVersions() {
        return new Object[][]{{"1.0.1"}, {"10.3.0"}, {"1.0.0"}, {"1.0.10"}};
    }

    @DataProvider(name = "getInvalidVersions")
    public static Object[][] getInvalidVersions() {
        return new Object[][]{{"0.0.1"}, {"1"}, {"1.1"}, {"1.1."}, {"1.01.1"}, {"01.1.1"}, {"1.1.01"}};
    }
}
