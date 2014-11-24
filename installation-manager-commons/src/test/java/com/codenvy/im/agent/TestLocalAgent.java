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

import com.codenvy.im.command.CommandException;
import com.codenvy.im.command.SimpleCommand;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

/**
 * @author Anatoliy Bazko
 */
public class TestLocalAgent {
    @Test
    public void testSuccessResult() throws Exception {
        SimpleCommand command = new SimpleCommand("ls", new LocalAgent(), "simple command");
        assertNotNull(command.execute());
    }

    @Test(expectedExceptions = CommandException.class,
            expectedExceptionsMessageRegExp = "Remote command execution fail. Error: Can't execute command 'ls d' " +
                                              "Error: ls: cannot access d: No such file or directory\n")
    public void testError() throws Exception {
        SimpleCommand command = new SimpleCommand("ls d", new LocalAgent(), "simple command");
        command.execute();
    }
}
