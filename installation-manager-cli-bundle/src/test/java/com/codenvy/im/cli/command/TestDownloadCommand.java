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
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.response.DownloadProgressResponse;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestDownloadCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private IMArtifactLabeledFacade service;
    @Mock
    private CommandSession          commandSession;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new DownloadCommand());
        spyCommand.facade = service;

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testDownload() throws Exception {
        doNothing().when(service).startDownload(null, null);
        doReturn(new DownloadProgressResponse(DownloadArtifactStatus.DOWNLOADED,
                                                100,
                                                Collections.<DownloadArtifactInfo>emptyList())).when(service).getDownloadProgress();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" +
                             "{\n" +
                             "  \"artifacts\" : [ ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }


    @Test
    public void testDownloadWhenErrorInResponse() throws Exception {
        doNothing().when(service).startDownload(null, null);
        doReturn(new DownloadProgressResponse(DownloadArtifactStatus.FAILED,
                                                0,
                                                Collections.<DownloadArtifactInfo>emptyList())).when(service).getDownloadProgress();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" +
                             "{\n" +
                             "  \"artifacts\" : [ ],\n" +
                             "  \"status\" : \"ERROR\"\n" +
                             "}\n");
    }

    @Test
    public void testDownloadWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception")).when(service).startDownload(null, null);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Downloading might take several minutes depending on your internet connection. Please wait.\n" + expectedOutput + "\n");
    }

    @Test
    public void testCheckUpdates() throws Exception {
        doReturn(Collections.emptyList()).when(service).getUpdates();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifacts\" : [ ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testCheckUpdatesWhenErrorInResponse() throws Exception {
        doThrow(new IOException("Some error")).when(service).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"message\" : \"Some error\",\n" +
                             "  \"status\" : \"ERROR\"\n" +
                             "}\n");
    }

    @Test
    public void testCheckUpdatesWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception")).when(service).getAllUpdates(any(Artifact.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check-remote", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testListLocalOption() throws Exception {
        doReturn(Collections.emptyList()).when(service).getDownloads(null, null);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list-local", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"artifacts\" : [ ],\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }
}
