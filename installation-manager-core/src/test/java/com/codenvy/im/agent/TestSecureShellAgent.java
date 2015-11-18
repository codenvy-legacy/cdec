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

import com.codenvy.im.testhelper.ssh.SshServerFactory;
import org.apache.sshd.SshServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class TestSecureShellAgent {

    private static final String TEST_COMMAND        = "echo test";
    private static final String TEST_COMMAND_OUTPUT = "test";

    private static final String UNEXISTED_HOST = "unexisted";

    private SshServer sshd;
    private SecureShellAgent testAgent;

    @BeforeClass
    public void setUp() throws IOException {
        sshd = SshServerFactory.createSshd();
        sshd.start();
    }

    @Test
    public void testAuthKey() throws Exception {
        testAgent = new SecureShellAgent(SshServerFactory.TEST_SSH_HOST, sshd.getPort(), SshServerFactory.TEST_SSH_USER, SshServerFactory.TEST_SSH_AUTH_PRIVATE_KEY);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class)
    public void testAuthKeyError() throws Exception {
        testAgent = new SecureShellAgent(UNEXISTED_HOST, sshd.getPort(), SshServerFactory.TEST_SSH_USER, SshServerFactory.TEST_SSH_AUTH_PRIVATE_KEY);

        String result = testAgent.execute(TEST_COMMAND);
        assertEquals(result, TEST_COMMAND_OUTPUT);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = ".* Output: ; Error: ls: cannot access unExisted_file: No such file or directory.")
    public void testErrorOnCommandExecution() throws Exception {
        testAgent = new SecureShellAgent(SshServerFactory.TEST_SSH_HOST, sshd.getPort(), SshServerFactory.TEST_SSH_USER, SshServerFactory.TEST_SSH_AUTH_PRIVATE_KEY);
        testAgent.execute("ls unExisted_file");
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        sshd.stop();
    }
}
