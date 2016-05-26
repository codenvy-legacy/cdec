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
package com.codenvy.im.cli.command;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.event.Event;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.InstallArtifactStepInfo;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static java.lang.String.format;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 *         Alexander Reshetnyak
 */
public class TestInstallCommand extends AbstractTestCommand {
    public static final String TEST_ARTIFACT = CDECArtifact.NAME;
    public static final String TEST_VERSION = "1.0.1";
    public static final String ERROR_MESSAGE = "error";
    public static final List<String> INSTALL_INFO = ImmutableList.of("step 1", "step 2", "step 3");
    private InstallCommand spyCommand;

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
        doReturn(INSTALL_INFO).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand(mockConfigManager));
        spyCommand.facade = facade;
        spyCommand.preferencesStorage = mockPreferencesStorage;

        performBaseMocks(spyCommand, true);
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
    public void shouldInstallArtifactOfCertainVersion() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 1 [OK]\n" +
                                    "step 2 [OK]\n" +
                                    "step 3 [OK]\n" +
                                    "{\n" +
                                    "  \"artifacts\" : [ {\n" +
                                    "    \"artifact\" : \"%s\",\n" +
                                    "    \"version\" : \"%s\",\n" +
                                    "    \"status\" : \"SUCCESS\"\n" +
                                    "  } ],\n" +
                                    "  \"status\" : \"OK\"\n" +
                                    "}\n", TEST_ARTIFACT, TEST_VERSION));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }

    @Test
    public void shouldInstallMultiServerArtifact() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);
        commandInvoker.option("--multi", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 1 [OK]\n" +
                                    "step 2 [OK]\n" +
                                    "step 3 [OK]\n" +
                                    "{\n" +
                                    "  \"artifacts\" : [ {\n" +
                                    "    \"artifact\" : \"%s\",\n" +
                                    "    \"version\" : \"%s\",\n" +
                                    "    \"status\" : \"SUCCESS\"\n" +
                                    "  } ],\n" +
                                    "  \"status\" : \"OK\"\n" +
                                    "}\n", TEST_ARTIFACT, TEST_VERSION));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }

    @Test
    public void shouldUpdateAfterEnteringInstallOptions() throws Exception {
        doReturn(false).when(spyCommand).isInstall(any(Artifact.class));
        doReturn(new HashMap<>(ImmutableMap.of("a", "2", "b", "MANDATORY"))).when(mockConfigManager).prepareInstallProperties(anyString(),
                                                                                                                              any(Path.class),
                                                                                                                              any(InstallType.class),
                                                                                                                              any(Artifact.class),
                                                                                                                              any(Version.class),
                                                                                                                              anyBoolean());
        // user always enter "some value" as property value
        doAnswer(invocationOnMock -> {
                spyCommand.console.print("some value\n");
                return "some value";
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn(Collections.emptyList()).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        // firstly don't confirm install options, then confirm
        doAnswer(invocationOnMock -> {
            spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
            return false;
        }).doAnswer(invocationOnMock -> {
            spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
            return true;
        }).when(spyCommand.console).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", "1.0.2");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.2\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");

        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void shouldFailsOnUnknownArtifact() throws Exception {
        doThrow(new IOException("Artifact 'any' not found")).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", "any");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Artifact 'any' not found\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");

        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void shouldFailOnInstallationStepException() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        doThrow(new IOException(ERROR_MESSAGE)).when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 1 [FAIL]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"%s\",\n" +
                             "    \"version\" : \"%s\",\n" +
                             "    \"status\" : \"FAILURE\"\n" +
                             "  } ],\n" +
                             "  \"message\" : \"%s\",\n" +
                             "  \"status\" : \"ERROR\"\n" +
                             "}\n", TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_UNSUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, ERROR-MESSAGE=%s, TIME=\\d*}",
                                                                           TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }

    @Test
    public void shouldFailOnErrorOfGettingUpdateStepInfo() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);
        
        InstallArtifactStepInfo testInstallArtifactStepInfo = new InstallArtifactStepInfo();
        testInstallArtifactStepInfo.setStatus(InstallArtifactStatus.FAILURE);
        testInstallArtifactStepInfo.setMessage(ERROR_MESSAGE);

        doReturn(testInstallArtifactStepInfo).when(facade).getUpdateStepInfo(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 1 [FAIL]\n" +
                                    "{\n" +
                                    "  \"artifacts\" : [ {\n" +
                                    "    \"artifact\" : \"%s\",\n" +
                                    "    \"version\" : \"%s\",\n" +
                                    "    \"status\" : \"FAILURE\"\n" +
                                    "  } ],\n" +
                                    "  \"message\" : \"%s\",\n" +
                                    "  \"status\" : \"ERROR\"\n" +
                                    "}\n", TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_UNSUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, ERROR-MESSAGE=%s, TIME=\\d*}",
                                                                           TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }


    @Test
    public void testInstallWhenServiceThrowsError2() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        String errorMessage = "Property is missed";
        doThrow(new IOException(errorMessage)).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, format("{\n"
                             + "  \"message\" : \"%s\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n", errorMessage));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_UNSUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, ERROR-MESSAGE=%s, TIME=\\d*}",
                                                                           TEST_ARTIFACT, TEST_VERSION, errorMessage)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }

     @Test
    public void testListInstalledArtifacts() throws Exception {		
        doReturn(ImmutableList.of(new InstallArtifactInfo().withArtifact("codenvy")		
                                                           .withVersion("1.0.1")		
                                                           .withStatus(InstallArtifactStatus.SUCCESS))).when(facade).getInstalledVersions();		
		
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);		
        commandInvoker.option("--list", Boolean.TRUE);		
		
        CommandInvoker.Result result = commandInvoker.invoke();		
        String output = result.getOutputStream();		
        assertEquals(output, "{\n"		
                             + "  \"artifacts\" : [ {\n"		
                             + "    \"artifact\" : \"codenvy\",\n"		
                             + "    \"version\" : \"1.0.1\",\n"		
                             + "    \"status\" : \"SUCCESS\"\n"		
                             + "  } ],\n"		
                             + "  \"status\" : \"OK\"\n"		
                             + "}\n");		
    }		
		
    @Test		
    public void testListInstalledArtifactsWhenServiceError() throws Exception {		
        doReturn(new HashMap<>(ImmutableMap.of("a", "2", "b", "MANDATORY"))).when(mockConfigManager)		
                                                                            .merge(any(Version.class), anyMap(), anyMap());		
		
        doThrow(new RuntimeException("Server Error Exception"))		
                .when(facade)		
                .getInstalledVersions();		
		
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);		
        commandInvoker.option("--list", Boolean.TRUE);		
		
        CommandInvoker.Result result = commandInvoker.invoke();		
        String output = result.disableAnsi().getOutputStream();		
        assertEquals(output, "{\n"		
                             + "  \"message\" : \"Server Error Exception\",\n"		
                             + "  \"status\" : \"ERROR\"\n"		
                             + "}\n");		
    }

    @Test
    public void testEnterInstallOptionsForInstall() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);
        
        doReturn(new HashMap<>(ImmutableMap.of("a", "MANDATORY"))).when(mockConfigManager).prepareInstallProperties(anyString(),
                                                                                                                    any(Path.class),
                                                                                                                    any(InstallType.class),
                                                                                                                    any(Artifact.class),
                                                                                                                    any(Version.class),
                                                                                                                    anyBoolean());

        doReturn(true).when(spyCommand).isInstall(any(Artifact.class));
        // user always enter "some value" as property value
        doAnswer(invocationOnMock -> {
            spyCommand.console.print("some value\n");
            return "some value";
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn(Collections.emptyList()).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        // firstly don't confirm install options, then confirm
        doAnswer(invocationOnMock -> {
            spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
            return false;
        }).doAnswer(invocationOnMock -> {
            spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
            return true;
        }).when(spyCommand.console).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, format("{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"%s\",\n" +
                             "    \"version\" : \"%s\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n", TEST_ARTIFACT, TEST_VERSION));

        verify(facade, times(2)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

        assertEquals(values.get(1).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY);
        assertTrue(values.get(1).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(1).getParameters().toString());
    }

    @Test
    public void testInstallArtifactFromLocalBinariesFailedIfVersionMissed() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--binaries", "/path/to/file");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Parameter 'version' is missed\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");

        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void testInstallArtifactFromLocalBinariesFailedIfArtifactMissed() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--binaries", "/path/to/file");
        commandInvoker.argument("version", "3.10.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Parameter 'artifact' is missed\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");

        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void testInstallArtifactFromLocalBinaries() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);
        
        String versionNumber = "3.10.1";
        String binaries = "/path/to/file";

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--binaries", binaries);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", versionNumber);

        CommandInvoker.Result result = commandInvoker.invoke();

        String output = result.disableAnsi().getOutputStream();   // TODO [ndp] strange result
        assertEquals(output, "step 1{\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");

        verify(mockConfigManager).prepareInstallProperties(isNull(String.class),
                                                       eq(Paths.get(binaries)),
                                                       eq(InstallType.SINGLE_SERVER),
                                                       eq(ArtifactFactory.createArtifact(TEST_ARTIFACT)),
                                                       eq(Version.valueOf(versionNumber)),
                                                       eq(Boolean.TRUE));

        verify(facade).install(eq(ArtifactFactory.createArtifact(TEST_ARTIFACT)),
                                          eq(Version.valueOf(versionNumber)),
                                          eq(Paths.get(binaries)),
                                          any(InstallOptions.class));

        verify(facade, times(1)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, versionNumber)),
                   "Actual parameters: " + values.get(0).getParameters().toString());
    }

    @Test
    public void testReinstallCodenvy() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.option("--reinstall", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"artifacts\" : [ {\n"
                             + "    \"artifact\" : \"codenvy\",\n"
                             + "    \"status\" : \"SUCCESS\"\n"
                             + "  } ],\n"
                             + "  \"status\" : \"OK\"\n"
                             + "}\n");

        verify(facade).reinstall(createArtifact(TEST_ARTIFACT));
        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void testReinstallImCli() throws Exception {
        doThrow(new UnsupportedOperationException("error message")).when(facade).reinstall(createArtifact(InstallManagerArtifact.NAME));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", InstallManagerArtifact.NAME);
        commandInvoker.option("--reinstall", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"artifacts\" : [ {\n"
                             + "    \"artifact\" : \"installation-manager-cli\",\n"
                             + "    \"status\" : \"FAILURE\"\n"
                             + "  } ],\n"
                             + "  \"message\" : \"error message\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");

        verify(facade, never()).logSaasAnalyticsEvent(any(Event.class), anyString());
    }

    @Test
    public void shouldNotInterruptInstallIfLoggingToSaasCodenvyFail() throws Exception {
        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        doThrow(new IOException("error")).when(facade).logSaasAnalyticsEvent(any(Event.class), any(String.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 1 [OK]\n" +
                                    "step 2 [OK]\n" +
                                    "step 3 [OK]\n" +
                                    "{\n" +
                                    "  \"artifacts\" : [ {\n" +
                                    "    \"artifact\" : \"%s\",\n" +
                                    "    \"version\" : \"%s\",\n" +
                                    "    \"status\" : \"SUCCESS\"\n" +
                                    "  } ],\n" +
                                    "  \"status\" : \"OK\"\n" +
                                    "}\n", TEST_ARTIFACT, TEST_VERSION));

    }

    @Test
    public void shouldInstallArtifactForceFirstStep() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);
        commandInvoker.option("--step", 1);
        commandInvoker.option("--forceInstall", true);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "step 1 [OK]\n");

        verify(facade, times(1)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_STARTED);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());
    }

    @Test
    public void shouldInstallArtifactForceMiddleStepAndFail() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        doThrow(new IOException(ERROR_MESSAGE)).when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);
        commandInvoker.option("--step", 2);
        commandInvoker.option("--forceInstall", true);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 2 [FAIL]\n"
                                    + "{\n"
                                    + "  \"artifacts\" : [ {\n"
                                    + "    \"artifact\" : \"%s\",\n"
                                    + "    \"version\" : \"%s\",\n"
                                    + "    \"status\" : \"FAILURE\"\n"
                                    + "  } ],\n"
                                    + "  \"message\" : \"%s\",\n"
                                    + "  \"status\" : \"ERROR\"\n"
                                    + "}\n",
                                    TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE));

        verify(facade, times(1)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_UNSUCCESSFULLY);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, ERROR-MESSAGE=%s, TIME=\\d*}",
                                                                           TEST_ARTIFACT, TEST_VERSION, ERROR_MESSAGE)),
                   "Actual parameters: " + values.get(0).getParameters().toString());

    }

    @Test
    public void shouldInstallArtifactForceLastStep() throws Exception {
        ArgumentCaptor<Event> eventArgument = ArgumentCaptor.forClass(Event.class);

        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", TEST_ARTIFACT);
        commandInvoker.argument("version", TEST_VERSION);
        commandInvoker.option("--step", INSTALL_INFO.size());
        commandInvoker.option("--forceInstall", true);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, format("step 3 [OK]\n" +
                                    "{\n" +
                                    "  \"artifacts\" : [ {\n" +
                                    "    \"artifact\" : \"%s\",\n" +
                                    "    \"version\" : \"%s\",\n" +
                                    "    \"status\" : \"SUCCESS\"\n" +
                                    "  } ],\n" +
                                    "  \"status\" : \"OK\"\n" +
                                    "}\n", TEST_ARTIFACT, TEST_VERSION));

        verify(facade, times(1)).logSaasAnalyticsEvent(eventArgument.capture(), isNull(String.class));

        List<Event> values = eventArgument.getAllValues();
        assertEquals(values.get(0).getType(), Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY);
        assertTrue(values.get(0).getParameters().toString().matches(format("\\{ARTIFACT=%s, VERSION=%s, TIME=\\d*}", TEST_ARTIFACT, TEST_VERSION)),
                   "Actual parameters: " + values.get(0).getParameters().toString());
    }
}
