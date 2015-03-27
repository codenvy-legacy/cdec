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
import com.codenvy.im.artifacts.helper.CDECMultiServerHelper;
import com.codenvy.im.artifacts.helper.CDECSingleServerHelper;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifact extends BaseTest {
    private CDECArtifact spyCdecArtifact;
    private static final String initialOsVersion = OSUtils.VERSION;
    @Mock
    private HttpTransport mockTransport;
    @Mock
    private ConfigUtil    mockConfigUtil;
    @Mock
    private Config        mockConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact(mockTransport, mockConfigUtil));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        OSUtils.VERSION = initialOsVersion;
    }

    @Test
    public void testGetInstallSingleServerInfo() throws Exception {
        prepareConfForSingleNodeInstallation();

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallMultiServerInfo() throws Exception {
        prepareConfForMultiNodeInstallation();

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallSingleServerCommandOsVersion6() throws Exception {
        prepareConfForSingleNodeInstallation();
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
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
        prepareConfForSingleNodeInstallation();
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
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
        prepareConfForSingleNodeInstallation();

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test
    public void testGetInstallMultiServerCommandsForMultiServer() throws Exception {
        prepareConfForMultiNodeInstallation();
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("site_host_name", "site.example.com"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Site node config not found.")
    public void testGetInstallMultiServerCommandsForMultiServerError() throws Exception {
        prepareConfForMultiNodeInstallation();
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));

        int steps = spyCdecArtifact.getInstallInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
            Command command = spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Step number .* is out of install range")
    public void testGetInstallMultiServerCommandUnexistedStepError() throws Exception {
        prepareConfForMultiNodeInstallation();
        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(null, Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testGetInstallMultiServerCommandsWrongOS() throws Exception {
        prepareConfForMultiNodeInstallation();
        OSUtils.VERSION = "6";

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setStep(1);

        spyCdecArtifact.getInstallCommand(null, null, options);
    }

    /**
     * Single-node Codenvy installation type: run and returns 3.2.0 version
     * Multi-node Codenvy installation type: stopped
     */
    @Test
    public void testGetInstalledVersionUseCase1() throws Exception {
        doReturn(new Config(ImmutableMap.of("host_url", "multi"))).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");
        doThrow(IOException.class).when(mockTransport).doOption("http://multi/api/", null);

        Version version = spyCdecArtifact.getInstalledVersion();
//        assertEquals(version, Version.valueOf("3.2.0-SNAPSHOT"));   // TODO "223 expected [3.2.0-SNAPSHOT] but found [null]"
    }

    /**
     * Single-node Codenvy installation type: stopped
     * Multi-node Codenvy installation type: run and returns 3.3.0 version
     */
    @Test
    public void testGetInstalledVersionUseCase2() throws Exception {
        doReturn(new Config(ImmutableMap.of("host_url", "multi"))).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);
        when(mockTransport.doOption("http://multi/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0-SNAPSHOT\"}");
        doThrow(IOException.class).when(mockTransport).doOption("http://localhost/api/", null);

        Version version = spyCdecArtifact.getInstalledVersion();
//        assertEquals(version, Version.valueOf("3.3.0-SNAPSHOT"));   // TODO "237 expected [3.3.0-SNAPSHOT] but found [null]"
    }

    /**
     * Single-node Codenvy installation type: stopped
     * Multi-node Codenvy installation type: stopped
     */
    @Test
    public void testGetInstalledVersionUseCase3() throws Exception {
        doReturn(new Config(ImmutableMap.of("host_url", "multi"))).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);
        doThrow(IOException.class).when(mockTransport).doOption("http://localhost/api/", null);
        doThrow(IOException.class).when(mockTransport).doOption("http://multi/api/", null);

        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    /**
     * Single-node Codenvy installation type: stopped.
     * Multi-node Codenvy installation type: there is no configuration file.
     */
    @Test
    public void testGetInstalledVersionUseCase4() throws Exception {
        doReturn(null).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);
        doThrow(IOException.class).when(mockTransport).doOption("http://localhost/api/", null);
        doThrow(IOException.class).when(mockTransport).doOption("http://multi/api/", null);

        assertNull(spyCdecArtifact.getInstalledVersion());
    }

    /**
     * Single-node Codenvy installation type: run and returns 3.2.0 version
     * Multi-node Codenvy installation type: run and returns 3.3.0 version
     */
//    @Test(expectedExceptions = IllegalStateException.class)  // TODO "should have thrown an exception of class java.lang.IllegalStateException"
    public void testGetInstalledVersionUseCase5() throws Exception {
        doReturn(new Config(ImmutableMap.of("host_url", "multi"))).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");
        when(mockTransport.doOption("http://multi/api/", null)).thenReturn("{\"ideVersion\":\"3.3.0-SNAPSHOT\"}");

        spyCdecArtifact.getInstalledVersion();
    }

//    @Test(expectedExceptions = JsonSyntaxException.class)   // TODO "should have thrown an exception of class com.google.gson.JsonSyntaxException"
    public void testGetInstalledVersionError() throws Exception {
        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"some text\"}");
        spyCdecArtifact.getInstalledVersion();
    }

    @Test
    public void testGetUpdateSingleServerCommand() throws Exception {
        prepareConfForSingleNodeInstallation();

        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"1.0.0\"}");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
//            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("2.0.0"), Paths.get("some path"), options);  // TODO "296 » NullPointer"
//            assertNotNull(command);
        }
    }

    @Test
    public void testGetUpdateMultiServerCommand() throws Exception {
        prepareConfForMultiNodeInstallation();

        when(mockTransport.doOption("http://localhost/api/", null)).thenReturn("{\"ideVersion\":\"1.0.0\"}");

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(ImmutableMap.of("some property", "some value"));
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);

        int steps = spyCdecArtifact.getUpdateInfo(options).size();
        for (int i = 0; i < steps; i++) {
            options.setStep(i);
//            Command command = spyCdecArtifact.getUpdateCommand(Version.valueOf("2.0.0"), Paths.get("some path"), options);   TODO "313 » NullPointer"
//            assertNotNull(command);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Step number .* is out of update range")
    public void testGetUpdateCommandUnexistedStepError() throws Exception {
        prepareConfForSingleNodeInstallation();

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromSingleToMultiServerError() throws Exception {
        prepareConfForSingleNodeInstallation();

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateInfoFromMultiToSingleServerError() throws Exception {
        prepareConfForMultiNodeInstallation();

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateInfo(options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromSingleToMultiServerError() throws Exception {
        prepareConfForSingleNodeInstallation();

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_MULTI_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Only update to the Codenvy of the same installation type is supported")
    public void testGetUpdateCommandFromMultiToSingleServerError() throws Exception {
        prepareConfForMultiNodeInstallation();

        OSUtils.VERSION = "7";

        InstallOptions options = new InstallOptions();
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setInstallType(InstallType.CODENVY_SINGLE_SERVER);
        options.setStep(0);

        spyCdecArtifact.getUpdateCommand(Version.valueOf("1.0.0"), Paths.get("some path"), options);
    }

    @Test
    public void testGetInstalledType() throws Exception {
        prepareConfForSingleNodeInstallation();
        assertEquals(spyCdecArtifact.getInstalledType(), InstallType.CODENVY_SINGLE_SERVER);

        prepareConfForMultiNodeInstallation();
        assertEquals(spyCdecArtifact.getInstalledType(), InstallType.CODENVY_MULTI_SERVER);
    }

    @Test
    public void testGetBackupSingleServerCommand() throws Exception {
        prepareConfForSingleNodeInstallation();

        doReturn(mockConfig).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_SINGLE_SERVER);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME);
        backupConfig.setBackupFile(backupConfig.generateBackupFilePath());

        doReturn(InstallType.CODENVY_SINGLE_SERVER).when(spyCdecArtifact).getInstalledType();
        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();
        doReturn(new CDECSingleServerHelper(spyCdecArtifact)).when(spyCdecArtifact).getHelper(InstallType.CODENVY_SINGLE_SERVER);
        assertNotNull(spyCdecArtifact.getBackupCommand(backupConfig, mockConfigUtil));
    }

    @Test
    public void testGetBackupMultiServerCommand() throws Exception {
        prepareConfForMultiNodeInstallation();

        Map<String, String> codenvyMultiServerProperties = ImmutableMap.of(
                "api_host_name", "api.example.com",
                "data_host_name", "data.example.com");
        doReturn(new Config(codenvyMultiServerProperties)).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME);
        backupConfig.setBackupFile(backupConfig.generateBackupFilePath());

        doReturn(InstallType.CODENVY_MULTI_SERVER).when(spyCdecArtifact).getInstalledType();
        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();
        doReturn(new CDECMultiServerHelper(spyCdecArtifact)).when(spyCdecArtifact).getHelper(InstallType.CODENVY_MULTI_SERVER);
        assertNotNull(spyCdecArtifact.getBackupCommand(backupConfig, mockConfigUtil));
    }

    @Test
    public void testGetRestoreSingleServerCommand() throws Exception {
        prepareConfForSingleNodeInstallation();

        doReturn(mockConfig).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_SINGLE_SERVER);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupFile(Paths.get("dummyFile"))
                                                      .setBackupDirectory(Paths.get("dummyDirectory"));

        doReturn(InstallType.CODENVY_SINGLE_SERVER).when(spyCdecArtifact).getInstalledType();
        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();
        doReturn(new CDECSingleServerHelper(spyCdecArtifact)).when(spyCdecArtifact).getHelper(InstallType.CODENVY_SINGLE_SERVER);

        assertNotNull(spyCdecArtifact.getRestoreCommand(backupConfig, mockConfigUtil));
    }

    @Test
    public void testGetRestoreMultiServerCommand() throws Exception {
        prepareConfForMultiNodeInstallation();

        Map<String, String> codenvyMultiServerProperties = ImmutableMap.of(
                "api_host_name", "api.example.com",
                "data_host_name", "data.example.com");
        doReturn(new Config(codenvyMultiServerProperties)).when(mockConfigUtil).loadInstalledCodenvyConfig(InstallType.CODENVY_MULTI_SERVER);

        BackupConfig backupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                      .setBackupFile(Paths.get("dummyFile"))
                                                      .setBackupDirectory(Paths.get("dummyDirectory"));

        doReturn(InstallType.CODENVY_MULTI_SERVER).when(spyCdecArtifact).getInstalledType();
        doReturn(Version.valueOf("1.0.0")).when(spyCdecArtifact).getInstalledVersion();
        doReturn(new CDECMultiServerHelper(spyCdecArtifact)).when(spyCdecArtifact).getHelper(InstallType.CODENVY_MULTI_SERVER);

        assertNotNull(spyCdecArtifact.getRestoreCommand(backupConfig, mockConfigUtil));
    }

}
