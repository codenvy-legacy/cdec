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
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstaller extends BaseTest {

    @Mock
    private ConfigManager    configManager;
    private InstallerManager installerManager;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        installerManager = spy(new InstallerManager());
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(null).when(artifact).getInstalledVersion();

        List<String> info = installerManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installerManager).doGetInstallInfo(artifact, options);
    }

    @Test
    public void testGetInstallInfoInstalledSameVersion() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(version).when(artifact).getInstalledVersion();

        List<String> info = installerManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installerManager).doGetInstallInfo(artifact, options);
    }

    @Test
    public void testGetUpdateInfoInstalledLowerVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(Version.valueOf("0.9.1")).when(artifact).getInstalledVersion();

        List<String> info = installerManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installerManager).doGetUpdateInfo(artifact, options);
    }

    @Test
    public void testGetUpdateInfoInstalledHigherVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(Version.valueOf("1.0.1")).when(artifact).getInstalledVersion();

        List<String> info = installerManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installerManager).doGetUpdateInfo(artifact, options);
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
        doReturn(null).when(installerManager).executeCommand(any(Command.class));

        installerManager.install(artifact, version, pathToBinaries, options);
        verify(installerManager).executeCommand(any(Command.class));
        verify(installerManager).doInstall(artifact, version, pathToBinaries, options);
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
        doReturn(null).when(installerManager).executeCommand(any(Command.class));

        installerManager.install(artifact, version, pathToBinaries, options);
        verify(installerManager).executeCommand(any(Command.class));
        verify(installerManager).doInstall(artifact, version, pathToBinaries, options);
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
        doReturn(null).when(installerManager).executeCommand(any(Command.class));

        installerManager.install(artifact, version, pathToBinaries, options);
        verify(installerManager).executeCommand(any(Command.class));
        verify(installerManager).doUpdate(artifact, version, pathToBinaries, options);
    }

    @Test
    public void testIUpdateInstalledHigherVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        Path pathToBinaries = Paths.get("some path");

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);
        options.setConfigProperties(Collections.<String, String>emptyMap());
        options.setStep(1);

        doReturn(Version.valueOf("1.0.1")).when(artifact).getInstalledVersion();
        doReturn(null).when(installerManager).executeCommand(any(Command.class));

        installerManager.install(artifact, version, pathToBinaries, options);
        verify(installerManager).executeCommand(any(Command.class));
        verify(installerManager).doUpdate(artifact, version, pathToBinaries, options);
    }

    @Test
    public void testExecuteCommand() throws AgentException, CommandException {
        Command mockCommand = mock(Command.class);
        doReturn("test").when(mockCommand).execute();

        assertEquals(installerManager.executeCommand(mockCommand), "test");
    }
}
