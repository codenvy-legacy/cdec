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

package com.codenvy.im;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestInstallationManagerCli {

    private static final Logger LOG = LoggerFactory.getLogger(TestInstallationManagerCli.class);

    private Path dir;

    @BeforeClass
    public void setUp() throws Exception {
        dir = Paths.get(getClass().getClassLoader().getResource("bin").getFile());

        doExecute(dir.toFile(), "chmod", "+x", "lib.sh");
        doExecute(dir.toFile(), "chmod", "+x", "config.sh");
    }

    @Test
    public void testInstallSingleNodeAndChangePassword() throws Exception {
        doTest("test-install-single-node-and-change-password.sh");
    }

    @Test
    public void testUpdateSingleNode() throws Exception {
        doTest("test-update-single-node.sh");
    }

    private void doTest(String testScript) throws Exception {
        doExecute(dir.toFile(), "chmod", "+x", testScript);
        doExecute(dir.toFile(), "./" + testScript);
    }

    private void doExecute(File directory, String... commands) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(directory);
        Process process = builder.start();
        process.waitFor();

        int exitCode = process.waitFor();

        String output = IOUtils.toString(process.getInputStream());
        if (!output.trim().isEmpty()) {
            LOG.info(output);
        }

        output = IOUtils.toString(process.getErrorStream());
        if (!output.trim().isEmpty()) {
            LOG.error(output);
        }

        assertEquals(exitCode, 0);
    }
}
