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

import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfigUtil {

    private ConfigUtil    configUtil;
    private HttpTransport transport;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);
        configUtil = spy(new ConfigUtil("", transport));
    }

    @Test
    public void testConfigProperties() throws Exception {
        Path conf = Paths.get("target", "conf.properties");
        FileUtils.write(conf.toFile(), "user=1\npwd=2\n");

        Map<String, String> m = configUtil.loadConfigProperties(conf);
        assertEquals(m.size(), 2);
        assertEquals(m.get("user"), "1");
        assertEquals(m.get("pwd"), "2");


        m = configUtil.loadConfigProperties(conf.toAbsolutePath());
        assertEquals(m.size(), 2);
        assertEquals(m.get("user"), "1");
        assertEquals(m.get("pwd"), "2");
    }

    @Test(expectedExceptions = FileNotFoundException.class, expectedExceptionsMessageRegExp = "Configuration file 'non-existed' not found")
    public void testLoadNonExistedConfigFile() throws IOException {
        configUtil.loadConfigProperties("non-existed");
    }

    @Test(expectedExceptions = ConfigException.class, expectedExceptionsMessageRegExp = "Can't load properties: error")
    public void testLoadConfigFileWhichCantBeLoad() throws IOException {
        Path confFile = Paths.get("target", "conf.properties");
        FileUtils.write(confFile.toFile(), "user=1\npwd=2\n");

        doThrow(new IOException("error")).when(configUtil).doLoad(any(InputStream.class));
        configUtil.loadConfigProperties(confFile);
    }

    @Test
    public void testLoadDefaultSingleServerCdecConfig() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "a=1\n" +
                                             "b=2\n");
        doReturn(properties).when(transport).download(endsWith("codenvy-single-server-properties/3.1.0"), any(Path.class));

        Map<String, String> m = configUtil.loadCodenvyDefaultProperties("3.1.0", InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        assertEquals(m.size(), 2);
        assertEquals(m.get("a"), "1");
        assertEquals(m.get("b"), "2");
    }

    @Test
    public void testLoadDefaultMultiServerCdecConfig() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "a=1\n" +
                                             "b=2\n");
        doReturn(properties).when(transport).download(endsWith("codenvy-multi-server-properties/3.1.0"), any(Path.class));

        Map<String, String> m = configUtil.loadCdecDefaultProperties("3.1.0", InstallOptions.InstallType.CODENVY_MULTI_SERVER);
        assertEquals(m.size(), 2);
        assertEquals(m.get("a"), "1");
        assertEquals(m.get("b"), "2");
    }

    @Test(expectedExceptions = IOException.class,
          expectedExceptionsMessageRegExp = "Can't download installation properties. error")
    public void testLoadDefaultCdecConfigTransportError() throws Exception {
        doThrow(new IOException("error")).when(transport).download(endsWith("codenvy-multi-server-properties/3.1.0"), any(Path.class));

        configUtil.loadCdecDefaultProperties("3.1.0", InstallOptions.InstallType.CODENVY_MULTI_SERVER);
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Can't load properties: error")
    public void testLoadDefaultCdecConfigLoadError() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "a=1\n" +
                                             "b=2\n");
        doReturn(properties).when(transport).download(endsWith("codenvy-multi-server-properties/3.1.0"), any(Path.class));

        doThrow(new IOException("error")).when(configUtil).doLoad(any(InputStream.class));

        configUtil.loadCdecDefaultProperties("3.1.0", InstallOptions.InstallType.CODENVY_MULTI_SERVER);
    }

    @Test
    public void testMerge() throws Exception {
        Map<String, String> properties1 = ImmutableMap.of("a", "1", "b", "2");
        Map<String, String> properties2 = ImmutableMap.of("a", "2", "c", "3");
        Map<String, String> m = configUtil.merge(properties1, properties2);

        assertEquals(m.size(), 3);
        assertEquals(m.get("a"), "1");
        assertEquals(m.get("b"), "2");
        assertEquals(m.get("c"), "3");
    }

    @Test(dataProvider = "Versions")
    public void testMergeVersion(Map<String, String> properties1, Map<String, String> properties2, String expectedVersion) throws Exception {
        Map<String, String> m = configUtil.merge(properties1, properties2);
        assertEquals(m.get("version"), expectedVersion);
    }

    @DataProvider(name = "Versions")
    public static Object[][] Versions() {
        return new Object[][]{{ImmutableMap.of(Config.VERSION, "1"), ImmutableMap.of(Config.VERSION, "2"), "2"},
                              {ImmutableMap.of(), ImmutableMap.of(Config.VERSION, "2"), "2"},
                              {ImmutableMap.of(Config.VERSION, "1"), ImmutableMap.of(), null},
                              {ImmutableMap.of(), ImmutableMap.of(), null}};
    }

    @Test(dataProvider = "HostUrls")
    public void testMergeHostUrl(Map<String, String> properties1,
                                 Map<String, String> properties2,
                                 Map<String, String> expectedProperties) throws Exception {
        Map<String, String> m = configUtil.merge(properties1, properties2);
        assertEquals(m, expectedProperties);
    }

    @DataProvider(name = "HostUrls")
    public static Object[][] HostUrls() {
        return new Object[][]{
                {ImmutableMap.of(Config.AIO_HOST_URL, "a"), ImmutableMap.of(Config.HOST_URL, "b"), ImmutableMap.of(Config.HOST_URL, "a")},
                {ImmutableMap.of(Config.HOST_URL, "a"), ImmutableMap.of(Config.HOST_URL, "b"), ImmutableMap.of(Config.HOST_URL, "a")},
                {ImmutableMap.of(Config.AIO_HOST_URL, "a"), ImmutableMap.of(Config.AIO_HOST_URL, "b"), ImmutableMap.of(Config.AIO_HOST_URL, "a")},
                {ImmutableMap.of(Config.HOST_URL, "a"), ImmutableMap.of(Config.AIO_HOST_URL, "b"),
                 ImmutableMap.of(Config.HOST_URL, "a", Config.AIO_HOST_URL, "b")}};
    }

    @Test
    public void testLoadInstalledCodenvyProperties() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "#\n" +
                                             "# Please finalize configurations by entering required values below:\n" +
                                             "#\n" +
                                             "# replace test.com placeholder with dns name of your single server installation.\n" +
                                             "# Note: DNS name that you configure must be later used as DNS name of the server where a single " +
                                             "server Codenvy Enterprise will be\n" +
                                             "# installed\n" +
                                             "node \"test.com\" inherits \"base_config\" {\n" +
                                             "  # enter dns name of your single server installation, same as above.\n" +
                                             "  $aio_host_url = \"test.com\"\n" +
                                             "\n" +
                                             "  ###############################\n" +
                                             "  # Codenvy Builder configurations\n" +
                                             "  #\n" +
                                             "  # (Mandatory) builder_max_execution_time -  max execution time in seconds for build process.\n" +
                                             "  # If process doesn't end before this time it may be terminated forcibly.\n" +
                                             "  $builder_max_execution_time = \"600\"\n" +
                                             "\n");

        doReturn(ImmutableList.of(properties).iterator()).when(configUtil)
                                                         .getCodenvyPropertiesFiles(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        Map<String, String> m = configUtil.loadInstalledCodenvyProperties(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        assertEquals(m.size(), 2);
        assertEquals(m.get("aio_host_url"), "test.com");
        assertEquals(m.get("builder_max_execution_time"), "600");
    }

    @Test(expectedExceptions = ConfigException.class)
    public void testLoadInstalledCodenvyPropertiesErrorIfFileAbsent() throws Exception {
        Path properties = Paths.get("target/unexisted");
        doReturn(ImmutableList.of(properties).iterator()).when(configUtil)
                                                         .getCodenvyPropertiesFiles(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        configUtil.loadInstalledCodenvyProperties(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        doReturn(ImmutableList.of(properties).iterator()).when(configUtil).getCssPropertiesFiles(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        configUtil.loadInstalledCssProperties(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
    }

    @Test
    public void testGetCssPropertiesFiles(){
        Iterator<Path> singleServerCssPropertiesFiles = configUtil.getCssPropertiesFiles(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);
        assertEquals(singleServerCssPropertiesFiles.next().toAbsolutePath().toString(), "/etc/puppet/manifests/nodes/single_server/single_server.pp");
        assertEquals(singleServerCssPropertiesFiles.next().toAbsolutePath().toString(), "/etc/puppet/manifests/nodes/single_server/base_config.pp");

        Iterator<Path> multiServerCssPropertiesFiles = configUtil.getCssPropertiesFiles(InstallOptions.InstallType.CODENVY_MULTI_SERVER);
        assertEquals(multiServerCssPropertiesFiles.next().toAbsolutePath().toString(), "/etc/puppet/manifests/nodes/multi_server/custom_configurations.pp");
        assertEquals(multiServerCssPropertiesFiles.next().toAbsolutePath().toString(), "/etc/puppet/manifests/nodes/multi_server/base_configurations.pp");
    }

    @Test
    public void testGetPuppetNodesConfigReplacement() {
        List<NodeConfig> nodes = ImmutableList.of(
            new NodeConfig(NodeConfig.NodeType.API, "api.dev.com"),
            new NodeConfig(NodeConfig.NodeType.DATA, "data.dev.com"),
            new NodeConfig(NodeConfig.NodeType.BUILDER, "builder1.dev.com"),
            new NodeConfig(NodeConfig.NodeType.RUNNER, "runner1.dev.com")
        );

        Map<String, String> expected = ImmutableMap.of("builder.*example.com", "builder\\\\d+\\\\.dev.com",
                                                       "runner.*example.com", "runner\\\\d+\\\\.dev.com",
                                                       "data.example.com", "data.dev.com",
                                                       "api.example.com", "api.dev.com");
        Map<String, String> actual = ConfigUtil.getPuppetNodesConfigReplacement(nodes);

        assertEquals(actual, expected);
    }
}
