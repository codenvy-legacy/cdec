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
package com.codenvy.im.install;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestInstallOptions {
    @Test(dataProvider = "test data")
    public void testCheckValidSingleServerOptions(Map<String, String> properties, boolean expectedResult) throws Exception {
        InstallOptions options = new InstallOptions();
        assertTrue(options.checkValid());

        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);

        assertFalse(options.checkValid());

        options.setConfigProperties(properties);
        assertEquals(options.checkValid(), expectedResult);
    }

    @Test(dataProvider = "test data")
    public void testCheckValidMultiServerOptions(Map<String, String> properties, boolean expectedResult) throws Exception {
        InstallOptions options = new InstallOptions();
        assertTrue(options.checkValid());

        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);

        assertFalse(options.checkValid());

        options.setConfigProperties(properties);
        assertEquals(options.checkValid(), expectedResult);
    }

    @DataProvider(name = "test data")
    public static Object[][] HostUrls() {
        return new Object[][]{
            {null, false},
            {new HashMap<String, String>(){{put("some property", null);}}, false},
            {ImmutableMap.of("some property", ""), true},
            {ImmutableMap.of("some property", "MANDATORY"), false},
            {ImmutableMap.of("some property", "test", "property2", "MANDATORY"), false},
            {ImmutableMap.of("some property", "some value", "property 2", ""), true},
            {ImmutableMap.of("some property", "test"), true}
        };
    }
}
