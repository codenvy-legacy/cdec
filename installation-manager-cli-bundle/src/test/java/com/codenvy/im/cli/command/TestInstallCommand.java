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
import com.google.common.collect.ImmutableSortedMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 *         Alexander Reshetnyak
 */
public class TestInstallCommand extends AbstractTestCommand {
    private InstallCommand spyCommand;

    private IMArtifactLabeledFacade facade;
    private ConfigManager           mockConfigManager;
    private CommandSession          commandSession;

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;

    PrintStream originOut = System.out;
    PrintStream originErr = System.err;

    @BeforeMethod
    public void initMocks() throws Exception {
        mockConfigManager = mock(ConfigManager.class);
        doReturn(new HashMap<>(ImmutableMap.of("a", "MANDATORY"))).when(mockConfigManager).loadCodenvyDefaultProperties(Version.valueOf("1.0.1"),
                                                                                                                        InstallType.SINGLE_SERVER);
        doReturn(new Config(new HashMap<>(ImmutableMap.of("a", "MANDATORY")))).when(mockConfigManager)
                                                                              .loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);

        facade = mock(IMArtifactLabeledFacade.class);
        doReturn(ImmutableList.of("step 1", "step 2")).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand(mockConfigManager));
        spyCommand.facade = facade;

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
    public void testInstallArtifact() throws Exception {
        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "step 1 [OK]\n" +
                             "step 2 [OK]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.1\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testInstallMultiServerArtifact() throws Exception {
        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");
        commandInvoker.option("--multi", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "step 1 [OK]\n" +
                             "step 2 [OK]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.1\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testEnterInstallOptionsForUpdate() throws Exception {
        doReturn(false).when(spyCommand).isInstall(any(Artifact.class));
        doReturn(new HashMap<>(ImmutableMap.of("a", "2", "b", "MANDATORY"))).when(mockConfigManager).prepareInstallProperties(anyString(),
                                                                                                                              any(InstallType.class),
                                                                                                                              any(Artifact.class),
                                                                                                                              any(Version.class),
                                                                                                                              anyBoolean());
        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print("some value\n");
                return "some value";
            }
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn(Collections.emptyList()).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        // firstly don't confirm install options, then confirm
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return false;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return true;
            }
        }).when(spyCommand.console).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.2");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "Please, enter mandatory Codenvy parameters (values cannot be left blank):\n" +
                             "b: some value\n" +
                             "\n" +
                             "Codenvy parameters list:\n" +
                             "{\n" +
                             "  \"a\" : \"2\",\n" +
                             "  \"b\" : \"some value\"\n" +
                             "}\n" +
                             "Do you confirm parameters above? [y/N]\n" +
                             "Please, enter Codenvy parameters (just press 'Enter' key to keep value as is):\n" +
                             "a (value='2'): some value\n" +
                             "b (value='some value'): some value\n" +
                             "\n" +
                             "Codenvy parameters list:\n" +
                             "{\n" +
                             "  \"a\" : \"some value\",\n" +
                             "  \"b\" : \"some value\"\n" +
                             "}\n" +
                             "Do you confirm parameters above? [y/N]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.2\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        InstallArtifactStepInfo info = mock(InstallArtifactStepInfo.class);
        doReturn(InstallArtifactStatus.SUCCESS).when(info).getStatus();

        doReturn(info).when(facade).getUpdateStepInfo(anyString());
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn("id").when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "step 1 [OK]\n" +
                             "step 2 [OK]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.1\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testInstallWhenUnknownArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doThrow(new IOException("Artifact 'any' not found")).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", "any");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Artifact 'any' not found\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");
    }

    @Test
    public void testInstallErrorStepFailed() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doThrow(new IOException("step failed")).when(facade).install(any(Artifact.class), any(Version.class), any(InstallOptions.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "step 1 [FAIL]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.1\",\n" +
                             "    \"status\" : \"FAILURE\"\n" +
                             "  } ],\n" +
                             "  \"message\" : \"step failed\",\n" +
                             "  \"status\" : \"ERROR\"\n" +
                             "}\n");
    }

    @Test
    public void testInstallWhenServiceThrowsError2() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doThrow(new IOException("Property is missed")).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "{\n"
                             + "  \"message\" : \"Property is missed\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");
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
        doReturn(new HashMap<>(ImmutableMap.of("a", "MANDATORY"))).when(mockConfigManager).prepareInstallProperties(anyString(),
                                                                                                                    any(InstallType.class),
                                                                                                                    any(Artifact.class),
                                                                                                                    any(Version.class),
                                                                                                                    anyBoolean());

        doReturn(true).when(spyCommand).isInstall(any(Artifact.class));
        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print("some value\n");
                return "some value";
            }
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn(Collections.emptyList()).when(facade).getInstallInfo(any(Artifact.class), any(InstallType.class));

        // firstly don't confirm install options, then confirm
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return false;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return true;
            }
        }).when(spyCommand.console).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.0");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "Please, enter mandatory Codenvy parameters (values cannot be left blank):\n" +
                             "a: some value\n" +
                             "\n" +
                             "Codenvy parameters list:\n" +
                             "{\n" +
                             "  \"a\" : \"some value\"\n" +
                             "}\n" +
                             "Do you confirm parameters above? [y/N]\n" +
                             "Please, enter Codenvy parameters (just press 'Enter' key to keep value as is):\n" +
                             "a (value='some value'): some value\n" +
                             "\n" +
                             "Codenvy parameters list:\n" +
                             "{\n" +
                             "  \"a\" : \"some value\"\n" +
                             "}\n" +
                             "Do you confirm parameters above? [y/N]\n" +
                             "{\n" +
                             "  \"artifacts\" : [ {\n" +
                             "    \"artifact\" : \"codenvy\",\n" +
                             "    \"version\" : \"1.0.0\",\n" +
                             "    \"status\" : \"SUCCESS\"\n" +
                             "  } ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testEnterEmptyMandatoryOptions() throws IOException {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);

        Map<String, String> properties = ImmutableMap.of();
        options.setConfigProperties(properties);

        assertEquals(spyCommand.enterMandatoryOptions(options), options);
    }

    @Test
    public void testEnterValidValuesOfMandatoryOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);

        Map<String, String> properties = ImmutableSortedMap.of("some property", "test");
        options.setConfigProperties(properties);
        Map<String, String> result = spyCommand.enterMandatoryOptions(options).getConfigProperties();
        assertEquals(result.toString(), "{some property=test}");
        assertEquals(outputStream.toString(), "Please, enter mandatory Codenvy parameters (values cannot be left blank):\n");
    }

    @Test
    public void testEnterInvalidValuesOfMandatoryOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);

        // firstly readLine() returns invalid "", then invalid "MANDATORY", then valid "new value"
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "";
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "MANDATORY";
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "new value";
            }
        }).when(spyCommand.console).readLine();

        Map<String, String> properties = ImmutableMap.of("property 1", "value 1", "property 2", "MANDATORY", "property 3", "");
        options.setConfigProperties(properties);
        Map<String, String> result = spyCommand.enterMandatoryOptions(options).getConfigProperties();
        assertEquals(result.toString(), "{property 1=value 1, property 2=new value, property 3=}");
        assertEquals(outputStream.toString(), "Please, enter mandatory Codenvy parameters (values cannot be left blank):\n"
                                              + "property 2: property 2: property 2: ");
    }

    @Test
    public void testEnterEmptyAllOptions() throws IOException {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);

        Map<String, String> properties = ImmutableMap.of();
        options.setConfigProperties(properties);

        assertEquals(spyCommand.enterAllOptions(options), options);
    }

    @Test
    public void testEnterAllOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallType.SINGLE_SERVER);

        // firstly readLine() returns invalid "", then invalid "MANDATORY", then valid "new value"
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "";
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "MANDATORY";
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "new value";
            }
        }).when(spyCommand.console).readLine();

        Map<String, String> properties = ImmutableMap.of("property 1", "value 1", "property 2", "value 2", "property 3", "");
        options.setConfigProperties(properties);
        Map<String, String> result = spyCommand.enterAllOptions(options).getConfigProperties();
        assertEquals(result.toString(), "{property 1=value 1, property 2=value 2, property 3=new value}");
        assertEquals(outputStream.toString(), "Please, enter Codenvy parameters (just press 'Enter' key to keep value as is):\n"
                                              + "property 1 (value='value 1'): property 2 (value='value 2'): property 3 (value=''): ");
    }

}
