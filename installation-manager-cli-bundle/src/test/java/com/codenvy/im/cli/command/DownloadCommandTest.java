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
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.Commons;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class DownloadCommandTest {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;
    @Mock
    private CommandSession             commandSession;

    private JacksonRepresentation<UserCredentials> userCredentialsRep;
    private String okServiceResponse = "{"
                                       + "artifact: {"
                                       + "           artifact: any,"
                                       + "           version: any,"
                                       + "           status: SUCCESS"
                                       + "           },"
                                       + "status: \"OK\""
                                       + "}";

    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new DownloadCommand());
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;

        doNothing().when(spyCommand).init();

        UserCredentials credentials = new UserCredentials("token", "accountId");
        userCredentialsRep = new JacksonRepresentation<>(credentials);
        doReturn(userCredentialsRep).when(spyCommand).getCredentialsRep();
    }

    @Test(enabled = false)
    public void testDownload() throws Exception {
//        doReturn("").when(((DownloadCommand)spyCommand).generateDownloadDescriptorId());
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).startDownload(anyString(), eq(userCredentialsRep));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(okServiceResponse)));
    }

    @Test(enabled = false)
    public void testDownloadArtifact() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).startDownload(CDECArtifact.NAME, "", userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(okServiceResponse) + "\n"));
    }

    @Test(enabled = false)
    public void testDownloadArtifactVersion() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).startDownload(CDECArtifact.NAME, "2.0.5", "", userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "2.0.5");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(okServiceResponse)));
    }

    @Test(enabled = false)
    public void testDownloadWhenErrorInResponse() throws Exception {
        String serviceErrorResponse = "{"
                                      + "message: \"Some error\","
                                      + "status: \"ERROR\""
                                      + "}";
        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).startDownload("", userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(serviceErrorResponse)));
    }

    @Test(enabled = false)
    public void testDownloadWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{"
                                + "message: \"Server Error Exception\","
                                + "status: \"ERROR\""
                                + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(mockInstallationManagerProxy).startDownload("", userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(expectedOutput)));
    }

    @Test
    public void testCheckUpdates() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).getUpdates(userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, Commons.getPrettyPrintingJson(okServiceResponse) + "\n");
    }

    @Test
    public void testCheckUpdatesWhenErrorInResponse() throws Exception {
        String serviceErrorResponse = "{"
                                      + "message: \"Some error\","
                                      + "status: \"ERROR\""
                                      + "}";
        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).getUpdates(userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, Commons.getPrettyPrintingJson(serviceErrorResponse) + "\n");
    }

    @Test
    public void testCheckUpdatesWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{"
                                + "message: \"Server Error Exception\","
                                + "status: \"ERROR\""
                                + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(mockInstallationManagerProxy).getUpdates(userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, Commons.getPrettyPrintingJson(expectedOutput) + "\n");
    }

    @Test
    public void testListLocalOption() throws Exception {
        final String ok = "{"
                          + "status: \"OK\""
                          + "}";
        doReturn(ok).when(mockInstallationManagerProxy).getDownloads();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list-local", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(ok)));
    }
}
