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

import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfig {
    public static final String INITIAL_OS_VERSION = OSUtils.VERSION;

    @AfterTest
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

    @Test(dataProvider = "getValues")
    public void testGetValues(String propertyName, String propertyValue, String osVersion, List<String> expectedResult) {
        OSUtils.VERSION = osVersion;
        Config config = new Config(Collections.singletonMap(propertyName, propertyValue));
        List<String> result = config.getAllValues(propertyName);
        assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] getValues() {
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

    @Test(dataProvider = "getEnclosedValue")
    public void testGetValues(String propertyName, String osVersion, Config config, String expectedValue) {
        OSUtils.VERSION = osVersion;
        String result = config.getValue(propertyName);
        assertEquals(result, expectedValue);
    }

    @DataProvider
    public Object[][] getEnclosedValue() {
        Map<String, String> properties = ImmutableMap.of(
            "prop_1", "value_1.1,$prop_2,value_1.2",
            "prop_2", "value_2,$prop_3",
            "prop_3", "$" + Config.PUPPET_AGENT_VERSION  // Config.PUPPET_AGENT_VERSION depends on version
        );

        Config config = new Config(properties);

        return new Object[][]{
            {"prop_1", null, config, String.format("value_1.1,value_2,$%s,value_1.2", Config.PUPPET_AGENT_VERSION)},  // enclosed value stayed as it because of osVersion = null
            {"prop_1", "6", config, "value_1.1,value_2,puppet-3.4.3-1.el6.noarch,value_1.2"},
            {"prop_1", "7", config, "value_1.1,value_2,puppet-3.5.1-1.el7.noarch,value_1.2"},
            {"prop_2", "6", config, "value_2,puppet-3.4.3-1.el6.noarch"},
            {"prop_2", "7", config, "value_2,puppet-3.5.1-1.el7.noarch"},
            {"prop_3", "6", config, "puppet-3.4.3-1.el6.noarch"},
            };
    }
}
