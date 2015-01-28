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
package com.codenvy.im.agent;

import com.codenvy.im.console.Console;

import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.codenvy.im.console.TestConsole.ConsoleTested;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestLocalAgent {
    private static final String TEST_PASSWORD = "test";
    public static final String COMMAND_WITHOUT_SUDO = "sleep 1; ls;";
    public static final String COMMAND_WITH_SUDO = "sudo true";

    @Mock
    Console mockConsole;

    ProcessTested spyProcess;

    private ByteArrayOutputStream outputStreamOfSpyProcess;
    private ByteArrayInputStream  inputStreamOfSpyProcess;
    private ByteArrayInputStream  errorStreamOfSpyProcess;

    private String outputOfMockConsole;

    LocalAgentTested spyTestAgent;

    @BeforeMethod
    public void setupTestAgent() throws IOException, InterruptedException {
        outputStreamOfSpyProcess = new ByteArrayOutputStream();
        inputStreamOfSpyProcess = new ByteArrayInputStream(new byte[0]);
        errorStreamOfSpyProcess = new ByteArrayInputStream(new byte[0]);

        spyProcess = spy(new ProcessTested());
        spyTestAgent = spy(new LocalAgentTested());
    }

    @BeforeMethod
    public void setupConsole() throws IOException {
        mockConsole = mock(Console.class);

        doNothing().when(mockConsole).showProgressor();
        doNothing().when(mockConsole).hideProgressor();
        doNothing().when(mockConsole).restoreProgressor();

        outputOfMockConsole = "";
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                outputOfMockConsole += "\n";
                return null;
            }
        }).when(mockConsole).println();

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                outputOfMockConsole += invocation.getArguments()[0] + "\n";
                return null;
            }
        }).when(mockConsole).println(anyString());

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                outputOfMockConsole += invocation.getArguments()[0];
                return null;
            }
        }).when(mockConsole).print(anyString());
    }

    @Test
    public void testSimpleCommandSuccess() throws Exception {
        Agent agent = new LocalAgent();
        assertNotNull(agent.execute(COMMAND_WITHOUT_SUDO));
    }

    @Test(expectedExceptions = AgentException.class,
            expectedExceptionsMessageRegExp = ".*Output: ; Error: ls: cannot access unExisted_file: No such file or directory.")
    public void testSimpleCommandError() throws Exception {
        Agent agent = new LocalAgent();
        agent.execute("ls unExisted_file");
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't obtain correct password")
    public void testSudoCommandRequiredPasswordWithoutConsole() throws Exception {
        inputStreamOfSpyProcess = new ByteArrayInputStream(LocalAgent.NEED_PASSWORD_STATUS.getBytes());
        spyTestAgent.execute("sudo true;");
    }

    @Test
    public void testSudoCommandWithPassingPassword() throws Exception {
        String expectedConsoleOutput = "\n[sudo] password for " + System.getProperty("user.name") + ": ";

        doReturn(TEST_PASSWORD).when(mockConsole).readPassword();
        inputStreamOfSpyProcess = new ByteArrayInputStream(LocalAgent.NEED_PASSWORD_STATUS.getBytes());
        spyTestAgent.execute("sudo true;");
        assertEquals(outputOfMockConsole, expectedConsoleOutput);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't execute command '" + COMMAND_WITH_SUDO + "'. Output: mock output; Error: mock error.")
    public void testSudoCommandWithPassingPasswordError() throws Exception {
        doReturn(TEST_PASSWORD.toCharArray()).when(spyTestAgent).obtainPassword();
        doReturn(true).when(spyTestAgent).isPasswordRequired(COMMAND_WITH_SUDO);
        doReturn(-1).when(spyProcess).waitFor();
        inputStreamOfSpyProcess = new ByteArrayInputStream("mock output".getBytes());
        errorStreamOfSpyProcess = new ByteArrayInputStream("mock error".getBytes());
        spyTestAgent.execute(COMMAND_WITH_SUDO);
    }

    @Test
    public void testIsPasswordRequiredOnCommandWithoutSudo() {
        assertFalse(spyTestAgent.isPasswordRequired(COMMAND_WITHOUT_SUDO));
        verify(spyTestAgent, never()).getProcess(LocalAgent.CHECK_PASSWORD_NECESSITY_COMMAND);
    }

    @Test
    public void testIsPasswordRequiredOnCommandWithSudoWhenProcessReturnsEmptyResult() {
        assertFalse(spyTestAgent.isPasswordRequired(COMMAND_WITH_SUDO));
        verify(spyTestAgent).getProcess(LocalAgent.CHECK_PASSWORD_NECESSITY_COMMAND);
    }

    @Test
    public void testIsPasswordRequiredOnCommandWithSudoWhenProcessReturnsCorrectStatus() {
        inputStreamOfSpyProcess = new ByteArrayInputStream(LocalAgent.NEED_PASSWORD_STATUS.getBytes());
        assertTrue(spyTestAgent.isPasswordRequired(COMMAND_WITH_SUDO));
        verify(spyTestAgent).getProcess(LocalAgent.CHECK_PASSWORD_NECESSITY_COMMAND);
    }

    @Test
    public void testIsPasswordRequiredOnCommandWithSudoWhenProcessThrowsException() throws InterruptedException {
        doThrow(InterruptedException.class).when(spyProcess).waitFor();
        assertFalse(spyTestAgent.isPasswordRequired(COMMAND_WITH_SUDO));
        verify(spyTestAgent).getProcess(LocalAgent.CHECK_PASSWORD_NECESSITY_COMMAND);
    }

    @Test
    public void testPassPasswordToProcess() throws InterruptedException {
        spyTestAgent.passPasswordToProcess(spyProcess, TEST_PASSWORD.toCharArray());
        assertEquals(outputStreamOfSpyProcess.toString(), TEST_PASSWORD + "\n");
    }

    @Test
    public void testIsPasswordCorrectTrue() throws Exception {
        errorStreamOfSpyProcess = new ByteArrayInputStream(new byte[] {-1});
        assertTrue(spyTestAgent.isPasswordCorrect(TEST_PASSWORD.toCharArray()));
        verify(spyTestAgent).getProcess(LocalAgent.CHECK_IS_PASSWORD_CORRECT_COMMAND);
    }

    @Test
    public void testIsPasswordCorrectFalse() throws Exception {
        errorStreamOfSpyProcess = new ByteArrayInputStream(LocalAgent.PASSWORD_INCORRECT_STATUS.getBytes());
        assertFalse(spyTestAgent.isPasswordCorrect(TEST_PASSWORD.toCharArray()));
        verify(spyTestAgent).getProcess(LocalAgent.CHECK_IS_PASSWORD_CORRECT_COMMAND);
    }

    @Test(enabled = false) // TODO [AB rework initialization]
    public void testGetConsole() throws IOException {
        LocalAgent agent = new LocalAgent();
        try {
            agent.getConsole();
        } catch(AgentException e) {
            assertEquals(e.getMessage(), "Can't obtain correct password");

            Console console = ConsoleTested.create(true);
            assertEquals(agent.getConsole(), console);
            return;
        }

        fail("Here should be an AgentException");
    }

    @Test
    public void testGetProcess() throws IOException {
        LocalAgent agent = new LocalAgent();
        assertNotNull(agent.getProcess(COMMAND_WITHOUT_SUDO));
    }

    @Test
    public void testObtainPassword() throws Exception {
        String expectedConsoleOutput = "\n[sudo] password for " + System.getProperty("user.name") + ": ";
        doReturn(TEST_PASSWORD).when(mockConsole).readPassword();

        assertEquals(new String(spyTestAgent.obtainPassword()), TEST_PASSWORD);
        assertEquals(outputOfMockConsole, expectedConsoleOutput);
        verify(spyTestAgent).isPasswordCorrect(TEST_PASSWORD.toCharArray());
        verify(mockConsole).hideProgressor();
        verify(mockConsole).showProgressor();
        verify(mockConsole).restoreProgressor();
    }

    @Test
    public void testObtainPasswordCorrectOnSecondTry() throws Exception {
        String expectedConsoleOutput = String.format("\n[sudo] password for %1$s: " +
                                                     "\nSorry, try again." +
                                                     "\n[sudo] password for %1$s: " +
                                                     "\n", System.getProperty("user.name"));

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                outputOfMockConsole += "\n";
                return TEST_PASSWORD;
            }
        }).when(mockConsole).readPassword();

        errorStreamOfSpyProcess = new ByteArrayInputStream(LocalAgent.PASSWORD_INCORRECT_STATUS.getBytes());

        assertEquals(new String(spyTestAgent.obtainPassword()), TEST_PASSWORD);
        assertEquals(outputOfMockConsole, expectedConsoleOutput);
        verify(spyTestAgent, times(2)).isPasswordCorrect(TEST_PASSWORD.toCharArray());
        verify(mockConsole).hideProgressor();
        verify(mockConsole).showProgressor();
        verify(mockConsole).restoreProgressor();
    }

    @Test
    public void testFailObtainPasswordCorrectOnSecondTry() throws Exception {
        String expectedConsoleOutput = String.format("\n[sudo] password for %1$s: " +
                                                     "\nSorry, try again." +
                                                     "\n[sudo] password for %1$s: " +
                                                     "\nSorry, try again." +
                                                     "\n[sudo] password for %1$s: " +
                                                     "\nSorry, try again." +
                                                     "\nsudo: 3 incorrect password attempts\n", System.getProperty("user.name"));

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                outputOfMockConsole += "\n";
                return TEST_PASSWORD;
            }
        }).when(mockConsole).readPassword();

        doReturn(false).when(spyTestAgent).isPasswordCorrect(TEST_PASSWORD.toCharArray());

        try {
            spyTestAgent.obtainPassword();
        } catch(AgentException e) {
            assertEquals(e.getMessage(), "Can't obtain correct password");

            assertEquals(outputOfMockConsole, expectedConsoleOutput);
            verify(spyTestAgent, times(3)).isPasswordCorrect(TEST_PASSWORD.toCharArray());
            verify(mockConsole).hideProgressor();
            verify(mockConsole).showProgressor();
            verify(mockConsole, never()).restoreProgressor();
            return;
        }

        fail("Here should be AgentException.");
    }

    class LocalAgentTested extends LocalAgent {
        @Override protected Process getProcess(String command) {
            return spyProcess;
        }

        @Override protected Console getConsole() throws AgentException {
            return mockConsole;
        }
    }

    class ProcessTested extends Process {
        @Override public OutputStream getOutputStream() {
            return outputStreamOfSpyProcess;
        }

        @Override public InputStream getInputStream() {
            return inputStreamOfSpyProcess;
        }

        @Override public InputStream getErrorStream() {
            return errorStreamOfSpyProcess;
        }

        @Override public int waitFor() throws InterruptedException {
            return 0;
        }

        @Override public int exitValue() {
            return 0;
        }

        @Override public void destroy() {
        }
    }
}
