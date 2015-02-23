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

import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.UserCredentials;
import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestAddNodeCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;
    @Mock
    private CommandSession             commandSession;

    private UserCredentials credentials;

    private static final NodeConfig TEST_BUILDER_NODE = new NodeConfig(NodeConfig.NodeType.BUILDER, "builder.node.com");
    private static final NodeConfig TEST_RUNNER_NODE  = new NodeConfig(NodeConfig.NodeType.RUNNER, "runner.node.com");

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new AddNodeCommand());
        spyCommand.service = mockInstallationManagerProxy;

        performBaseMocks(spyCommand, true);

        credentials = new UserCredentials("token", "accountId");
        doReturn(credentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testAddNodeCommand() throws Exception {
        String okServiceResponse = "{\n"
                                   + "  \"status\" : \"OK\"\n"
                                   + "}";
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).addNode(TEST_BUILDER_NODE);
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).addNode(TEST_RUNNER_NODE);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--builder", TEST_BUILDER_NODE.getHost());
        commandInvoker.option("--runner", TEST_RUNNER_NODE.getHost());

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, okServiceResponse + "\n" + okServiceResponse + "\n");

        verify(mockInstallationManagerProxy).addNode(TEST_BUILDER_NODE);
        verify(mockInstallationManagerProxy).addNode(TEST_RUNNER_NODE);
    }

    @Test
    public void testAddNodeCommandWithoutOptions() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "");

        verify(mockInstallationManagerProxy, never()).addNode(any(NodeConfig.class));
    }
}
