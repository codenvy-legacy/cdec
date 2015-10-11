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

import java.io.File;
import java.io.IOException;
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
    protected static final String TEST_DIR            = "target";
    protected static final String DOWNLOAD_DIR        = "target/download";
    protected static final String UPDATE_API_ENDPOINT = "update/endpoint";
    protected static final String ASSEMBLY_PROPERTIES = "target/assembly.properties";
    protected static final String SAAS_API_ENDPOINT   = "saas/endpoint";
    public static final Path   PUPPET_CONF_FILE = Paths.get("target", "puppet", Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();
    public static final String TEST_VERSION_STR = "3.3.0";

    @BeforeMethod
    public void clear() throws Exception {
        if (exists(PUPPET_CONF_FILE)) {
            delete(PUPPET_CONF_FILE);
        }

        Path props = Paths.get(ASSEMBLY_PROPERTIES);
        if (exists(props)) {
            delete(props);
        }

        FileUtils.deleteDirectory(Paths.get(DOWNLOAD_DIR).toFile());
    }

    protected void createSingleNodeConf() throws Exception {
        FileUtils.writeStringToFile(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                               "    certname = hostname\n" +
                                                               "[main]\n" +
                                                               "  dns_alt_names = puppet\n" +
                                                               "[agent]\n" +
                                                               "  show_diff = true\n" +
                                                               "  pluginsync = true\n" +
                                                               "  report = true\n" +
                                                               "  default_schedules = false\n" +
                                                               "  certname = %s\n" +
                                                               "  runinterval = 300\n" +
                                                               "  configtimeout = 600\n");
    }

    protected void createMultiNodeConf() throws Exception {
        FileUtils.writeStringToFile(PUPPET_CONF_FILE.toFile(), "[main]\n"
                                                               + " server = master.codenvy.onprem\n"
                                                               + " runinterval = 420\n"
                                                               + " configtimeout = 600\n"
                                                               + "\n"
                                                               + "   # The Puppet log directory.\n"
                                                               + "   # The default value is '$vardir/log'.\n"
                                                               + "   logdir = /var/log/puppet\n"
                                                               + "\n"
                                                               + "   # Where Puppet PID files are kept.\n"
                                                               + "   # The default value is '$vardir/run'.\n"
                                                               + "   rundir = /var/run/puppet\n"
                                                               + "\n"
                                                               + "   # Where SSL certificates are kept.\n"
                                                               + "   # The default value is '$confdir/ssl'.\n"
                                                               + "   ssldir = $vardir/ssl\n"
                                                               + "\n"
                                                               + "[master]\n"
                                                               + " certname = master.codenvy.onprem\n"
                                                               + "\n"
                                                               + "[agent]\n"
                                                               + " show_diff = true\n"
                                                               + " pluginsync = true\n"
                                                               + " report = true\n"
                                                               + " default_schedules = false\n"
                                                               + " certname = master.codenvy.onprem\n"
                                                               + "\n"
                                                               + "   # The file in which puppetd stores a list of the classes\n"
                                                               + "   # associated with the retrieved configuratiion.  Can be loaded in\n"
                                                               + "   # the separate ``puppet`` executable using the ``--loadclasses``\n"
                                                               + "   # option.\n"
                                                               + "   # The default value is '$confdir/classes.txt'.\n"
                                                               + "   classfile = $vardir/classes.txt\n"
                                                               + "\n"
                                                               + "   # Where puppetd caches the local configuration.  An\n"
                                                               + "   # extension indicating the cache format is added automatically.\n"
                                                               + "   # The default value is '$confdir/localconfig'.\n"
                                                               + "   localconfig = $vardir/localconfig\n");
    }

    private void createAssemblyProperty() throws IOException {
        File assemblyPropertiesFile = Paths.get(ASSEMBLY_PROPERTIES).toFile();
        FileUtils.deleteQuietly(assemblyPropertiesFile);
        FileUtils.writeStringToFile(assemblyPropertiesFile, "assembly.version=" + TEST_VERSION_STR);
    }

    protected void prepareSingleNodeEnv(ConfigManager configManager, HttpTransport transport) throws Exception {
        prepareSingleNodeEnv(configManager);
        when(transport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"" + TEST_VERSION_STR + "\"}");
    }

    protected void prepareSingleNodeEnv(ConfigManager configManager) throws Exception {
        Map<String, String> properties = ImmutableMap.of("host_url", "hostname",
                                                         Config.VERSION, TEST_VERSION_STR);

        createSingleNodeConf();
        createAssemblyProperty();
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn("http://localhost/api").when(configManager).getApiEndpoint();
        doReturn(new Config(properties)).when(configManager).loadInstalledCodenvyConfig();
    }

    protected void prepareMultiNodeEnv(ConfigManager configManager, HttpTransport transport) throws Exception {
        prepareMultiNodeEnv(configManager);
        when(transport.doOption("http://hostname/api/", null)).thenReturn("{\"ideVersion\":\"" + TEST_VERSION_STR + "\"}");
    }

    protected void prepareMultiNodeEnv(ConfigManager configManager) throws Exception {
        Map<String, String> properties = ImmutableMap.of
                (
                        "api_host_name", "api.example.com",
                        "data_host_name", "data.example.com",
                        "analytics_host_name", "analytics.example.com",
                        "host_url", "hostname",
                        Config.VERSION, TEST_VERSION_STR
                );

        createMultiNodeConf();
        createAssemblyProperty();
        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();
        doReturn(new Config(properties)).when(configManager).loadInstalledCodenvyConfig();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(Paths.get(DOWNLOAD_DIR).toFile());
    }

}
