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
package com.codenvy.im.facade;

import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.ArtifactStatus;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactStatus;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.utils.Commons.toJson;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerFacade extends BaseTest {
    private InstallationManagerFacade installationManagerFacade;
    private Artifact                  cdecArtifact;

    @Mock
    private HttpTransport           transport;
    @Mock
    private SaasAuthServiceProxy    saasAuthServiceProxy;
    @Mock
    private SaasAccountServiceProxy saasAccountServiceProxy;
    @Mock
    private PasswordManager         passwordManager;
    @Mock
    private NodeManager             nodeManager;
    @Mock
    private BackupManager           backupManager;
    @Mock
    private StorageManager          storageManager;
    @Mock
    private InstallManager          installManager;
    @Mock
    private DownloadManager         downloadManager;
    @Mock
    private ConfigManager           configManager;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
        installationManagerFacade = spy(new InstallationManagerFacade("target/download",
                                                                      "update/endpoint",
                                                                      transport,
                                                                      saasAuthServiceProxy,
                                                                      saasAccountServiceProxy,
                                                                      passwordManager,
                                                                      nodeManager,
                                                                      backupManager,
                                                                      storageManager,
                                                                      installManager,
                                                                      downloadManager));
    }

    @Test
    public void testStartDownload() throws Exception {
        Version version = Version.valueOf("1.0.1");

        installationManagerFacade.startDownload(cdecArtifact, version);

        verify(downloadManager).startDownload(cdecArtifact, version);
    }

    @Test
    public void testStopDownload() throws Exception {
        installationManagerFacade.stopDownload();

        verify(downloadManager).stopDownload();
    }

    @Test
    public void testGetDownloadStatus() throws Exception {
        installationManagerFacade.getDownloadProgress();

        verify(downloadManager).getDownloadProgress();
    }

    @Test
    public void testInstall() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        final Version version = Version.valueOf("2.10.5");
        final Path pathToBinaries = Paths.get("file");

        doReturn(new TreeMap<Version, Path>() {{
            put(version, pathToBinaries);
        }}).when(downloadManager).getDownloadedVersions(cdecArtifact);

        installationManagerFacade.install(cdecArtifact, version, installOptions);

        verify(installManager).performInstallStep(cdecArtifact, version, pathToBinaries, installOptions);
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testInstallError() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        Version version = Version.valueOf("1.0.1");

        doThrow(FileNotFoundException.class).when(downloadManager).getDownloadedVersions(cdecArtifact);

        installationManagerFacade.install(cdecArtifact, version, installOptions);
    }

    @Test
    public void testUpdate() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        final Version version = Version.valueOf("2.10.5");
        final Path pathToBinaries = Paths.get("file");

        doReturn(new TreeMap<Version, Path>() {{
            put(version, pathToBinaries);
        }}).when(downloadManager).getDownloadedVersions(cdecArtifact);

        installationManagerFacade.update(cdecArtifact, version, installOptions);

        verify(installManager).performUpdateStep(cdecArtifact, version, pathToBinaries, installOptions);
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void testUpdateError() throws Exception {
        InstallOptions installOptions = new InstallOptions();
        Version version = Version.valueOf("1.0.1");

        doThrow(FileNotFoundException.class).when(downloadManager).getDownloadedVersions(cdecArtifact);

        installationManagerFacade.update(cdecArtifact, version, installOptions);
    }

    @Test
    public void testAddNode() throws IOException {
        doReturn(new NodeConfig(NodeConfig.NodeType.BUILDER, "builder.node.com", null)).when(nodeManager).add("builder.node.com");
        assertEquals(toJson(installationManagerFacade.addNode("builder.node.com")), "{\n" +
                                                                                    "  \"type\" : \"BUILDER\",\n" +
                                                                                    "  \"host\" : \"builder.node.com\"\n" +
                                                                                    "}");
    }


    @Test(expectedExceptions = IOException.class)
    public void testAddNodeException() throws IOException {
        doThrow(new IOException("error")).when(nodeManager).add("builder.node.com");

        installationManagerFacade.addNode("builder.node.com");
    }

    @Test
    public void testRemoveNode() throws IOException {
        final String TEST_NODE_DNS = "builder.node.com";
        final NodeConfig TEST_NODE = new NodeConfig(NodeConfig.NodeType.BUILDER, TEST_NODE_DNS, null);
        doReturn(TEST_NODE).when(nodeManager).remove(TEST_NODE_DNS);

        assertEquals(toJson(installationManagerFacade.removeNode(TEST_NODE_DNS)), "{\n" +
                                                                                  "  \"type\" : \"BUILDER\",\n" +
                                                                                  "  \"host\" : \"builder.node.com\"\n" +
                                                                                  "}");
    }

    @Test(expectedExceptions = IOException.class)
    public void testRemoveNodeException() throws IOException {
        final String TEST_NODE_DNS = "builder.node.com";
        doThrow(new IOException("error")).when(nodeManager).remove(TEST_NODE_DNS);

        installationManagerFacade.removeNode(TEST_NODE_DNS);
    }

    @Test
    public void testBackup() throws IOException {
        Path testBackupDirectory = Paths.get("test/backup/directory");
        Path testBackupFile = testBackupDirectory.resolve("backup.tar.gz");
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupDirectory(testBackupDirectory.toString());

        doReturn(testBackupConfig.setBackupFile(testBackupFile.toString()).setArtifactVersion("1.0.0"))
                .when(backupManager).backup(testBackupConfig);

        assertEquals(toJson(installationManagerFacade.backup(testBackupConfig)), "{\n" +
                                                                                 "  \"file\" : \"test/backup/directory/backup.tar.gz\",\n" +
                                                                                 "  \"artifact\" : \"codenvy\",\n" +
                                                                                 "  \"version\" : \"1.0.0\"\n" +
                                                                                 "}");
    }


    @Test(expectedExceptions = IOException.class)
    public void testBackupException() throws IOException {
        String testBackupDirectory = "test/backup/directory";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupDirectory(testBackupDirectory);

        doThrow(new IOException("error")).when(backupManager).backup(testBackupConfig);

        installationManagerFacade.backup(testBackupConfig);
    }

    @Test
    public void testRestore() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        assertEquals(toJson(installationManagerFacade.restore(testBackupConfig)), "{\n" +
                                                                                  "  \"file\" : \"test/backup/directory/backup.tar.gz\",\n" +
                                                                                  "  \"artifact\" : \"codenvy\"\n" +
                                                                                  "}");
    }


    @Test(expectedExceptions = IOException.class)
    public void testRestoreException() throws IOException {
        String testBackupFile = "test/backup/directory/backup.tar.gz";
        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                          .setBackupFile(testBackupFile);

        doThrow(new IOException("error")).when(backupManager).restore(testBackupConfig);

        installationManagerFacade.restore(testBackupConfig);
    }

    @Test
    public void testLoginToSaas() throws Exception {
        Credentials credentials = new DtoServerImpls.CredentialsImpl();

        installationManagerFacade.loginToCodenvySaaS(credentials);

        verify(saasAuthServiceProxy).login(credentials);
    }

    @Test
    public void testLoginFromSaas() throws Exception {
        installationManagerFacade.logoutFromCodenvySaaS("token");

        verify(saasAuthServiceProxy).logout("token");
    }

    @Test
    public void testLoginToSaasWhenTokenNull() throws Exception {
        final String TEST_USER_NAME = "user";
        final String TEST_USER_PASSWORD = "password";
        final String TEST_CREDENTIALS_JSON = "{\n"
                                             + "  \"username\": \"" + TEST_USER_NAME + "\",\n"
                                             + "  \"password\": \"" + TEST_USER_PASSWORD + "\"\n"
                                             + "}";

        Credentials testSaasUsernameAndPassword = Commons.createDtoFromJson(TEST_CREDENTIALS_JSON, Credentials.class);

        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", TEST_USER_NAME,
                                                              "password", TEST_USER_PASSWORD));

        doReturn(null).when(transport).doPost("api/endpoint/auth/login", body);

        Token token = installationManagerFacade.loginToCodenvySaaS(testSaasUsernameAndPassword);
        assertNull(token);
    }

    @Test
    public void testChangeAdminPassword() throws Exception {
        byte[] curPwd = "curPassword".getBytes("UTF-8");
        byte[] newPwd = "newPassword".getBytes("UTF-8");

        installationManagerFacade.changeAdminPassword(curPwd, newPwd);
        verify(passwordManager).changeAdminPassword(curPwd, newPwd);
    }

    @Test
    public void testStoreProperties() throws Exception {
        Map<String, String> properties = ImmutableMap.of("x", "y");

        installationManagerFacade.storeStorageProperties(properties);

        verify(storageManager).storeProperties(properties);
    }

    @Test
    public void testLoadProperties() throws Exception {
        installationManagerFacade.loadStorageProperties();
        verify(storageManager).loadProperties();
    }

    @Test
    public void testLoadProperty() throws Exception {
        String key = "x";

        installationManagerFacade.loadStorageProperty(key);
        verify(storageManager).loadProperty(key);
    }

    @Test
    public void testStoreProperty() throws Exception {
        String key = "x";
        String value = "y";

        installationManagerFacade.storeStorageProperty(key, value);
        verify(storageManager).storeProperty(key, value);
    }

    @Test
    public void testDeleteProperty() throws Exception {
        String key = "x";

        installationManagerFacade.deleteStorageProperty(key);
        verify(storageManager).deleteProperty(key);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn("update/endpoint").when(installationManagerFacade).extractServerUrl("update/endpoint");
        Map<String, String> m = installationManagerFacade.getInstallationManagerProperties();
        assertEquals(m.size(), 2);
        assertTrue(m.containsValue("target/download"));
        assertTrue(m.containsValue("update/endpoint"));
    }


    @Test
    public void updateArtifactConfig() throws IOException {
        Map<String, String> properties = ImmutableMap.of("a", "b");
        doNothing().when(installationManagerFacade).doUpdateArtifactConfig(cdecArtifact, properties);

        installationManagerFacade.updateArtifactConfig(cdecArtifact, properties);

        verify(installationManagerFacade).doUpdateArtifactConfig(cdecArtifact, properties);
    }

    @Test(expectedExceptions = IOException.class)
    public void updateArtifactConfigWhenError() throws Exception {
        Map<String, String> properties = ImmutableMap.of("a", "b");
        prepareSingleNodeEnv(configManager);
        doThrow(IOException.class).when(installationManagerFacade).doUpdateArtifactConfig(cdecArtifact, properties);

        installationManagerFacade.updateArtifactConfig(cdecArtifact, properties);
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        when(downloadManager.getUpdates()).thenReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(cdecArtifact, version100);
            }
        });

        when(downloadManager.getDownloadedVersions(cdecArtifact)).thenReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }});

        Collection<UpdatesArtifactInfo> updates = installationManagerFacade.getUpdates();
        assertEquals(updates.size(), 1);

        UpdatesArtifactInfo info = updates.iterator().next();
        assertEquals(info.getStatus(), UpdatesArtifactStatus.DOWNLOADED);
        assertEquals(info.getArtifact(), cdecArtifact.getName());
        assertEquals(info.getVersion(), version100.toString());
    }

    @Test
    public void testAllGetUpdates() throws Exception {
        doReturn(new ArrayList<Map.Entry<Artifact, Version>>() {{
            add(new AbstractMap.SimpleEntry<>(cdecArtifact, Version.valueOf("1.0.1")));
            add(new AbstractMap.SimpleEntry<>(cdecArtifact, Version.valueOf("1.0.2")));
        }}).when(downloadManager).getAllUpdates(cdecArtifact);

        doReturn(new TreeMap<Version, Path>() {{
            put(Version.valueOf("1.0.1"), Paths.get("file1"));
        }}).when(downloadManager).getDownloadedVersions(cdecArtifact);

        Collection<UpdatesArtifactInfo> updates = installationManagerFacade.getAllUpdates(cdecArtifact);
        assertEquals(updates.size(), 2);

        Iterator<UpdatesArtifactInfo> iter = updates.iterator();

        UpdatesArtifactInfo info = iter.next();
        assertEquals(info.getVersion(), "1.0.2");
        assertEquals(info.getArtifact(), cdecArtifact.getName());
        assertEquals(info.getStatus(), UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);

        info = iter.next();
        assertEquals(info.getVersion(), "1.0.1");
        assertEquals(info.getArtifact(), cdecArtifact.getName());
        assertEquals(info.getStatus(), UpdatesArtifactStatus.DOWNLOADED);
    }


    @Test
    public void testHasValidSaaSSubscription() throws Exception {
        SaasUserCredentials saasUserCredentials = new SaasUserCredentials();
        saasUserCredentials.setAccountId("id");
        saasUserCredentials.setToken("token");

        installationManagerFacade.hasValidSaasSubscription("OnPremises", saasUserCredentials);

        verify(saasAccountServiceProxy).hasValidSubscription("OnPremises", "token", "id");
    }

    @Test
    public void testGetSubscription() throws Exception {
        SaasUserCredentials saasUserCredentials = new SaasUserCredentials();
        saasUserCredentials.setAccountId("id");
        saasUserCredentials.setToken("token");

        installationManagerFacade.getSaasSubscription("OnPremises", saasUserCredentials);

        verify(saasAccountServiceProxy).getSubscription("OnPremises", "token", "id");
    }

    @Test
    public void testGetArtifacts() throws Exception {
        doReturn(new HashMap<Artifact, Version>() {{
            put(cdecArtifact, Version.valueOf("1.0.1"));
        }}).when(installManager).getInstalledArtifacts();

        doReturn(new LinkedHashMap<Artifact, SortedMap<Version, Path>>() {{
            put(cdecArtifact, new TreeMap<Version, Path>() {{
                put(Version.valueOf("1.0.0"), Paths.get("target/file1"));
                put(Version.valueOf("1.0.1"), Paths.get("target/file2"));
                put(Version.valueOf("1.0.2"), Paths.get("target/file3"));
            }});
        }}).when(downloadManager).getDownloadedArtifacts();

        doReturn(true).when(installManager).isInstallable(cdecArtifact, Version.valueOf("1.0.2"));

        Collection<ArtifactInfo> artifacts = installationManagerFacade.getArtifacts();

        assertEquals(artifacts.size(), 3);

        Iterator<ArtifactInfo> iterator = artifacts.iterator();
        ArtifactInfo info = iterator.next();
        assertEquals(info.getArtifact(), "codenvy");
        assertEquals(info.getVersion(), "1.0.2");
        assertEquals(info.getStatus(), ArtifactStatus.READY_TO_INSTALL);

        info = iterator.next();
        assertEquals(info.getArtifact(), "codenvy");
        assertEquals(info.getVersion(), "1.0.1");
        assertEquals(info.getStatus(), ArtifactStatus.INSTALLED);

        info = iterator.next();
        assertEquals(info.getArtifact(), "codenvy");
        assertEquals(info.getVersion(), "1.0.0");
        assertEquals(info.getStatus(), ArtifactStatus.DOWNLOADED);
    }

    @Test
    public void testWaitForInstallStepCompleted() throws Exception {
        installationManagerFacade.waitForInstallStepCompleted("id");

        verify(installManager).waitForStepCompleted("id");
    }

    @Test
    public void testGetInstallStepInfo() throws Exception {
        installationManagerFacade.getUpdateStepInfo("id");

        verify(installManager).getUpdateStepInfo("id");
    }
}
