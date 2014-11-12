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

import com.codenvy.im.request.Request;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov
 *          Alexander Reshetnyak
 */
public class InstallCommandTest {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;
    @Mock
    private CommandSession             commandSession;

    private static final String ANY = "any";
    private UserCredentials userCredentials;
    private String okServiceResponse = "{\n"
                                       + "  \"artifacts\" : [ {\n"
                                       + "    \"artifact\" : \"any\",\n"
                                       + "    \"version\" : \"any\",\n"
                                       + "    \"status\" : \"SUCCESS\"\n"
                                       + "  } ],\n"
                                       + "  \"status\" : \"OK\"\n"
                                       + "}";

    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new InstallCommand());
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;

        doNothing().when(spyCommand).init();

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testInstallArtifact() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", ANY);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, okServiceResponse + "\n");

    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", ANY);
        commandInvoker.argument("version", "2.0.5");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, (okServiceResponse) + "\n");
    }

    @Test
    public void testInstallWhenUnknownArtifact() throws Exception {
        String serviceErrorResponse = "{\n"
                                      + "  \"message\" : \"Artifact 'non-exists' not found\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";

        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", ANY);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, serviceErrorResponse + "\n");
    }

    @Test
    public void testInstallWhenErrorInResponse() throws Exception {
        String serviceErrorResponse = "{\n"
                                      + "  \"message\" : \"Server Error Exception\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";

        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        commandInvoker.argument("artifact", ANY);

        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, serviceErrorResponse + "\n");
    }

    @Test
    public void testInstallWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";

        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", ANY);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
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
                             + "    \"artifact\" : \"any\",\n"
                             + "    \"version\" : \"any\",\n"
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

    @Test
    public void testInstallStepwisely() {
        // TODO
    }
}
