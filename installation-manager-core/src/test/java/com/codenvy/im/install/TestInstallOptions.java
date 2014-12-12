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

import com.codenvy.im.config.CodenvySingleServerConfig;
import com.codenvy.im.config.Config;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallOptions {

    @Test
    public void testIsValid() throws Exception {
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
    public void testIsValidCSSInstallationShouldReturnFalseIfEmptyProperties() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());

        assertFalse(options.checkValid());
    }

    @Test
    public void testIsValidCSSInstallation() throws Exception {
        Map<String, String> properties = new HashMap<>();
        for (CodenvySingleServerConfig.Property property : CodenvySingleServerConfig.Property.values()) {
            properties.put(Config.getPropertyName(property), "some value");
        }

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        options.setConfigProperties(properties);

        assertTrue(options.checkValid());
    }
}
