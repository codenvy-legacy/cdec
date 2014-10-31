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

import com.jcraft.jsch.JSchException;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class SecureShellAgentTest {
    SshServer spySshd;

    SecureShellAgent testAgent;

    static final String TEST_USER             = "vagrant";
    static final String TEST_HOST             = "127.0.0.1";
    static final int    TEST_PORT             = 2223;
    static final String TEST_PASSWORD         = "vagrant";
    static final String TEST_AUTH_PRIVATE_KEY = "~/.ssh/id_rsa";
    static final String TEST_PASSPHRASE       = null;

    static final String TEST_COMMAND          = "echo test";
    static final String TEST_COMMAND_OUTPUT   = "test\n";

    static final String INVALID_COMMAND        = "ls unexists_file";
    static final String INVALID_COMMAND_OUTPUT = "ls: cannot access unexists_file: No such file or directory\n";

    static final String UNEXISTS_HOST = "unexists";

    @BeforeTest
    public void setUp() throws JSchException, IOException {
        spySshd = getSpySshd();
        spySshd.start();
    }

    @Test
    public void testUserPasswd() {
        testAgent = new SecureShellAgent(TEST_HOST, TEST_PORT, TEST_USER, TEST_PASSWORD);

        String result = testAgent.execute(TEST_COMMAND);
        result += testAgent.execute(TEST_COMMAND);

        assertEquals(result, TEST_COMMAND_OUTPUT + TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't connect to host '" + TEST_USER + "@" + UNEXISTS_HOST + ":" + TEST_PORT
                                            + "'. Error: java.net.UnknownHostException: " + UNEXISTS_HOST)
    public void testUserPasswdError() {
        testAgent = new SecureShellAgent("unexists", TEST_PORT, TEST_USER, TEST_PASSWORD);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test
    public void testAuthKey() {
        testAgent = new SecureShellAgent(TEST_HOST, TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY, TEST_PASSPHRASE);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't connect to host '" + TEST_USER + "@" + UNEXISTS_HOST + ":" + TEST_PORT
                                            + "' by using private key '" + TEST_AUTH_PRIVATE_KEY
                                            + "'. Error: java.net.UnknownHostException: " + UNEXISTS_HOST)
    public void testAuthKeyError() {
        testAgent = new SecureShellAgent("unexists", TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY, TEST_PASSPHRASE);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Command '" + INVALID_COMMAND + "' execution fail. Error: " + INVALID_COMMAND_OUTPUT)
    public void testErrorOnCommandExecution() {
        testAgent = new SecureShellAgent(TEST_HOST, TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY, TEST_PASSPHRASE);
        testAgent.execute(INVALID_COMMAND);
    }

    @AfterTest
    public void tearDown() throws InterruptedException {
        testAgent.disconnect();
        spySshd.stop();
    }

    private SshServer getSpySshd() {
        SshServer sshdSpy = spy(SshServer.setUpDefaultServer());
        sshdSpy.setPort(TEST_PORT);

        sshdSpy.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        sshdSpy.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                return true;
            }
        });

        sshdSpy.setCommandFactory(new CommandFactory() {
            public Command createCommand(String command) {
                return new ProcessShellFactory(command.split(" ")).create();
            }
        });

        return sshdSpy;
    }
}
