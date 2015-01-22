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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestOSUtils {

    @Test
    public void testFetchVersion() throws Exception {
        assertEquals(OSUtils.fetchVersion("6.6"), "6");
        assertEquals(OSUtils.fetchVersion("7"), "7");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFetchVersionErrorUnsupportedVersion() throws Exception {
        OSUtils.fetchVersion("8.1");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFetchVersionError() throws Exception {
        OSUtils.fetchVersion("");
    }

    @Test
    public void testDetectOSVersion() throws Exception {
        String osVersion = OSUtils.detectVersion();
        assertTrue(osVersion == null || OSUtils.SUPPORTED_VERSIONS.contains(osVersion));
    }
}