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
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Matchers.any;
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

        installManagerArtifact = spy(new InstallManagerArtifact(UPDATE_API_ENDPOINT, transport, configManager));
        cdecArtifact = spy(new CDECArtifact(UPDATE_API_ENDPOINT, transport, configManager));

        installManager = spy(new InstallManager(new HashSet<>(Arrays.asList(installManagerArtifact, cdecArtifact))));
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(null).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installManager).doGetInstallInfo(artifact, options);
    }

    @Test
    public void testGetInstallInfoInstalledSameVersion() throws Exception {
        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(version).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installManager).doGetInstallInfo(artifact, options);
    }

    @Test
    public void testGetUpdateInfoInstalledLowerVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(Version.valueOf("0.9.1")).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installManager).doGetUpdateInfo(artifact, options);
    }

    @Test
    public void testGetUpdateInfoInstalledHigherVersion() throws Exception {
        createSingleNodeConf();

        Version version = Version.valueOf("1.0.0");
        Artifact artifact = spy(createArtifact(CDECArtifact.NAME));
        InstallOptions options = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);

        doReturn(Version.valueOf("1.0.1")).when(artifact).getInstalledVersion();

        List<String> info = installManager.getInstallInfo(artifact, version, options);
        assertTrue(info.size() > 0);
        verify(installManager).doGetUpdateInfo(artifact, options);
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

        installManager.install(artifact, version, pathToBinaries, options);
        verify(installManager).executeCommand(any(Command.class));
        verify(installManager).doInstall(artifact, version, pathToBinaries, options);
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

        installManager.install(artifact, version, pathToBinaries, options);
        verify(installManager).executeCommand(any(Command.class));
        verify(installManager).doInstall(artifact, version, pathToBinaries, options);
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

        installManager.install(artifact, version, pathToBinaries, options);
        verify(installManager).executeCommand(any(Command.class));
        verify(installManager).doUpdate(artifact, version, pathToBinaries, options);
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
        doReturn(null).when(installManager).executeCommand(any(Command.class));

        installManager.install(artifact, version, pathToBinaries, options);
        verify(installManager).executeCommand(any(Command.class));
        verify(installManager).doUpdate(artifact, version, pathToBinaries, options);
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

//    @Test(expectedExceptions = IllegalStateException.class,
//            expectedExceptionsMessageRegExp = "Can not install the artifact '" + InstallManagerArtifact.NAME + "' version '2.10.1'.")
//    public void testReInstallAlreadyInstalledArtifact() throws Exception {
//        final Version version2101 = Version.valueOf("2.10.1");
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(installManagerArtifact, new TreeMap<Version, Path>() {{
//                put(version2101, Paths.get("target/download/" + InstallManagerArtifact.NAME + "/2.10.1/file1"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//
//        doReturn(false).when(installManagerArtifact).isInstallable(version2101);
//
//        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();
//
//        manager.install(installManagerArtifact, version2101, new InstallOptions());
//    }
//
//    @Test
//    public void testInstallArtifact() throws Exception {
//        final Version version100 = Version.valueOf("1.0.0");
//        final InstallOptions options = new InstallOptions();
//        final Path pathToBinaries = Paths.get("some path");
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(cdecArtifact, new TreeMap<Version, Path>() {{
//                put(version100, pathToBinaries);
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//        doReturn(true).when(cdecArtifact).isInstallable(version100);
//
//        manager.install(cdecArtifact, version100, options);
//
//        verify(installManager).install(cdecArtifact, version100, pathToBinaries, options);
//    }
//
//    @Test(expectedExceptions = FileNotFoundException.class,
//            expectedExceptionsMessageRegExp = "Binaries to install artifact '" + InstallManagerArtifact.NAME + "' version '2.10.1' not found")
//    public void testNotInstallableUpdate() throws Exception {
//        final Version version200 = Version.valueOf("2.0.0");
//
//        doReturn(new TreeMap<Version, Path>() {{
//            put(version200, null);
//        }}).when(installManagerArtifact).getDownloadedVersions(any(Path.class));
//
//        manager.install(installManagerArtifact, Version.valueOf("2.10.1"), new InstallOptions());
//        doReturn(false).when(installManagerArtifact).isInstallable(version200);
//
//        manager.install(installManagerArtifact, version200, null);
//    }
//
//    @Test(expectedExceptions = FileNotFoundException.class)
//    public void testInstallArtifactErrorIfBinariesNotFound() throws Exception {
//        doReturn(null).when(cdecArtifact).getInstalledVersion();
//        manager.install(cdecArtifact, Version.valueOf("2.10.1"), new InstallOptions());
//    }
//
//    @Test(expectedExceptions = IllegalStateException.class,
//            expectedExceptionsMessageRegExp = "Can not install the artifact '" + InstallManagerArtifact.NAME + "' version '2.10.0'.")
//    public void testUpdateIMErrorIfInstalledIMHasGreaterVersion() throws Exception {
//        final Version version2100 = Version.valueOf("2.10.0");
//        Version version2101 = Version.valueOf("2.10.1");
//
//        when(transport.doOption(endsWith("api/"), anyString())).thenReturn("{\"ideVersion\":\"1.0.0\"}");
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(installManagerArtifact, new TreeMap<Version, Path>() {{
//                put(version2100, Paths.get("target/download/" + InstallManagerArtifact.NAME + "/2.10.0/file1"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();
//
//        doReturn(false).when(installManagerArtifact).isInstallable(version2100);
//
//        manager.install(installManagerArtifact, version2100, new InstallOptions());
//    }
//
//    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Can not install the artifact 'codenvy' version '1
// .0" +
//                                                                                              ".0'.")
//    public void testInstallZeroInstallationStep() throws Exception {
//        final Version version100 = Version.valueOf("1.0.0");
//        InstallOptions options = new InstallOptions();
//        options.setInstallType(InstallType.SINGLE_SERVER);
//        options.setStep(0);
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(cdecArtifact, new TreeMap<Version, Path>() {{
//                put(version100, Paths.get("some path"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//        doReturn(false).when(cdecArtifact).isInstallable(version100);
//
//        manager.install(cdecArtifact, version100, options);
//    }
//
//    @Test
//    public void testInstallNonZeroInstallationStep() throws Exception {
//        final Version version100 = Version.valueOf("1.0.0");
//        InstallOptions options = new InstallOptions();
//        options.setInstallType(InstallType.SINGLE_SERVER);
//        options.setStep(1);
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(cdecArtifact, new TreeMap<Version, Path>() {{
//                put(version100, Paths.get("some path"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//        doReturn(false).when(cdecArtifact).isInstallable(version100);
//
//        manager.install(cdecArtifact, version100, options);
//
//        verify(installManager).install(cdecArtifact, version100, Paths.get("some path"), options);
//    }
//
//    @Test
//    public void testInstallWithInstallOptions() throws Exception {
//        final Version version100 = Version.valueOf("1.0.0");
//        final InstallOptions testOptions = new InstallOptions();
//        testOptions.setInstallType(InstallType.SINGLE_SERVER);
//        testOptions.setStep(1);
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(cdecArtifact, new TreeMap<Version, Path>() {{
//                put(version100, Paths.get("some path"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//        doReturn(true).when(cdecArtifact).isInstallable(version100);
//
//        when(transport.doOption(endsWith("api/"), anyString())).thenReturn("{\"ideVersion\":\"3.0.0\"}");
//
//        manager.install(cdecArtifact, version100, testOptions);
//
//        verify(installManager).install(cdecArtifact, version100, Paths.get("some path"), testOptions);
//    }
//
//    @Test
//    public void testInstallArtifactNewlyArtifact() throws Exception {
//        final Version version2102 = Version.valueOf("2.10.2");
//        final Version version2101 = Version.valueOf("2.10.1");
//
//        doReturn(new HashMap<Artifact, SortedMap<Version, Path>>() {{
//            put(installManagerArtifact, new TreeMap<Version, Path>() {{
//                put(version2102, Paths.get("target/download/installation-manager/2.10.2/file1"));
//            }});
//        }}).when(downloadManager).getDownloadedArtifacts();
//
//        doReturn(version2101).when(installManagerArtifact).getInstalledVersion();
//
//        doReturn(true).when(installManagerArtifact).isInstallable(version2102);
//
//        doNothing().when(installManager).install(any(Artifact.class), any(Version.class), any(Path.class), any(InstallOptions.class));
//
//        manager.install(installManagerArtifact, version2102, new InstallOptions());
//    }

}
