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
package com.codenvy.im.command;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.agent.SecureShellAgent;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Dmytro Nochevnov
 */
public class RemoteCommandTest {
    @Mock
    SecureShellAgent mockAgent;

    @BeforeTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCommand() {
        doReturn("test").when(mockAgent).execute("ls");

        Command command = new RemoteCommand("ls", mockAgent, "test description");

        String result = command.execute();
        assertEquals(result, "test");
    }

    @Test
    public void testCommandWithTimeout() {
        doReturn("test").when(mockAgent).execute("ls", 123);

        Command command = new RemoteCommand("ls", mockAgent, "test description");

        String result = command.execute(123);
        assertEquals(result, "test");
    }

    @Test
    public void testCommandToString() {
        Command command = new RemoteCommand("ls", null, "test description");
        assertEquals(command.toString(), "'test description' command: 'ls'");
    }

    //@Test  TODO fix
    public void testCommandException() {
        doThrow(new AgentException("agent error")).when(mockAgent).execute("ls");

        Command command = new RemoteCommand("ls", mockAgent, "test description");

        try {
            command.execute(123);
        } catch(CommandException e) {
            assertEquals(e.getMessage(), "Remote command execution fail. Error: agent error");
            return;
        }

        fail("CommandException should be thrown.");
    }

    //@Test  TODO fix
    public void testCommandExceptionWithoutAgentErrorMessage() {
        doThrow(new AgentException()).when(mockAgent).execute("ls", 123);

        Command command = new RemoteCommand("ls", mockAgent, "test description");

        try {
            command.execute(123);
        } catch(CommandException e) {
            assertEquals(e.getMessage(), "Remote command execution fail.");
            return;
        }

        fail("CommandException should be thrown.");
    }
}
