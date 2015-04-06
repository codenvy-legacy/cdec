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
package com.codenvy.im;

import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author Anatoliy Bazko
 */
public class BaseTest {
    public static final Path PUPPET_CONF_FILE = Paths.get("target", "puppet", Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();

    @BeforeMethod
    public void clear() throws Exception {
        if (exists(PUPPET_CONF_FILE)) {
            delete(PUPPET_CONF_FILE);
        }
    }

    protected void createSingleNodeConf() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                   "    certname = hostname\n" +
                                                   "[agent]\n" +
                                                   "    certname = hostname\n");
    }

    protected void createMultiNodeConf() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                   "\n" +
                                                   "[main]\n" +
                                                   "    certname = hostname\n");
    }

    protected void prepareSingleNodeEnv(ConfigUtil configUtil, HttpTransport transport) throws Exception {
        prepareSingleNodeEnv(configUtil);
        when(transport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0\"}");
    }

    protected void prepareSingleNodeEnv(ConfigUtil configUtil) throws Exception {
        Map<String, String> properties = ImmutableMap.of("host_url", "hostname");

        createSingleNodeConf();
        doReturn(InstallType.SINGLE_SERVER).when(configUtil).detectInstallationType();
        doReturn(new Config(properties)).when(configUtil).loadInstalledCodenvyConfig();
    }

    protected void prepareMultiNodeEnv(ConfigUtil configUtil, HttpTransport transport) throws Exception {
        prepareMultiNodeEnv(configUtil);
        when(transport.doOption("http://hostname/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0\"}");
    }

    protected void prepareMultiNodeEnv(ConfigUtil configUtil) throws Exception {
        Map<String, String> properties = ImmutableMap.of
                (
                        "api_host_name", "api.example.com",
                        "data_host_name", "data.example.com",
                        "host_url", "hostname"
                );

        createMultiNodeConf();
        doReturn(InstallType.MULTI_SERVER).when(configUtil).detectInstallationType();
        doReturn(new Config(properties)).when(configUtil).loadInstalledCodenvyConfig();
    }
}
