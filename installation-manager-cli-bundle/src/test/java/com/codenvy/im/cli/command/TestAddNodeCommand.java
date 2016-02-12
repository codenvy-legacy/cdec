/*
 *  [2012] - [2016] Codenvy, S.A.
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
import com.codenvy.im.response.NodeInfo;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestAddNodeCommand extends AbstractTestCommand {
    private static final String TEST_DNS_NAME = "test";

    private AddNodeCommand          spyCommand;
    @Mock
    private IMArtifactLabeledFacade mockInstallationManagerProxy;
    @Mock
    private CommandSession          commandSession;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new AddNodeCommand());
        spyCommand.facade = mockInstallationManagerProxy;

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(new NodeInfo()).when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"node\" : { },\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testAddNodeThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception"))
                .when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }


    @Test
    public void testCodenvy4AddNodeThrowsErrorIfConfigWrong() throws Exception {
        doReturn(Boolean.TRUE).when(spyCommand).isCodenvy4Installed();
        doReturn(new NodeInfo()).when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertTrue(output.contains("ERROR"));
    }

    @Test
    public void testCodenvy4AddNode() throws Exception {
        doReturn(Boolean.TRUE).when(spyCommand).isCodenvy4Installed();
        doReturn(new NodeInfo()).when(mockInstallationManagerProxy).addNode(TEST_DNS_NAME);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("dns", TEST_DNS_NAME);
        commandInvoker.option("--codenvy-ip", "someIp");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"node\" : { },\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");

        verify(spyCommand).updateCodenvyConfig();
        verify(mockInstallationManagerProxy).updateArtifactConfig(any(Artifact.class), anyMap());
    }
}
