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

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class TestLocalAsyncAgent {
    public final static Path TEST_FILE_PATH = Paths.get(TestLocalAsyncAgent.class.getClassLoader().getResource("../test-classes/").getPath()).resolve("test.txt");

    @AfterClass
    public void deleteFile() throws IOException {
        Files.delete(TEST_FILE_PATH);
    }

    @Test
    public void testAsyncResult() throws Exception {
        Agent agent = new LocalAsyncAgent();
        assertNull(agent.execute("sleep 1; echo 'test' >> " + TEST_FILE_PATH.toAbsolutePath()));
        Thread.sleep(1500);  // waiting for creating test file
        assertTrue(Files.exists(TEST_FILE_PATH));
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't execute command 'null'.")
    public void testError() throws Exception {
        Agent agent = new LocalAsyncAgent();
        agent.execute(null);
        Thread.sleep(100);
    }
}
