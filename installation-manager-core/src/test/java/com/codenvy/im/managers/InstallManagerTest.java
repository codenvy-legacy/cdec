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
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.InstallArtifactStepInfo;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class InstallManagerTest extends BaseTest {

    @Mock
    private ConfigManager configManager;
    @Mock
    private HttpTransport transport;

    private Artifact       cdecArtifact;
    private Artifact       installManagerArtifact;
    private InstallManager installManager;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        installManagerArtifact = spy(new InstallManagerArtifact(UPDATE_API_ENDPOINT, DOWNLOAD_DIR, transport, configManager));
        cdecArtifact = spy(new CDECArtifact(UPDATE_API_ENDPOINT, DOWNLOAD_DIR, ASSEMBLY_PROPERTIES, transport, configManager));

        installManager = spy(new InstallManager(new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact))));
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        prepareSingleNodeEnv(configManager);
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));

        doReturn(null).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, InstallType.SINGLE_SERVER);
        assertTrue(info.size() > 0);
        verify(installManager).getInstallInfo(artifact, InstallType.SINGLE_SERVER);
    }

    @Test
    public void testGetInstallInfoInstalledSameVersion() throws Exception {
        prepareSingleNodeEnv(configManager);
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));

        doReturn(version).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, InstallType.SINGLE_SERVER);

        assertTrue(info.size() > 0);
        verify(installManager).getInstallInfo(artifact, InstallType.SINGLE_SERVER);
    }

    @Test
    public void testGetUpdateInfoInstalledLowerVersion() throws Exception {
        createSingleNodeConf();

        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        doReturn(Version.valueOf("0.9.1")).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, InstallType.SINGLE_SERVER);

        assertTrue(info.size() > 0);
        verify(installManager).getInstallInfo(artifact, InstallType.SINGLE_SERVER);
    }

    @Test
    public void testGetUpdateInfoInstalledHigherVersion() throws Exception {
        createSingleNodeConf();

        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        doReturn(Version.valueOf("1.0.1")).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, InstallType.SINGLE_SERVER);

        assertTrue(info.size() > 0);
        verify(installManager).getInstallInfo(artifact, InstallType.SINGLE_SERVER);
    }

    @Test
    public void testInstall() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setStep(1);

        doReturn(null).when(artifact).getInstalledVersion();
        doReturn(null).when(installManager).executeCommand(any(Command.class));
        doReturn(true).when(installManager).isInstallable(artifact, version);

        String stepId = installManager.performInstallStep(artifact, version, pathToBinaries, options);
        installManager.waitForStepCompleted(stepId);

        verify(installManager).executeCommand(any(Command.class));
    }

    @Test
    public void testInstallInstalledSameVersion() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setStep(1);

        doReturn(version).when(artifact).getInstalledVersion();
        doReturn(null).when(installManager).executeCommand(any(Command.class));
        doReturn(true).when(installManager).isInstallable(artifact, version);

        String stepId = installManager.performInstallStep(artifact, version, pathToBinaries, options);
        installManager.waitForStepCompleted(stepId);

        verify(installManager).executeCommand(any(Command.class));
    }

    @Test
    public void testUpdateInstalledLowerVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setStep(1);

        doReturn(Version.valueOf("0.9.1")).when(artifact).getInstalledVersion();
        doReturn(null).when(installManager).executeCommand(any(Command.class));
        doReturn(true).when(installManager).isInstallable(artifact, version);

        String stepId = installManager.performInstallStep(artifact, version, pathToBinaries, options);
        installManager.waitForStepCompleted(stepId);

        verify(installManager).executeCommand(any(Command.class));
    }

    @Test
    public void testUpdateInstalledHigherVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setStep(1);

        doReturn(Version.valueOf("1.0.1")).when(artifact).getInstalledVersion();
        doReturn(null).when(installManager).executeCommand(any(Command.class));
        doReturn(true).when(installManager).isInstallable(artifact, version);

        String stepId = installManager.performInstallStep(artifact, version, pathToBinaries, options);
        installManager.waitForStepCompleted(stepId);

        verify(installManager).executeCommand(any(Command.class));
    }

    @Test
    public void testExecuteCommand() throws AgentException, CommandException {
        Command mockCommand = mock(Command.class);
        doReturn("test").when(mockCommand).execute();

        assertEquals(installManager.executeCommand(mockCommand), "test");
    }

    @Test
    public void testGetInstalledArtifacts() throws Exception {
        Version version100 = Version.valueOf("1.0.0");
        Version version200 = Version.valueOf("2.0.0");

        doReturn(version100).when(cdecArtifact).getInstalledVersion();
        doReturn(version200).when(installManagerArtifact).getInstalledVersion();

        Map<Artifact, Version> installedArtifacts = installManager.getInstalledArtifacts();

        assertEquals(installedArtifacts.size(), 2);
        assertEquals(installedArtifacts.toString(), "{codenvy=1.0.0, " + InstallManagerArtifact.NAME + "=2.0.0}");
    }

    @Test
    public void testIsInstallable() throws IOException {
        final Version version200 = Version.valueOf("2.0.0");
        final Version version201 = Version.valueOf("2.0.1");

        doReturn(true).when(installManagerArtifact).isInstallable(version200);
        assertTrue(installManager.isInstallable(installManagerArtifact, version200));

        doReturn(false).when(installManagerArtifact).isInstallable(version201);
        assertFalse(installManager.isInstallable(installManagerArtifact, version201));
    }

    @Test
    public void testWaitForInstallStepCompleted() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1);

        Version version = Version.valueOf("1.0.0");
        Path pathToBinaries = Paths.get("some path");
        final CountDownLatch latch = new CountDownLatch(1);
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));

        Command command = mock(Command.class);
        doReturn(command).when(artifact).getInstallCommand(any(Version.class), any(Path.class), any(InstallOptions.class));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.await();
                return null;
            }
        }).when(installManager).executeCommand(any(Command.class));

        String stepId = installManager.performInstallStep(artifact, version, pathToBinaries, options);

        InstallArtifactStepInfo info = installManager.getUpdateStepInfo(stepId);
        assertEquals(info.getStatus(), InstallArtifactStatus.IN_PROGRESS);

        latch.countDown();

        installManager.waitForStepCompleted(stepId);

        assertEquals(info.getStatus(), InstallArtifactStatus.SUCCESS);
        assertEquals(info.getStep(), 1);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "codenvy:1.0.0 is not installable")
    public void testReInstallAlreadyInstalledArtifact() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setStep(1);

        doReturn(false).when(installManager).isInstallable(artifact, version);

        installManager.performInstallStep(artifact, version, pathToBinaries, options);
    }
}
