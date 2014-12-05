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

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Anatoliy Bazko
 */
public class TestLocalAgent {
    @Test
    public void testSuccessResult() throws Exception {
        Agent agent = new LocalAgent();
        assertNotNull(agent.execute("sleep 1; ls;"));
    }

    @Test(expectedExceptions = AgentException.class,
            expectedExceptionsMessageRegExp = "Can't execute command 'ls unExisted_file'. " +
                                              "Error: ls: cannot access unExisted_file: No such file or directory\n")
    public void testError() throws Exception {
        Agent agent = new LocalAgent();
        agent.execute("ls unExisted_file");
    }
}
