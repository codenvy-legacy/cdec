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

import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
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
    protected static final String DOWNLOAD_DIR        = "target/download";
    protected static final String UPDATE_API_ENDPOINT = "update/endpoint";
    protected static final String SAAS_API_ENDPOINT   = "saas/endpoint";
    public static final    Path   PUPPET_CONF_FILE    = Paths.get("target", "puppet", Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();

    @BeforeMethod
    public void clear() throws Exception {
        if (exists(PUPPET_CONF_FILE)) {
            delete(PUPPET_CONF_FILE);
        }

        FileUtils.deleteDirectory(Paths.get(DOWNLOAD_DIR).toFile());
    }

    protected void createSingleNodeConf() throws Exception {
        FileUtils.writeStringToFile(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                               "    certname = hostname\n" +
                                                               "[agent]\n" +
                                                               "    certname = hostname\n");
    }

    protected void createMultiNodeConf() throws Exception {
        FileUtils.writeStringToFile(PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                               "    server = hostname\n" +
                                                               "[agent]\n" +
                                                               "    certname = hostname\n");
    }

    protected void prepareSingleNodeEnv(ConfigManager configManager, HttpTransport transport) throws Exception {
        prepareSingleNodeEnv(configManager);
        when(transport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0\"}");
    }

    protected void prepareSingleNodeEnv(ConfigManager configManager) throws Exception {
        Map<String, String> properties = ImmutableMap.of("host_url", "hostname");

        createSingleNodeConf();
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn("http://localhost/api").when(configManager).getApiEndpoint();
        doReturn(new Config(properties)).when(configManager).loadInstalledCodenvyConfig();
    }

    protected void prepareMultiNodeEnv(ConfigManager configManager, HttpTransport transport) throws Exception {
        prepareMultiNodeEnv(configManager);
        when(transport.doOption("http://hostname/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0\"}");
    }

    protected void prepareMultiNodeEnv(ConfigManager configManager) throws Exception {
        Map<String, String> properties = ImmutableMap.of
                (
                        "api_host_name", "api.example.com",
                        "data_host_name", "data.example.com",
                        "analytics_host_name", "analytics.example.com",
                        "host_url", "hostname"
                );

        createMultiNodeConf();
        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();
        doReturn(new Config(properties)).when(configManager).loadInstalledCodenvyConfig();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get(DOWNLOAD_DIR).toFile());
    }

}
