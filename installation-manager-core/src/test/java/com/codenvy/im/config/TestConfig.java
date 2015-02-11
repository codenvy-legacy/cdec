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
package com.codenvy.im.config;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfig {

    @Test
    public void testIsValid() throws Exception {
        assertFalse(Config.isValid(null));
        assertTrue(Config.isValid(""));
        assertFalse(Config.isValid(Config.MANDATORY));
        assertTrue(Config.isValid("test"));
    }

    @Test
    public void testIsMandatory() throws Exception {
        assertFalse(Config.isMandatory(null));
        assertFalse(Config.isMandatory(""));
        assertTrue(Config.isMandatory(Config.MANDATORY));
        assertFalse(Config.isMandatory("test"));
    }

    @Test
    public void testIsValidForMandatoryProperty() throws Exception {
        assertFalse(Config.isValidForMandatoryProperty(null));
        assertFalse(Config.isValidForMandatoryProperty(""));
        assertFalse(Config.isValidForMandatoryProperty(Config.MANDATORY));
        assertTrue(Config.isValidForMandatoryProperty("test"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(Config.isEmpty(null));
        assertTrue(Config.isEmpty(""));
        assertFalse(Config.isEmpty(Config.MANDATORY));
        assertFalse(Config.isEmpty("test"));
    }

        @Test
    public void testIsValidDefaultConfig() throws Exception {
        Config config = new Config(Collections.<String, String>emptyMap());
        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidCSSConfig() throws Exception {
        Map<String, String> properties = ImmutableMap.of("some property", "some value");

        Config config = new Config(properties);
        assertTrue(config.isValid());
    }

    @Test
    public void testIsValidCSSConfigError() throws Exception {
        Map<String, String> properties = ImmutableMap.of("some property", "MANDATORY");

        Config config = new Config(properties);
        assertFalse(config.isValid());
    }

    @Test
    public void testGetHostUrl() throws Exception {
        Config config = new Config(ImmutableMap.of(Config.HOST_URL, "host"));
        assertEquals(config.getHostUrl(), "host");

        config = new Config(ImmutableMap.of(Config.AIO_HOST_URL, "host"));
        assertEquals(config.getHostUrl(), "host");

        config = new Config(ImmutableMap.of(Config.HOST_URL, "host1", Config.AIO_HOST_URL, "host2"));
        assertEquals(config.getHostUrl(), "host1");
    }

    @Test
    public void testGetPropertyByVersion() throws Exception {
        Config config = new Config(Collections.<String, String>emptyMap());
        assertNotNull(config.getProperty(Config.PUPPET_AGENT_VERSION));
        assertNotNull(config.getProperty(Config.PUPPET_SERVER_VERSION));
        assertNotNull(config.getProperty(Config.PUPPET_RESOURCE_URL));
    }
}
