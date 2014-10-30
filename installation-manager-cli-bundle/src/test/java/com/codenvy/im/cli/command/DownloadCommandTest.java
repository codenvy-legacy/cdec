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
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.Commons;

import org.apache.felix.service.command.CommandSession;
import org.json.JSONException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.ansi;
import static org.mockito.Mockito.*;
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
    private String okResponse                   = "{status: \"OK\"}";
    private String ok100DownloadStatusResponse  = "{\n" +
                                                  "  \"download_info\": {\n" +
                                                  "    \"downloadResult\": \"{\\\"status\\\":\\\"OK\\\"," +
                                                  "\\\"artifacts\\\":[{\\\"status\\\":\\\"SUCCESS\\\"," +
                                                  "\\\"file\\\":\\\"/home/codenvy-shared/updates/cdec/3.0.0/cdec-3.0.0.zip\\\"," +
                                                  "\\\"artifact\\\":\\\"cdec\\\",\\\"version\\\":\\\"3.0.0\\\"}]}\",\n" +
                                                  "    \"percents\": 100,\n" +
                                                  "    \"status\": \"DOWNLOADED\"\n" +
                                                  "  },\n" +
                                                  "  \"status\": \"OK\"\n" +
                                                  "}\n";
    private String ok100DownloadCommandResponse = "{\n" +
                                                  "  \"artifacts\": [{\n" +
                                                  "    \"artifact\": \"cdec\",\n" +
                                                  "    \"file\": \"/home/codenvy-shared/updates/cdec/3.0.0/cdec-3.0.0.zip\",\n" +
                                                  "    \"status\": \"SUCCESS\",\n" +
                                                  "    \"version\": \"3.0.0\"\n" +
                                                  "  }],\n" +
                                                  "  \"status\": \"OK\"\n" +
                                                  "}";


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

    @Test
    public void testDownload() throws Exception {
        when(((DownloadCommand)spyCommand).generateDownloadDescriptorId()).thenReturn("id1");
        doReturn(okResponse).when(mockInstallationManagerProxy).startDownload("id1", userCredentialsRep);
        doReturn(ok100DownloadStatusResponse).when(mockInstallationManagerProxy).downloadStatus("id1");

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(ok100DownloadCommandResponse)));
    }

    @Test
    public void testDownloadArtifact() throws Exception {
        when(((DownloadCommand)spyCommand).generateDownloadDescriptorId()).thenReturn("id2");
        doReturn(okResponse).when(mockInstallationManagerProxy).startDownload(CDECArtifact.NAME, "id2", userCredentialsRep);
        doReturn(ok100DownloadStatusResponse).when(mockInstallationManagerProxy).downloadStatus("id2");

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(ok100DownloadCommandResponse)));
    }

    @Test
    public void testDownloadArtifactVersion() throws Exception {
        when(((DownloadCommand)spyCommand).generateDownloadDescriptorId()).thenReturn("id3");
        doReturn(okResponse).when(mockInstallationManagerProxy).startDownload(CDECArtifact.NAME, "3.0.0", "id3", userCredentialsRep);
        doReturn(ok100DownloadStatusResponse).when(mockInstallationManagerProxy).downloadStatus("id3");

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "3.0.0");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(ok100DownloadCommandResponse)));
    }

    @Test
    public void testDownloadWhenErrorInResponse() throws Exception {
        String downloadStatusResponse = "{\n" +
                                        "  \"download_info\": {\n" +
                                        "    \"downloadResult\": \"{\\\"status\\\":\\\"ERROR\\\",\\\"message\\\":\\\"There is no any version of " +
                                        "artifact 'cdec'\\\",\\\"artifacts\\\":[{\\\"status\\\":\\\"FAILURE\\\",\\\"artifact\\\":\\\"cdec\\\"," +
                                        "\\\"version\\\":\\\"3.0.0\\\"}]}\",\n" +
                                        "    \"percents\": 0,\n" +
                                        "    \"status\": \"FAILURE\"\n" +
                                        "  },\n" +
                                        "  \"status\": \"OK\"\n" +
                                        "}\n";


        String serviceErrorResponse = "{\n" +
                                      "  \"artifacts\": [{\n" +
                                      "    \"artifact\": \"cdec\",\n" +
                                      "    \"status\": \"FAILURE\",\n" +
                                      "    \"version\": \"3.0.0\"\n" +
                                      "  }],\n" +
                                      "  \"message\": \"There is no any version of artifact 'cdec'\",\n" +
                                      "  \"status\": \"ERROR\"\n" +
                                      "}\n";
        when(((DownloadCommand)spyCommand).generateDownloadDescriptorId()).thenReturn("id4");
        doReturn(okResponse).when(mockInstallationManagerProxy).startDownload("id4", userCredentialsRep);
        doReturn(downloadStatusResponse).when(mockInstallationManagerProxy).downloadStatus("id4");

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(serviceErrorResponse)));
    }

    @Test
    public void testDownloadWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{"
                                + "message: \"Server Error Exception\","
                                + "status: \"ERROR\""
                                + "}";
        when(((DownloadCommand)spyCommand).generateDownloadDescriptorId()).thenReturn("id5");
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(mockInstallationManagerProxy).startDownload("id5", userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(expectedOutput)));
    }

    @Test
    public void testCheckUpdates() throws Exception {
        doReturn(okResponse).when(mockInstallationManagerProxy).getUpdates(userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, Commons.getPrettyPrintingJson(okResponse) + "\n");
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
        doReturn(ok).when(mockInstallationManagerProxy).getDownloads(userCredentialsRep);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list-local", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(Commons.getPrettyPrintingJson(ok)));
    }


    @Test(enabled = false)
    public void testDownloadWhenErrorInResponseSubscriptionNotFound() throws Exception {
        String downloadStatusResponse = "{\"status\":\"ERROR\",\"download_info\":{\"status\":\"FAILURE\",\"percents\":0," +
                                        "\"downloadResult\":\"{\"status\":\\\"ERROR\\\",\\\"message\\\":\\\"Unexpected error. Can't download " +
                                        "the artifact 'cdec' version 3.0.0. {\\\\\\\"message\\\\\\\":\\\\\\\"Subscription not found " +
                                        "communityaccountvyiu9z02hxyfcapj\\\"}\\\",\\\"artifacts\\\":[{\\\"status\\\":\\\"FAILURE\\\"," +
                                        "\"artifact\":\"cdec\",\"version\":\"3.0.0\"}]}\"}}";


        printResponse(downloadStatusResponse);
        DownloadStatusInfo downloadStatusInfo = DownloadStatusInfo.valueOf(downloadStatusResponse);
        printResponse(downloadStatusInfo.getDownloadResult());
    }

    protected void printResponse(@Nullable String response) {
        try {
            String message = getPrettyPrintingJson(response);
            System.out.println(ansi().a(message));
        } catch (JSONException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
