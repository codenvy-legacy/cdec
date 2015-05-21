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
package com.codenvy.im.artifacts;

import com.codenvy.im.BaseTest;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.helper.CDECArtifactHelper;
import com.codenvy.im.artifacts.helper.CDECMultiServerHelper;
import com.codenvy.im.artifacts.helper.CDECSingleServerHelper;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.UnknownInstallationTypeException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifact extends BaseTest {
    private static final String initialOsVersion = OSUtils.VERSION;
    private static final String SYSTEM_USER_NAME = System.getProperty("user.name");
    
    private CDECArtifact  spyCdecArtifact;
    @Mock
    private HttpTransport transport;
    @Mock
    private ConfigManager configManager;
    @Mock
    private Config        config;
    @Mock
    private Command       mockCommand;
    @Mock
    private CDECArtifactHelper mockHelper;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact("", transport, configManager));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OSUtils.VERSION = initialOsVersion;
    }

    @Test
    public void testGetInstallSingleServerInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallMultiServerInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallSingleServerCommandOsVersion6() throws Exception {
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test
    public void testGetInstallSingleServerCommandOsVersion7() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetInstallSingleServerCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test
    public void testGetInstallMultiServerCommandsForMultiServer() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("site_host_name", "site.example.com"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Site node config not found.")
    public void testGetInstallMultiServerCommandsForMultiServerError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Step number .* is out of install range")
    public void testGetInstallMultiServerCommandUnexistedStepError() throws Exception {
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetInstallMultiServerCommandsWrongOS() throws Exception {
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setStep(1);

        spyCdecArtifact.getInstallCommand(null, null, options);
    }

    @Test
    public void getInstalledVersionShouldReturnNullIfPuppetConfigAbsent() throws Exception {
        doThrow(UnknownInstallationTypeException.class).when(configManager).loadInstalledCodenvyConfig();
        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    @Test
    public void getInstalledVersionShouldReturnNullIfConfigAbsent() throws Exception {
        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
        doThrow(IOException.class).when(configManager).loadInstalledCodenvyConfig();

        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    @Test
    public void getInstalledVersionShouldReturnNullIfRequestThrowException() throws Exception {
        prepareSingleNodeEnv(configManager, transport);
        doThrow(IOException.class).when(transport).doOption("http://localhost/api/", null);

        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    @Test
    public void getInstalledVersionShouldReturnVersion() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        assertEquals(spyCdecArtifact.getInstalledVersion(), Version.valueOf("3.3.0"));
    }

    @Test
    public void getInstalledVersionShouldReturnNullIfRequestEmpty() throws Exception {
        prepareSingleNodeEnv(configManager, transport);
        doReturn("").when(transport).doOption("http://localhost/api/", null);

        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    @Test
    public void getInstalledVersionShouldReturn310CodenvyVersion() throws Exception {
        prepareSingleNodeEnv(configManager, transport);
        when(transport.doOption("http://localhost/api/", null)).thenReturn("{\"implementationVersion\":\"0.26.0\"}");

        assertEquals(spyCdecArtifact.getInstalledVersion(), Version.valueOf("3.1.0"));
    }

    @Test
    public void testGetUpdateSingleServerCommand() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.SINGLE_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("4.0.0"), Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test
    public void testGetUpdateMultiServerCommand() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.MULTI_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("4.0.0"), Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Step number .* is out of update range")
    public void testGetUpdateCommandUnexistedStepError() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromSingleToMultiServerError() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromMultiToSingleServerError() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromSingleToMultiServerError() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromMultiToSingleServerError() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test
    public void testGetBackupSingleServerCommand() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupDirectory("some_dir");
        
        backupConfig.setBackupFile(backupConfig.generateBackupFilePath().toString());

        assertNotNull(spyCdecArtifact.getBackupCommand(backupConfig));
    }

    @Test
    public void testGetBackupMultiServerCommand() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupDirectory("some_dir");
        backupConfig.setBackupFile(backupConfig.generateBackupFilePath().toString());

        assertNotNull(spyCdecArtifact.getBackupCommand(backupConfig));
    }

    @Test
    public void testGetRestoreSingleServerCommand() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupFile("dummyFile")
                                                      .setBackupDirectory("dummyDirectory");

        assertNotNull(spyCdecArtifact.getRestoreCommand(backupConfig));
    }

    @Test
    public void testGetRestoreMultiServerCommand() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupFile("dummyFile")
                                                      .setBackupDirectory("dummyDirectory");

        assertNotNull(spyCdecArtifact.getRestoreCommand(backupConfig));
    }

    @Test
    public void testChangeCodenvyConfig() throws IOException {
        String testProperty = Config.HOST_URL;
        String testValue = "c";
        Config testConfig = new Config(ImmutableMap.of(testProperty, "a", "property2", "b"));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();

        doReturn(InstallType.MULTI_SERVER).when(configManager).detectInstallationType();

        doReturn(mockCommand).when(mockHelper).getChangeConfigCommand(testProperty, testValue, testConfig);
        doReturn(mockHelper).when(spyCdecArtifact).getHelper(InstallType.MULTI_SERVER);

        spyCdecArtifact.changeConfig(testProperty, testValue);
        verify(mockHelper).getChangeConfigCommand(testProperty, testValue, testConfig);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "There is no property 'unknown' in Codenvy configuration")
    public void testChangeCodenvyConfigWhenPropertyAbsent() throws IOException {
        String testProperty = "unknown";
        String testValue = "c";
        Config testConfig = new Config(ImmutableMap.of("property", "b"));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();
        spyCdecArtifact.changeConfig(testProperty, testValue);
    }

    @Test(expectedExceptions = IOException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testChangeCodenvyConfigWhenCommandException() throws IOException {
        String testProperty = "a";
        String testValue = "b";
        Config testConfig = new Config(ImmutableMap.of(testProperty, "c"));
        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();

        doThrow(new CommandException("error", new AgentException("error"))).when(mockCommand).execute();
        doReturn(mockCommand).when(mockHelper).getChangeConfigCommand(testProperty, testValue, testConfig);
        doReturn(mockHelper).when(spyCdecArtifact).getHelper(any(InstallType.class));

        spyCdecArtifact.changeConfig(testProperty, testValue);
    }

    @Test
    public void testGetChangeSingleServerConfigCommand() throws IOException {
        String testProperty = Config.HOST_URL;
        String testValue = "a";
        Config testConfig = new Config(ImmutableMap.of(testProperty, "c"));

        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();

        CDECSingleServerHelper testHelper = new CDECSingleServerHelper(spyCdecArtifact);

        Command command = testHelper.getChangeConfigCommand(testProperty, testValue, testConfig);
        assertEquals(command.toString(), "[" +
                                         "{'command'='sudo cp /etc/puppet/manifests/nodes/single_server/single_server.pp /etc/puppet/manifests/nodes/single_server/single_server.pp.back', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo sed -i 's|$host_url = .*|$host_url = \"a\"|g' /etc/puppet/manifests/nodes/single_server/single_server.pp', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo cp /etc/puppet/manifests/nodes/single_server/base_config.pp /etc/puppet/manifests/nodes/single_server/base_config.pp.back', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo sed -i 's|$host_url = .*|$host_url = \"a\"|g' /etc/puppet/manifests/nodes/single_server/base_config.pp', 'agent'='LocalAgent'}, " +
                                         "{'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; fi;', 'agent'='LocalAgent'}, " +
                                         "Expected to be installed 'codenvy' of the version '1.0.0'" +
                                         "]");
    }

    @Test
    public void testGetChangeMultiServerConfigCommand() throws IOException {
        String testProperty = Config.HOST_URL;
        String testValue = "a";
        Config testConfig = new Config(ImmutableMap.of(testProperty, "c",
                                                       "api_host_name", "api.dev.com",
                                                       "data_host_name", "data.dev.com"));

        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();

        CDECMultiServerHelper testHelper = new CDECMultiServerHelper(spyCdecArtifact);

        Command command = testHelper.getChangeConfigCommand(testProperty, testValue, testConfig);
        assertEquals(command.toString(), format("[" +
                                         "{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo sed -i 's|$host_url = .*|$host_url = \"a\"|g' /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/base_configurations.pp /etc/puppet/manifests/nodes/multi_server/base_configurations.pp.back', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo sed -i 's|$host_url = .*|$host_url = \"a\"|g' /etc/puppet/manifests/nodes/multi_server/base_configurations.pp', 'agent'='LocalAgent'}, " +
                                         "{'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; fi;', 'agent'='{'host'='data.dev.com', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}, " +
                                         "{'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; fi;', 'agent'='{'host'='api.dev.com', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}, " +
                                         "Expected to be installed 'codenvy' of the version '1.0.0'" +
                                         "]", SYSTEM_USER_NAME));
    }
}
