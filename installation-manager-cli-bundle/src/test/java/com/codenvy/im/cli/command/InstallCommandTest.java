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
import com.codenvy.im.response.Response;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;

import org.apache.felix.service.command.CommandSession;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.any;
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
public class InstallCommandTest {
    private AbstractIMCommand spyCommand;

    private InstallationManagerService mockInstallationManagerProxy;
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
        mockInstallationManagerProxy = mock(InstallationManagerService.class);
        doReturn(new Response.Builder().withInfos(new ArrayList<String>() {
            {
                add("step 1");
                add("step 2");
            }
        }).build().toJson()).when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand());
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;

        doNothing().when(spyCommand).init();

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testInstallArtifact() throws Exception {
        final String expectedOutput = "Step 1: step 1 [OK]\n" +
                                      "Step 2: step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
// TODO        assertEquals(output, expectedOutput);

    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        final String expectedOutput = "Step 1: step 1 [OK]\n" +
                                      "Step 2: step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
// TODO        assertEquals(output, expectedOutput);
    }

    @Test
    public void testInstallWhenUnknownArtifact() throws Exception {
        String serviceErrorResponse = "{\n"
                                      + "  \"message\" : \"Artifact 'any' not found\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";

        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", "any");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
// TODO        assertEquals(output, serviceErrorResponse + "\n");
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
        String expectedOutput = "Step 1: step 1 [FAIL]\n" +
                                "{\n"
                                + "  \"message\" : \"step failed\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doReturn("{\n"
                 + "  \"message\" : \"step failed\",\n"
                 + "  \"status\" : \"ERROR\"\n"
                 + "}").when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
// TODO        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testInstallWhenServiceThrowsError2() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Property is missed\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doReturn(expectedOutput).when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

// TODO        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testListOption() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).getVersions(new JacksonRepresentation<>(userCredentials));

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
                .when(mockInstallationManagerProxy)
                .getVersions(new JacksonRepresentation<>(userCredentials));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Server Error Exception\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");
    }
}
