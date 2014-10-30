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
package com.codenvy.im.utils;

import com.codenvy.im.agent.SecureShellAgent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class TestSecureShellAgent {
    SecureShellAgent ssh;

    //    @Test
    public void testUserPasswd() throws Exception {
        ssh = new SecureShellAgent("127.0.0.1", 2222, "vagrant", "vagrant");
        String result = ssh.execute("ls");
        result += ssh.execute("ls");

        System.out.println(result);
    }

    //    @Test
    public void testAuthKey() throws IOException {
        ssh = new SecureShellAgent("127.0.0.1", 2222, "vagrant", "~/.ssh/id_rsa", null);

        String result = ssh.execute("ls src");

        System.out.println(result);
    }

    //    @Test
    public void testErrorOnCommandExecution() throws IOException {
        ssh = new SecureShellAgent("127.0.0.1", 2222, "vagrant", "~/.ssh/id_rsa", null);

        String result = ssh.execute("unknown_command");

        System.out.println(result);
    }

    //    @AfterMethod
    public void tearDown() {
        ssh.disconnect();
    }
}
