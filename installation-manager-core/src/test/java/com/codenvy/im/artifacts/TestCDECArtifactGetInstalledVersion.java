/*
 *  2012-2016 Codenvy, S.A.
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
import com.codenvy.im.artifacts.helper.CDECArtifactHelper;
import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.UnknownInstallationTypeException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.IllegalVersionException;
import com.codenvy.im.utils.Version;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifactGetInstalledVersion extends BaseTest {
    public static final Version TEST_VERSION = Version.valueOf(TEST_VERSION_STR);
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    private CDECArtifact       spyCdecArtifact;
    @Mock
    private HttpTransport      transport;
    @Mock
    private ConfigManager      configManager;
    @Mock
    private CDECArtifactHelper mockHelper;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact(UPDATE_API_ENDPOINT, DOWNLOAD_DIR, ASSEMBLY_PROPERTIES, transport, configManager));
    }

    @Test
    public void fetchAssemblyVersionFromSingleNode() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        assertEquals(spyCdecArtifact.fetchAssemblyVersion(), Optional.of(TEST_VERSION));
    }

    @Test
    public void fetchAssemblyVersionFromMultiNode() throws Exception {
        prepareMultiNodeEnv(configManager, transport);

        Command readAssemblyPropertiesCommand = spyCdecArtifact.getReadAssemblyPropertiesCommand();
        assertEquals(readAssemblyPropertiesCommand.toString(), format("{'command'='if  test -f target/assembly.properties; then" +
                                                                      "     cat target/assembly.properties" +
                                                                      "        | grep assembly.version" +
                                                                      "        | sed 's/assembly.version\\s*=\\s*\\(.*\\)/\\1/';fi', " +
                                                                      "'agent'='{'host'='api.example.com', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}",
                                                                      SYSTEM_USER_NAME));
    }

    @Test
    public void getVersionFromApiService() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        assertEquals(spyCdecArtifact.getVersionFromApiService(), Optional.of(TEST_VERSION));
    }

    @Test
    public void fetchVersionFromPuppetConfig() throws Exception {
        prepareSingleNodeEnv(configManager, transport);

        assertEquals(spyCdecArtifact.fetchVersionFromPuppetConfig(), Optional.of(TEST_VERSION));
    }

    @Test
    public void getInstalledVersionEmpty() throws Exception {
        doReturn(true).when(spyCdecArtifact).isApiServiceAlive();

        doReturn(Optional.empty()).when(spyCdecArtifact).getVersionFromApiService();
        doReturn(Optional.empty()).when(spyCdecArtifact).fetchVersionFromPuppetConfig();
        doReturn(Optional.empty()).when(spyCdecArtifact).fetchAssemblyVersion();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());

        // check empty result when IOException thrown
        doThrow(IOException.class).when(spyCdecArtifact).fetchVersionFromPuppetConfig();
        doThrow(IOException.class).when(spyCdecArtifact).getVersionFromApiService();
        doThrow(IOException.class).when(spyCdecArtifact).fetchAssemblyVersion();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());

        // check empty result when UnknownInstallationTypeException thrown
        doThrow(UnknownInstallationTypeException.class).when(spyCdecArtifact).fetchVersionFromPuppetConfig();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());
        doThrow(UnknownInstallationTypeException.class).when(spyCdecArtifact).getVersionFromApiService();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());
        doThrow(UnknownInstallationTypeException.class).when(spyCdecArtifact).fetchAssemblyVersion();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());

        // check empty result when IllegalVersionException thrown
        doThrow(IllegalVersionException.class).when(spyCdecArtifact).fetchVersionFromPuppetConfig();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());
        doThrow(IllegalVersionException.class).when(spyCdecArtifact).getVersionFromApiService();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());
        doThrow(IllegalVersionException.class).when(spyCdecArtifact).fetchAssemblyVersion();
        assertFalse(spyCdecArtifact.getInstalledVersion().isPresent());

    }

    @Test
    public void getInstalledVersionUsingFetchAssemblyVersion() throws Exception {
        doReturn(true).when(spyCdecArtifact).isApiServiceAlive();

        doReturn(Optional.of(TEST_VERSION)).when(spyCdecArtifact).fetchAssemblyVersion();
        assertEquals(spyCdecArtifact.getInstalledVersion().get(), TEST_VERSION);
    }

    @Test
    public void getInstalledVersionUsingGetVersionFromApiService() throws Exception {
        doReturn(true).when(spyCdecArtifact).isApiServiceAlive();
        doReturn(Optional.empty()).when(spyCdecArtifact).fetchAssemblyVersion();

        doReturn(Optional.of(TEST_VERSION)).when(spyCdecArtifact).getVersionFromApiService();
        assertEquals(spyCdecArtifact.getInstalledVersion().get(), TEST_VERSION);
    }

    @Test
    public void getInstalledVersionUsingFetchVersionFromPuppetConfig() throws Exception {
        doReturn(true).when(spyCdecArtifact).isApiServiceAlive();
        doReturn(Optional.empty()).when(spyCdecArtifact).getVersionFromApiService();
        doReturn(Optional.empty()).when(spyCdecArtifact).fetchAssemblyVersion();

        doReturn(Optional.of(TEST_VERSION)).when(spyCdecArtifact).fetchVersionFromPuppetConfig();
        assertEquals(spyCdecArtifact.getInstalledVersion().get(), TEST_VERSION);
    }

}
