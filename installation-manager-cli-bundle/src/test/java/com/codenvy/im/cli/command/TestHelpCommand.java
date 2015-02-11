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

import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.im.service.InstallationManagerService;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.AssertJUnit.assertTrue;

/** @author Anatoliy Bazko */
public class TestHelpCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerService service;
    @Mock
    private CommandSession             commandSession;
    @Mock
    private MultiRemoteCodenvy         multiRemoteCodenvy;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new HelpCommand());
        spyCommand.service = service;
        doReturn(multiRemoteCodenvy).when(spyCommand).getMultiRemoteCodenvy();
        doReturn("").when(multiRemoteCodenvy).listRemotes();

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testHelp() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();

        assertTrue(output.contains("im-download"));
        assertTrue(output.contains("im-config"));
        assertTrue(output.contains("im-subscription"));
        assertTrue(output.contains("im-install"));
    }
}
