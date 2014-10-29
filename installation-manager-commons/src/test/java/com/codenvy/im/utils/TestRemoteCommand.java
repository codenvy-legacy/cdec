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

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.SecureShellAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.RemoteCommand;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.PuppetConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class TestRemoteCommand {
    SecureShellAgent agent;
    CdecConfig cdec;


//    @BeforeTest
    public void setUp() {
        agent = new SecureShellAgent("127.0.0.1", 2222, "vagrant", "~/.ssh/id_rsa", null);
        cdec = new CdecConfig();
    }

//    @Test
    public void testCommand() {

        final PuppetConfig config = cdec.getPuppetClient();

        final Agent agent = new SecureShellAgent(
            config.getHost(),
            Integer.valueOf(config.getSSHPort()),
            config.getUser(),
            config.getPrivateKeyFileAbsolutePath(),
            null
        );

        Command command = new RemoteCommand("ls", agent, "test");

        String result = command.execute();
        assertEquals(result, "");  // TODO
    }

//    @Test
    public void testCommandWithParameters() {

        final PuppetConfig config = cdec.getPuppetClient();

        final Agent agent = new SecureShellAgent(
            config.getHost(),
            Integer.valueOf(config.getSSHPort()),
            config.getUser(),
            config.getPrivateKeyFileAbsolutePath(),
            null
        );

        Command command = new RemoteCommand("ls $TEST_DIR", agent, "test parameter", new HashMap<String, String>() {{
            put("TEST", "/etc");
        }});

        String result = command.execute();
        assertEquals(result, "");  // TODO
    }
}
