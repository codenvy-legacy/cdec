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
import com.codenvy.im.request.Request;
import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.UserCredentials;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestDownloadCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService service;
    @Mock
    private CommandSession             commandSession;

    private UserCredentials testCredentials = new UserCredentials("token", "accountId");
    private String          okResponse      = "{\n" +
                                              "  \"status\" : \"OK\"\n" +
                                              "}";

    private String ok100DownloadStatusResponse = "{\n" +
                                                 "  \"downloadInfo\" : {\n" +
                                                 "     \"downloadResult\" : {\n" +
                                                 "       \"artifacts\" :[ {\n" +
                                                 "         \"artifact\" :\"cdec\",\n" +
                                                 "         \"version\" : \"3.0.0\",\n" +
                                                 "         \"file\" :\"/home/codenvy-shared/updates/cdec/3.0.0/cdec-3.0.0.zip\",\n" +
                                                 "         \"status\" :\"SUCCESS\"\n" +
                                                 "       } ],\n" +
                                                 "       \"status\" :\"OK\"\n" +
                                                 "    },\n" +
                                                 "    \"percents\" : 100,\n" +
                                                 "    \"status\" : \"DOWNLOADED\"\n" +
                                                 "  },\n" +
                                                 "  \"status\" : \"OK\"\n" +
                                                 "}";

    private String ok100DownloadCommandResponse = "{\n" +
                                                  "  \"artifacts\" : [ {\n" +
                                                  "    \"artifact\" : \"cdec\",\n" +
                                                  "    \"version\" : \"3.0.0\",\n" +
                                                  "    \"file\" : \"/home/codenvy-shared/updates/cdec/3.0.0/cdec-3.0.0.zip\",\n" +
                                                  "    \"status\" : \"SUCCESS\"\n" +
                                                  "  } ],\n" +
                                                  "  \"status\" : \"OK\"\n" +
                                                  "}";

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new DownloadCommand());
        spyCommand.service = service;

        performBaseMocks(spyCommand, true);

        doReturn(testCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testDownload() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials);
        doReturn(okResponse).when(service).startDownload(request);
        doReturn(ok100DownloadStatusResponse).when(service).getDownloadStatus();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n"
                             + ok100DownloadCommandResponse + "\n");
    }

    @Test
    public void testDownloadArtifact() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME);
        doReturn(okResponse).when(service).startDownload(request);
        doReturn(ok100DownloadStatusResponse).when(service).getDownloadStatus();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" +
                             ok100DownloadCommandResponse + "\n");
    }

    @Test
    public void testDownloadArtifactVersion() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials).setArtifactName(CDECArtifact.NAME).setVersion("3.0.0");
        doReturn(okResponse).when(service).startDownload(request);
        doReturn(ok100DownloadStatusResponse).when(service).getDownloadStatus();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "3.0.0");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" +
                             ok100DownloadCommandResponse + "\n");
    }

    @Test
    public void testDownloadWhenErrorInResponse() throws Exception {
        String downloadStatusResponse = "{\n" +
                                        "  \"downloadInfo\" : {\n" +
                                        "    \"downloadResult\" : {\n" +
                                        "      \"artifacts\" : [ {\n" +
                                        "        \"artifact\" :\"cdec\",\n" +
                                        "        \"version\" :\"3.0.0\",\n" +
                                        "        \"status\" :\"FAILURE\"\n" +
                                        "      } ],\n" +
                                        "      \"message\" :\"There is no any version of artifact 'cdec'\"," +
                                        "      \"status\" :\"ERROR\"" +
                                        "    },\n" +
                                        "    \"percents\" : 0,\n" +
                                        "    \"status\" : \"FAILURE\"\n" +
                                        "  },\n" +
                                        "  \"status\" : \"OK\"\n" +
                                        "}";

        String serviceErrorResponse = "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"3.0.0\",\n" +
                                      "    \"status\" : \"FAILURE\"\n" +
                                      "  } ],\n" +
                                      "  \"message\" : \"There is no any version of artifact 'cdec'\",\n" +
                                      "  \"status\" : \"ERROR\"\n" +
                                      "}";
        Request request = new Request().setUserCredentials(testCredentials);
        doReturn(okResponse).when(service).startDownload(request);
        doReturn(downloadStatusResponse).when(service).getDownloadStatus();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" +
                             serviceErrorResponse + "\n");
    }

    @Test
    public void testDownloadWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(service).startDownload(any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" + expectedOutput + "\n");
    }

    @Test
    public void testCheckUpdates() throws Exception {
        Request request = new Request().setUserCredentials(testCredentials);
        doReturn(okResponse).when(service).getUpdates(request);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, okResponse + "\n");
    }

    @Test
    public void testCheckUpdatesWhenErrorInResponse() throws Exception {
        String serviceErrorResponse = "{"
                                      + "\"message\": \"Some error\","
                                      + "\"status\": \"ERROR\""
                                      + "}";
        Request request = new Request().setUserCredentials(testCredentials);
        doReturn(serviceErrorResponse).when(service).getUpdates(request);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, serviceErrorResponse + "\n");
    }

    @Test
    public void testCheckUpdatesWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(service).getUpdates(any(Request.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testListLocalOption() throws Exception {
        final String ok = "{\n"
                          + "  \"status\": \"OK\"\n"
                          + "}";

        Request request = new Request().setUserCredentials(testCredentials);
        doReturn(ok).when(service).getDownloads(request);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list-local", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, ok + "\n");
    }
}
