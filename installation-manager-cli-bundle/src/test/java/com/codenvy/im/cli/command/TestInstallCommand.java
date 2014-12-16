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

import org.apache.felix.service.command.CommandSession;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    @BeforeMethod
    public void initMocks() throws Exception {
        configUtil = mock(ConfigUtil.class);
        doReturn(ImmutableMap.of("a", "MANDATORY")).when(configUtil).loadCdecDefaultProperties("1.0.1");

        service = mock(InstallationManagerService.class);
        doReturn("1.0.1").when(service).getVersionToInstall(any(Request.class));
        doReturn(new Response().setInfos(ImmutableList.of("step 1", "step 2")).toJson())
                .when(service).getInstallInfo(any(InstallOptions.class), any(Request.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand(configUtil));
        spyCommand.service = service;

        performBaseMocks(spyCommand, true);

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testInstallArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
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
        doReturn("1.0.2").when(service).getVersionToInstall(any(Request.class));
        doReturn(false).when(spyCommand).isInstall();
        doReturn(ImmutableMap.of("a", "2", "b", "MANDATORY")).when(configUtil).merge(anyMap(), anyMap());

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

        // first reply [n], then reply [y]
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

        assertEquals(output, "Please, enter CDEC required parameters:\n" +
                             "b: some value\n" +
                             "{\n" +
                             "  \"a\" : \"2\",\n" +
                             "  \"b\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "Please, enter CDEC required parameters:\n" +
                             "b (some value): some value\n" +
                             "a (2): some value\n" +
                             "{\n" +
                             "  \"a\" : \"some value\",\n" +
                             "  \"b\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "{\"infos\":[]}\n");
    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));

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
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
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
    public void testInstallWhenArtifactNameIsAbsent() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();

        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Argument 'artifact' is required.\n");
    }

    @Test
    public void testInstallErrorStepFailed() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
    public void testListOption() throws Exception {
        doReturn(okServiceResponse).when(service).getInstalledVersions(any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"CLI client version\" : \"1.1.0-SNAPSHOT\",\n"
                             + "  \"artifacts\" : [ {\n"
                             + "    \"artifact\" : \"cdec\",\n"
                             + "    \"version\" : \"1.0.1\",\n"
                             + "    \"status\" : \"SUCCESS\"\n"
                             + "  } ],\n"
                             + "  \"status\" : \"OK\"\n"
                             + "}\n");
    }

    @Test
    public void testListOptionWhenServiceError() throws Exception {
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

        // first reply [n], then reply [y]
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

        assertEquals(output, "Please, enter CDEC required parameters:\n" +
                             "a: some value\n" +
                             "{\n" +
                             "  \"a\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "Please, enter CDEC required parameters:\n" +
                             "a (some value): some value\n" +
                             "{\n" +
                             "  \"a\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "{\"infos\":[]}\n");
    }
}
