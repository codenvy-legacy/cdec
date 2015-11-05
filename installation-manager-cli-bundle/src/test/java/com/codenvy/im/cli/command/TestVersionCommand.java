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
package com.codenvy.im.cli.command;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.VersionLabel;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactStatus;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 */
public class TestVersionCommand extends AbstractTestCommand {
    private VersionCommand spyCommand;

    private IMArtifactLabeledFacade facade;
    private CommandSession          commandSession;

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;

    PrintStream originOut = System.out;
    PrintStream originErr = System.err;

    @Mock
    private PreferencesStorage mockPreferencesStorage;
    @Mock
    private ConfigManager      mockConfigManager;

    @BeforeMethod
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(new HashMap<>(ImmutableMap.of("a", "MANDATORY"))).when(mockConfigManager).loadCodenvyDefaultProperties(Version.valueOf("1.0.1"),
                                                                                                                        InstallType.SINGLE_SERVER);
        doReturn(new Config(new HashMap<>(ImmutableMap.of("a", "MANDATORY")))).when(mockConfigManager)
                                                                              .loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);

        facade = mock(IMArtifactLabeledFacade.class);
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new VersionCommand());
        spyCommand.facade = facade;
        spyCommand.preferencesStorage = mockPreferencesStorage;

        performBaseMocks(spyCommand, true);

        doNothing().when(spyCommand).updateImCliClientIfNeeded();
    }

    @BeforeMethod
    public void initStreams() {
        this.outputStream = new ByteArrayOutputStream();
        this.errorStream = new ByteArrayOutputStream();

        System.setOut(new PrintStream(this.outputStream));
        System.setErr(new PrintStream(this.errorStream));
    }

    @AfterMethod
    public void restoreSystemStreams() {
        System.setOut(originOut);
        System.setErr(originErr);
    }

    @Test
    public void shouldBePrintAvailableStableVersion() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();
        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);
        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.1");
        updatesArtifactInfo.setLabel(VersionLabel.STABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"stable\" : \"1.0.1\"\n" +
                             "  }\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintStatusLatestStableVersionInstalled() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        doReturn(new ArrayList<UpdatesArtifactInfo>()).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.1\",\n" +
                             "  \"label\" : \"STABLE\",\n" +
                             "  \"status\" : \"You are running the latest stable version of Codenvy!\"\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintAvailableStableVersionAndStatusNewStableVersionAvailable() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.2");
        updatesArtifactInfo.setLabel(VersionLabel.STABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.1\",\n" +
                             "  \"label\" : \"STABLE\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"stable\" : \"1.0.2\"\n" +
                             "  },\n" +
                             "  \"status\" : \"There is a new stable version of Codenvy available. Run im-download 1.0.2.\"\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintAvailableStableVersionAndStatusNewStableVersionAvailableAndInstallIt() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.2");
        updatesArtifactInfo.setLabel(VersionLabel.STABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.DOWNLOADED);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.1\",\n" +
                             "  \"label\" : \"STABLE\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"stable\" : \"1.0.2\"\n" +
                             "  },\n" +
                             "  \"status\" : \"There is a new stable version of Codenvy available. Run im-install to install it.\"\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintAvailableStableAndUnstableVersionsAndStatusNewStableVersionAvailable() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.2");
        updatesArtifactInfo.setLabel(VersionLabel.STABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.3");
        updatesArtifactInfo.setLabel(VersionLabel.UNSTABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.1\",\n" +
                             "  \"label\" : \"STABLE\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"stable\" : \"1.0.2\",\n" +
                             "    \"unstable\" : \"1.0.3\"\n" +
                             "  },\n" +
                             "  \"status\" : \"There is a new stable version of Codenvy available. Run im-download 1.0.2.\"\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintAvailableUnstableVersionAndStatusLatestStableVersionInstalled() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.3");
        updatesArtifactInfo.setLabel(VersionLabel.UNSTABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.1\",\n" +
                             "  \"label\" : \"STABLE\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"unstable\" : \"1.0.3\"\n" +
                             "  },\n" +
                             "  \"status\" : \"You are running the latest stable version of Codenvy!\"\n" +
                             "}\n");
    }

    @Test
    public void shouldBePrintInstalledUnstableVersionAndAvailableStableVersionAndStatusNewStableVersionAvailable() throws Exception {
        List<InstallArtifactInfo> installedList = new ArrayList<>();

        InstallArtifactInfo installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact("install-im-cli");
        installedArtifactInfo.setVersion("1.0.1");
        installedArtifactInfo.setLabel(VersionLabel.STABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        installedArtifactInfo = new InstallArtifactInfo();
        installedArtifactInfo.setArtifact(CDECArtifact.NAME);
        installedArtifactInfo.setVersion("1.0.3");
        installedArtifactInfo.setLabel(VersionLabel.UNSTABLE);
        installedArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
        installedList.add(installedArtifactInfo);

        doReturn(installedList).when(facade).getInstalledVersions();

        List<UpdatesArtifactInfo> updateList = new ArrayList<>();
        UpdatesArtifactInfo updatesArtifactInfo = new UpdatesArtifactInfo();
        updatesArtifactInfo.setArtifact(CDECArtifact.NAME);
        updatesArtifactInfo.setVersion("1.0.4");
        updatesArtifactInfo.setLabel(VersionLabel.STABLE);
        updatesArtifactInfo.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
        updateList.add(updatesArtifactInfo);

        doReturn(updateList).when(facade).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifact\" : \"codenvy\",\n" +
                             "  \"version\" : \"1.0.3\",\n" +
                             "  \"label\" : \"UNSTABLE\",\n" +
                             "  \"availableVersion\" : {\n" +
                             "    \"stable\" : \"1.0.4\"\n" +
                             "  },\n" +
                             "  \"status\" : \"There is a new stable version of Codenvy available. Run im-download 1.0.4.\"\n" +
                             "}\n");
    }

}