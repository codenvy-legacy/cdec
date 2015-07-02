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
package com.codenvy.im.managers;

import com.codenvy.im.utils.OSUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfig {
    public static final String INITIAL_OS_VERSION = OSUtils.VERSION;

    @After
    public void tearDown() {
        OSUtils.VERSION = INITIAL_OS_VERSION;
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
    public void testGetValueByVersion() throws Exception {
        Config config = new Config(Collections.<String, String>emptyMap());
        assertNotNull(config.getValue(Config.PUPPET_AGENT_VERSION));
        assertNotNull(config.getValue(Config.PUPPET_SERVER_VERSION));
        assertNotNull(config.getValue(Config.PUPPET_RESOURCE_URL));
    }

    @Test(dataProvider = "GetValues")
    public void testGetValues(String propertyName, String propertyValue, String osVersion, List<String> expectedResult) {
        OSUtils.VERSION = osVersion;
        Config config = new Config(Collections.singletonMap(propertyName, propertyValue));
        List<String> result = config.getAllValues(propertyName);
        assertEquals(result, expectedResult);
    }

    @DataProvider(name = "GetValues")
    public static Object[][] GetValues() {
        return new Object[][]{
                {"property", null, "6", null},
                {"property", "", "6", new ArrayList<String>()},
                {"property", "value1", "6", new ArrayList<>(ImmutableList.of("value1"))},
                {"property", "value1,value2", "6", new ArrayList<>(ImmutableList.of("value1", "value2"))},
                {"property", "value1,value2,value3", "6", new ArrayList<>(ImmutableList.of("value1", "value2", "value3"))},
                {Config.PUPPET_AGENT_VERSION, "", "6", new ArrayList<>(ImmutableList.of("puppet-3.4.3-1.el6.noarch"))},
                {Config.PUPPET_AGENT_VERSION, "", "7", new ArrayList<>(ImmutableList.of("puppet-3.5.1-1.el7.noarch"))},
        };
    }

    @Test
    public void testGetMongoAdminPassword() {
        Config config = new Config(ImmutableMap.of(Config.MONGO_ADMIN_PASSWORD_PROPERTY, "pswd"));
        assertEquals(config.getMongoAdminPassword(), "pswd");
    }
}
