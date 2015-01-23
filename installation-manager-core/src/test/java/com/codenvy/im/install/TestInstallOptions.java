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
package com.codenvy.im.install;

import com.google.common.collect.ImmutableMap;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestInstallOptions {
    @Test
    public void testCheckValidDefaultOptions() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setConfigProperties(null);
        assertTrue(options.checkValid());

        options.setConfigProperties(new HashMap<String, String>() {{
            put("some property", null);
        }});
        assertTrue(options.checkValid());
    }

    @Test
    public void testCheckValidCodenvySingleServerOptions() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        options.setConfigProperties(null);
        assertFalse(options.checkValid());

        options.setConfigProperties(new HashMap<String, String>(){{put("some property", null);}});
        assertFalse(options.checkValid());

        options.setConfigProperties(ImmutableMap.of("some property", ""));
        assertTrue(options.checkValid());

        options.setConfigProperties(ImmutableMap.of("some property", "MANDATORY"));
        assertFalse(options.checkValid());

        options.setConfigProperties(ImmutableMap.of("some property", "test", "property2", "MANDATORY"));
        assertFalse(options.checkValid());

        options.setConfigProperties(ImmutableMap.of("some property", "test"));
        assertTrue(options.checkValid());
    }


    @Test
    public void testIsValidEmptyOptions() throws Exception {
        InstallOptions options = new InstallOptions();
        assertTrue(options.checkValid());
    }

    @Test
    public void testIsValidCSSInstallationShouldReturnFalseIfNullProperties() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        assertFalse(options.checkValid());
    }

    @Test
    public void testIsValidCSSInstallation() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        Map<String, String> properties = ImmutableMap.of("some property", "some value","property 2", "");
        options.setConfigProperties(properties);

        assertTrue(options.checkValid());
    }
}
