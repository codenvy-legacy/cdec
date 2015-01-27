/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.Response;
import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.UserCredentials;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.resource.ResourceException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyInt;
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

    private InstallationManagerService service;
    private ConfigUtil configUtil;
    private CommandSession             commandSession;
    private UserCredentials userCredentials;
    private String okServiceResponse = "{\n"
                                       + "  \"artifacts\" : [ {\n"
                                       + "    \"artifact\" : \"cdec\",\n"
                                       + "    \"version\" : \"1.0.1\",\n"
                                       + "    \"status\" : \"SUCCESS\"\n"
                                       + "  } ],\n"
                                       + "  \"status\" : \"OK\"\n"
                                       + "}";

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;

    PrintStream originOut = System.out;
    PrintStream originErr = System.err;

    @BeforeMethod
    public void initMocks() throws Exception {
        configUtil = mock(ConfigUtil.class);
        doReturn(new HashMap<>(ImmutableMap.of("a", "MANDATORY"))).when(configUtil).loadCdecDefaultProperties("1.0.1");

        service = mock(InstallationManagerService.class);
        doReturn("1.0.1").when(service).getVersionToInstall(any(Request.class), anyInt());
        doReturn(new Response().setInfos(ImmutableList.of("step 1", "step 2")).toJson())
            .when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand(configUtil));
        spyCommand.service = service;

        performBaseMocks(spyCommand, true);

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
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
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        final String expectedOutput = "step 1 [OK]\n" +
                                      "step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(service).install(any(InstallOptions.class), any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput);
    }

    @Test
    public void testEnterInstallOptionsForUpdate() throws Exception {
        doReturn("1.0.2").when(service).getVersionToInstall(any(Request.class), anyInt());
        doReturn(false).when(spyCommand).isInstall();
        doReturn(new HashMap<>(ImmutableMap.of("a", "2", "b", "MANDATORY"))).when(configUtil).merge(anyMap(), anyMap());

        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print("some value\n");
                return "some value";
            }
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn("{\"infos\":[]}").when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));

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

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "Please, enter mandatory CDEC parameters (values cannot be left blank):\n"
                             + "b: some value\n"
                             + "\n"
                             + "CDEC parameters list:\n"
                             + "{\n"
                             + "  \"a\"       : \"2\",\n"
                             + "  \"b\"       : \"some value\",\n"
                             + "  \"version\" : \"1.0.2\"\n"
                             + "}\n"
                             + "Do you confirm parameters above? [y/N]\n"
                             + "Please, enter CDEC parameters (just press 'Enter' key to keep value as is):\n"
                             + "a (value='2'): some value\n"
                             + "b (value='some value'): some value\n"
                             + "version (value='1.0.2'): some value\n"
                             + "\n"
                             + "CDEC parameters list:\n"
                             + "{\n"
                             + "  \"a\"       : \"some value\",\n"
                             + "  \"b\"       : \"some value\",\n"
                             + "  \"version\" : \"some value\"\n"
                             + "}\n"
                             + "Do you confirm parameters above? [y/N]\n"
                             + "{\"infos\":[]}\n"
                             + "");
    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));

        final String expectedOutput = "step 1 [OK]\n" +
                                      "step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(service).install(any(InstallOptions.class), any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput);
    }

    @Test
    public void testInstallWhenUnknownArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        final String serviceErrorResponse = "{\n"
                                            + "  \"message\" : \"Artifact 'any' not found\",\n"
                                            + "  \"status\" : \"ERROR\"\n"
                                            + "}";

        doReturn(serviceErrorResponse).when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", "any");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, serviceErrorResponse + "\n");
    }

    @Test
    public void testInstallErrorStepFailed() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        final String expectedOutput = "step 1 [FAIL]\n" +
                                      "{\n"
                                      + "  \"message\" : \"step failed\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";
        doReturn("{\n"
                 + "  \"message\" : \"step failed\",\n"
                 + "  \"status\" : \"ERROR\"\n"
                 + "}").when(service).install(any(InstallOptions.class), any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testInstallWhenServiceThrowsError2() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterMandatoryOptions(any(InstallOptions.class));
        final String expectedOutput = "{\n"
                                      + "  \"message\" : \"Property is missed\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";
        doReturn(expectedOutput).when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testListInstalledArtifacts() throws Exception {
        doReturn(okServiceResponse).when(service).getInstalledVersions(any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"artifacts\" : [ {\n"
                             + "    \"artifact\" : \"cdec\",\n"
                             + "    \"version\" : \"1.0.1\",\n"
                             + "    \"status\" : \"SUCCESS\"\n"
                             + "  } ],\n"
                             + "  \"status\" : \"OK\"\n"
                             + "}\n");
    }

    @Test
    public void testListInstalledArtifactsWhenServiceError() throws Exception {
        doReturn(new HashMap<>(ImmutableMap.of("a", "2", "b", "MANDATORY"))).when(configUtil).merge(anyMap(), anyMap());

        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
            .when(service)
            .getInstalledVersions(any(Request.class));

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
        doReturn(true).when(spyCommand).isInstall();
        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                spyCommand.console.print("some value\n");
                return "some value";
            }
        }).when(spyCommand.console).readLine();

        // no installation info provided
        doReturn("{\"infos\":[]}").when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));

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

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "Please, enter mandatory CDEC parameters (values cannot be left blank):\n"
                             + "a: some value\n"
                             + "\n"
                             + "CDEC parameters list:\n"
                             + "{\n"
                             + "  \"a\" : \"some value\"\n"
                             + "}\n"
                             + "Do you confirm parameters above? [y/N]\n"
                             + "Please, enter CDEC parameters (just press 'Enter' key to keep value as is):\n"
                             + "a (value='some value'): some value\n"
                             + "\n"
                             + "CDEC parameters list:\n"
                             + "{\n"
                             + "  \"a\" : \"some value\"\n"
                             + "}\n"
                             + "Do you confirm parameters above? [y/N]\n"
                             + "{\"infos\":[]}\n"
                             + "");
    }

    @Test
    public void testEnterEmptyMandatoryOptions() throws IOException {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        Map<String, String> properties = ImmutableMap.of();
        options.setConfigProperties(properties);

        assertEquals(spyCommand.enterMandatoryOptions(options), options);
    }

    @Test
    public void testEnterValidValuesOfMandatoryOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        Map<String, String> properties = ImmutableSortedMap.of("some property", "test");
        options.setConfigProperties(properties);
        Map<String, String> result = spyCommand.enterMandatoryOptions(options).getConfigProperties();
        assertEquals(result.toString(), "{some property=test}");
        assertEquals(outputStream.toString(), "Please, enter mandatory CDEC parameters (values cannot be left blank):\n");
    }

    @Test
    public void testEnterInvalidValuesOfMandatoryOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

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
        assertEquals(outputStream.toString(), "Please, enter mandatory CDEC parameters (values cannot be left blank):\n"
                                              + "property 2: property 2: property 2: ");
    }

    @Test
    public void testEnterEmptyAllOptions() throws IOException {
        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

        Map<String, String> properties = ImmutableMap.of();
        options.setConfigProperties(properties);

        assertEquals(spyCommand.enterAllOptions(options), options);
    }

    @Test
    public void testEnterAllOptions() throws IOException {
        spyCommand.artifactName = CDECArtifact.NAME;

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

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
        assertEquals(outputStream.toString(), "Please, enter CDEC parameters (just press 'Enter' key to keep value as is):\n"
                                              + "property 1 (value='value 1'): property 2 (value='value 2'): property 3 (value=''): ");
    }
}
