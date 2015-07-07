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

import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestConfigManager extends BaseTest {

    private ConfigManager configManager;
    private HttpTransport transport;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);
        configManager = spy(new ConfigManager("", "target/puppet", transport));
    }

    @Test
    public void testConfigProperties() throws Exception {
        Path conf = Paths.get("target", "conf.properties");
        FileUtils.write(conf.toFile(), "user=1\npwd=2\n");

        Map<String, String> m = configManager.loadConfigProperties(conf);
        assertEquals(m.size(), 2);
        assertEquals(m.get("user"), "1");
        assertEquals(m.get("pwd"), "2");


        m = configManager.loadConfigProperties(conf.toAbsolutePath().toString());
        assertEquals(m.size(), 2);
        assertEquals(m.get("user"), "1");
        assertEquals(m.get("pwd"), "2");
    }

    @Test(expectedExceptions = FileNotFoundException.class, expectedExceptionsMessageRegExp = "Configuration file 'non-existed' not found")
    public void testLoadNonExistedConfigFile() throws IOException {
        configManager.loadConfigProperties("non-existed");
    }

    @Test(expectedExceptions = ConfigException.class, expectedExceptionsMessageRegExp = "Can't load properties: error")
    public void testLoadConfigFileWhichCantBeLoad() throws IOException {
        Path confFile = Paths.get("target", "conf.properties");
        FileUtils.write(confFile.toFile(), "user=1\npwd=2\n");

        doThrow(new IOException("error")).when(configManager).doLoad(any(InputStream.class));
        configManager.loadConfigProperties(confFile);
    }

    @Test
    public void testLoadDefaultSingleServerCdecConfig() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "a=1\n" +
                                             "b=2\n");
        doReturn(properties).when(transport).download(endsWith("codenvy-single-server-properties/3.1.0"), any(Path.class));

        Map<String, String> m = configManager.loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.SINGLE_SERVER);
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

        Map<String, String> m = configManager.loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.MULTI_SERVER);
        assertEquals(m.size(), 2);
        assertEquals(m.get("a"), "1");
        assertEquals(m.get("b"), "2");
    }

    @Test(expectedExceptions = IOException.class,
            expectedExceptionsMessageRegExp = "Can't download installation properties. error")
    public void testLoadDefaultCdecConfigTransportError() throws Exception {
        doThrow(new IOException("error")).when(transport).download(endsWith("codenvy-multi-server-properties/3.1.0"), any(Path.class));

        configManager.loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.MULTI_SERVER);
    }

    @Test(expectedExceptions = ConfigException.class, expectedExceptionsMessageRegExp = "Can't load properties: error")
    public void testLoadDefaultCdecConfigLoadError() throws Exception {
        Path properties = Paths.get("target/test.properties");
        FileUtils.write(properties.toFile(), "a=1\n" +
                                             "b=2\n");
        doReturn(properties).when(transport).download(endsWith("codenvy-multi-server-properties/3.1.0"), any(Path.class));

        doThrow(new IOException("error")).when(configManager).doLoad(any(InputStream.class));

        configManager.loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.MULTI_SERVER);
    }

    @Test
    public void testMerge() throws Exception {
        Version curVersion = Version.valueOf("1.0.0");
        Map<String, String> curProps = ImmutableMap.of("a", "1", "b", "1");
        Map<String, String> newProps = ImmutableMap.of("a", "2", "b", "1", "c", "3");

        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doReturn(ImmutableMap.of("a", "1", "b", "1")).when(configManager).loadCodenvyDefaultProperties(curVersion, InstallType.SINGLE_SERVER);

        Map<String, String> m = configManager.merge(curVersion, curProps, newProps);

        assertEquals(m.size(), 3);
        assertEquals(m.get("a"), "2");
        assertEquals(m.get("b"), "1");
        assertEquals(m.get("c"), "3");
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

        doReturn(ImmutableList.of(properties).iterator()).when(configManager)
                                                         .getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        Map<String, String> m = configManager.loadInstalledCodenvyProperties(InstallType.SINGLE_SERVER);
        assertEquals(m.size(), 2);
        assertEquals(m.get("aio_host_url"), "test.com");
        assertEquals(m.get("builder_max_execution_time"), "600");
    }

    @Test(expectedExceptions = ConfigException.class)
    public void testLoadInstalledCodenvyPropertiesErrorIfFileAbsent() throws Exception {
        Path properties = Paths.get("target/unexisted");
        doReturn(ImmutableList.of(properties).iterator()).when(configManager)
                                                         .getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        configManager.loadInstalledCodenvyProperties(InstallType.SINGLE_SERVER);
        doReturn(ImmutableList.of(properties).iterator()).when(configManager)
                                                         .getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        configManager.loadInstalledCodenvyProperties(InstallType.SINGLE_SERVER);
    }

    @Test
    public void testGetCssPropertiesFiles() {
        Iterator<Path> singleServerCssPropertiesFiles = configManager.getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        assertTrue(singleServerCssPropertiesFiles.next().toAbsolutePath().toString()
                                                 .endsWith("target/puppet/manifests/nodes/single_server/single_server.pp"));
        assertTrue(singleServerCssPropertiesFiles.next().toAbsolutePath().toString()
                                                 .endsWith("target/puppet/manifests/nodes/single_server/base_config.pp"));

        Iterator<Path> multiServerCssPropertiesFiles = configManager.getCodenvyPropertiesFiles(InstallType.MULTI_SERVER);
        assertTrue(multiServerCssPropertiesFiles.next().toAbsolutePath().toString()
                                                .endsWith("target/puppet/manifests/nodes/multi_server/custom_configurations.pp"));
        assertTrue(multiServerCssPropertiesFiles.next().toAbsolutePath().toString()
                                                .endsWith("target/puppet/manifests/nodes/multi_server/base_configurations.pp"));
    }

    @Test
    public void testGetPuppetNodesConfigReplacement() {
        List<NodeConfig> nodes = ImmutableList.of(
                new NodeConfig(NodeConfig.NodeType.API, "api.dev.com", null),
                new NodeConfig(NodeConfig.NodeType.DATA, "data.dev.com", null),
                new NodeConfig(NodeConfig.NodeType.BUILDER, "builder2.dev.com", null),
                new NodeConfig(NodeConfig.NodeType.RUNNER, "runner23.runner89.com", null)
                                                 );

        Map<String, String> expected = ImmutableMap.of("builder.*example.com", "builder\\\\d+\\\\.dev.com",
                                                       "runner.*example.com", "runner\\\\d+\\\\.runner89.com",
                                                       "data.example.com", "data.dev.com",
                                                       "api.example.com", "api.dev.com");
        Map<String, String> actual = ConfigManager.getPuppetNodesConfigReplacement(nodes);

        assertEquals(actual, expected);
    }

    @Test
    public void testLoadInstalledCodenvyConfig() throws IOException {
        Map<String, String> properties = ImmutableMap.of("a", "1", "b", "2");
        doReturn(properties).when(configManager).loadInstalledCodenvyProperties(InstallType.MULTI_SERVER);

        Config result = configManager.loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);
        assertEquals(result.getProperties().toString(), properties.toString());
    }

    @Test(expectedExceptions = UnknownInstallationTypeException.class)
    public void testDetectInstallationTypeErrorIfConfAbsent() throws Exception {
        configManager.detectInstallationType();
    }

    @Test
    public void testDetectInstallationMultiType() throws Exception {
        createMultiNodeConf();
        assertEquals(configManager.detectInstallationType(), InstallType.MULTI_SERVER);
    }

    @Test
    public void testDetectInstallationSingleType() throws Exception {
        createSingleNodeConf();
        assertEquals(configManager.detectInstallationType(), InstallType.SINGLE_SERVER);
    }

    @Test(expectedExceptions = IOException.class)
    public void testFetchMasterHostNameErrorIfFileAbsent() throws Exception {
        configManager.fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFetchMasterHostNameErrorIfFileEmpty() throws Exception {
        doReturn(new Config(new HashMap<String, String>())).when(configManager).loadInstalledCodenvyConfig();

        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "");
        configManager.fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFetchMasterHostNameErrorIfPropertyAbsent() throws Exception {
        doReturn(new Config(new HashMap<String, String>())).when(configManager).loadInstalledCodenvyConfig();

        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]");
        configManager.fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFetchMasterHostNameErrorIfValueEmpty() throws Exception {
        doReturn(new Config(new HashMap<String, String>())).when(configManager).loadInstalledCodenvyConfig();

        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                            "   certname = ");
        configManager.fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testFetchMasterHostNameErrorIfBadFormat() throws Exception {
        doReturn(new Config(new HashMap<String, String>())).when(configManager).loadInstalledCodenvyConfig();

        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                            "    certname  bla.bla.com\n");
        configManager.fetchMasterHostName();
    }

    @Test
    public void testFetchMasterHostName() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                            "certname=master.dev.com\n" +
                                                            "    hostprivkey= $privatekeydir/$certname.pem { mode = 640 }\n" +
                                                            "[agent]\n" +
                                                            "certname=la-la.com");
        assertEquals(configManager.fetchMasterHostName(), "master.dev.com");
    }

    @Test
    public void testFetchMasterHostNameUseCase2() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[agent]\n" +
                                                            "certname=la-la.com\n" +
                                                            "[main]\n" +
                                                            "certname= master.dev.com\n" +
                                                            "    hostprivkey= $privatekeydir/$certname.pem { mode = 640 }\n");
        assertEquals(configManager.fetchMasterHostName(), "master.dev.com");
    }

    @Test
    public void testPrepareInstallPropertiesIMArtifact() throws Exception {
        Map<String, String> properties = configManager.prepareInstallProperties(null,
                                                                                null,
                                                                                ArtifactFactory.createArtifact(InstallManagerArtifact.NAME),
                                                                                null,
                                                                                true);
        assertTrue(properties.isEmpty());
    }

    @Test
    public void testPrepareInstallPropertiesLoadPropertiesFromConfigInstallUseCase() throws Exception {
        Map<String, String> properties = new HashMap<>(ImmutableMap.of("a", "b"));

        doReturn(properties).when(configManager).loadConfigProperties("file");

        Map<String, String> actualProperties = configManager.prepareInstallProperties("file",
                                                                                      InstallType.SINGLE_SERVER,
                                                                                      ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                      Version.valueOf("3.1.0"),
                                                                                      true);
        assertEquals(actualProperties.size(), 1);
        assertEquals(actualProperties.get("a"), "b");
    }

    @Test
    public void testPrepareInstallPropertiesLoadDefaultPropertiesInstallUseCase() throws Exception {
        Map<String, String> expectedProperties = new HashMap<>(ImmutableMap.of("a", "b"));

        doReturn(expectedProperties).when(configManager).loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.SINGLE_SERVER);

        Map<String, String> actualProperties = configManager.prepareInstallProperties(null,
                                                                                      InstallType.SINGLE_SERVER,
                                                                                      ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                      Version.valueOf("3.1.0"),
                                                                                      true);
        assertEquals(actualProperties.size(), 1);
        assertEquals(actualProperties.get("a"), "b");
    }

    @Test
    public void testPrepareInstallPropertiesLoadPropertiesFromConfigUpdateUseCase() throws Exception {
        Map<String, String> properties = new HashMap<>(ImmutableMap.of("a", "1"));

        Artifact artifact = mock(Artifact.class);
        doReturn(Version.valueOf("1.0.0")).when(artifact).getInstalledVersion();
        doReturn(CDECArtifact.NAME).when(artifact).getName();
        doReturn(properties).when(configManager).loadConfigProperties("file");
        doReturn(ImmutableMap.of("b", "2")).when(configManager).loadInstalledCodenvyProperties(InstallType.SINGLE_SERVER);
        doReturn(new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
        }}).when(configManager).merge(any(Version.class), anyMap(), anyMap());

        Map<String, String> actualProperties = configManager.prepareInstallProperties("file",
                                                                                      InstallType.SINGLE_SERVER,
                                                                                      artifact,
                                                                                      Version.valueOf("3.1.0"),
                                                                                      false);
        assertEquals(actualProperties.size(), 2);
        assertEquals(actualProperties.get("a"), "1");
        assertEquals(actualProperties.get("b"), "2");
    }

    @Test
    public void testPrepareInstallPropertiesLoadDefaultPropertiesUpdateUseCase() throws Exception {
        Map<String, String> expectedProperties = new HashMap<>(ImmutableMap.of("a", "1"));

        doReturn(new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
        }}).when(configManager).merge(any(Version.class), anyMap(), anyMap());
        doReturn(expectedProperties).when(configManager).loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.SINGLE_SERVER);
        doReturn(ImmutableMap.of("b", "2")).when(configManager).loadInstalledCodenvyProperties(InstallType.SINGLE_SERVER);

        Map<String, String> actualProperties = configManager.prepareInstallProperties(null,
                                                                                      InstallType.SINGLE_SERVER,
                                                                                      ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                      Version.valueOf("3.1.0"),
                                                                                      false);
        assertEquals(actualProperties.size(), 2);
        assertEquals(actualProperties.get("a"), "1");
        assertEquals(actualProperties.get("b"), "2");
    }

    @Test
    public void testPrepareInstallPropertiesLoadPropertiesUseTemplates() throws Exception {
        Map<String, String> properties = new HashMap<>(ImmutableMap.of("a", "b", "c", "${a}"));

        doReturn(properties).when(configManager).loadConfigProperties("file");
        doReturn("key").when(configManager).readSSHKey(any(Path.class));

        Map<String, String> actualProperties = configManager.prepareInstallProperties("file",
                                                                                      InstallType.MULTI_SERVER,
                                                                                      ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                      Version.valueOf("3.1.0"),
                                                                                      true);
        assertEquals(actualProperties.size(), 4);
        assertEquals(actualProperties.get("a"), "b");
        assertEquals(actualProperties.get("c"), "b");
        assertEquals(actualProperties.get(Config.NODE_SSH_USER_NAME_PROPERTY), System.getProperty("user.name"));
        assertEquals(actualProperties.get(Config.NODE_SSH_USER_PRIVATE_KEY_PROPERTY), "key");
    }

    @Test
    public void testPrepareInstallPropertiesLoadDefaultPropertiesUpdateMultiServerUseCase() throws Exception {
        Map<String, String> expectedProperties = new HashMap<>(ImmutableMap.of("a", "1"));

        doReturn(new HashMap<String, String>() {{
            put("a", "1");
            put("b", "2");
        }}).when(configManager).merge(any(Version.class), anyMap(), anyMap());

        doReturn(expectedProperties).when(configManager).loadCodenvyDefaultProperties(Version.valueOf("3.1.0"), InstallType.MULTI_SERVER);
        doReturn(ImmutableMap.of("b", "2")).when(configManager).loadInstalledCodenvyProperties(InstallType.MULTI_SERVER);
        doReturn("master").when(configManager).fetchMasterHostName();

        Map<String, String> actualProperties = configManager.prepareInstallProperties(null,
                                                                                      InstallType.MULTI_SERVER,
                                                                                      ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                      Version.valueOf("3.1.0"),
                                                                                      false);
        assertEquals(actualProperties.size(), 3);
        assertEquals(actualProperties.get("a"), "1");
        assertEquals(actualProperties.get("b"), "2");
        assertEquals(actualProperties.get(Config.PUPPET_MASTER_HOST_NAME_PROPERTY), "master");
    }

    @Test
    public void testGetApiEndpointSingleServer() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();

        assertEquals(configManager.getApiEndpoint(), "http://localhost/api");
    }

    @Test
    public void testGetApiEndpointMultiServer() throws Exception {
        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();
        doReturn(new Config(ImmutableMap.of("host_protocol", "http", "host_url", "codenvy.onprem")))
                .when(configManager).loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);

        assertEquals(configManager.getApiEndpoint(), "http://codenvy.onprem/api");
    }
}

