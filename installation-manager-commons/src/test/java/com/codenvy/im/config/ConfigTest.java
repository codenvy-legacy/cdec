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
package com.codenvy.im.config;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class ConfigTest {
    @Test
    public void testLoad() throws IOException {
        InputStream mockIs = new ByteArrayInputStream((TestConfig.Property.TEST_PROPERTY.toString() + "=test_value").getBytes());

        Config testConfig = new TestConfig();
        testConfig.load(mockIs, "test source");

        String testPropertyValue = testConfig.getProperty(TestConfig.Property.TEST_PROPERTY);
        assertEquals(testPropertyValue, "test_value");

        assertEquals(testConfig.getConfigSource(), "test source");
    }

    @Test(expectedExceptions = ConfigException.class, expectedExceptionsMessageRegExp = "Property 'test_property' hasn't been found.")
    public void testGetUnexistsProperty () {
        Config testConfig = new TestConfig();
        testConfig.getProperty(TestConfig.Property.TEST_PROPERTY);
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Property 'test_property' hasn't been found at 'test source'.")
    public void testGetUnexistsPropertyAfterLoad () {
        InputStream mockIs = new ByteArrayInputStream(("unknown_property=test_value").getBytes());

        Config testConfig = new TestConfig();
        testConfig.load(mockIs, "test source");
        testConfig.getProperty(TestConfig.Property.TEST_PROPERTY);
    }

    @Test
    public void testSetProperty () {
        Config testConfig = new TestConfig();
        testConfig.setProperty(TestConfig.Property.TEST_PROPERTY, "test value");

        String testPropertyValue = testConfig.getProperty(TestConfig.Property.TEST_PROPERTY);
        assertEquals(testPropertyValue, "test value");
    }

    @Test
    public void testStore() throws IOException {
        Config testConfig = new TestConfig();
        testConfig.setProperty(TestConfig.Property.TEST_PROPERTY, "test_value");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        testConfig.store(os);

        assertTrue(os.toString().contains("test_property=test_value"), "Config '" + os.toString() + "' didn't contain 'test_property=test_value'.");
    }

    static class TestConfig extends Config {
        enum Property implements ConfigProperty {
            TEST_PROPERTY
        }
    }
}
