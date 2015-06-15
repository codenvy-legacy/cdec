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
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class TestSecureShellAgent {

    private static final String TEST_USER             = "testUser";
    private static final String TEST_HOST             = "127.0.0.1";
    private static final int    TEST_PORT             = 2223;
    private static final Path   TEST_AUTH_PRIVATE_KEY = Paths.get(TestSecureShellAgent.class.getClassLoader().getResource("../test-classes/test_rsa").getFile());

    private static final String TEST_COMMAND        = "echo test";
    private static final String TEST_COMMAND_OUTPUT = "test";

    private static final String UNEXISTED_HOST = "unexisted";

    private SshServer        spySshd;
    private SecureShellAgent testAgent;

    @BeforeTest
    public void setUp() throws JSchException, IOException {
        spySshd = getSpySshd();
        spySshd.start();
    }

    @Test
    public void testAuthKey() throws Exception {
        testAgent = new SecureShellAgent(TEST_HOST, TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class)
    public void testAuthKeyError() throws Exception {
        testAgent = new SecureShellAgent(UNEXISTED_HOST, TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = ".* Output: ; Error: ls: cannot access unExisted_file: No such file or directory.")
    public void testErrorOnCommandExecution() throws Exception {
        testAgent = new SecureShellAgent(TEST_HOST, TEST_PORT, TEST_USER, TEST_AUTH_PRIVATE_KEY);
        testAgent.execute("ls unExisted_file");
    }

    @AfterTest
    public void tearDown() throws InterruptedException {
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
