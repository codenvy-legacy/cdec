/*
 *  2012-2016 Codenvy, S.A.
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
 * Modify /etc/hosts:
 *
 * 192.168.56.110 codenvy
 * 192.168.56.110 test.codenvy
 * 192.168.56.19 master.codenvy
 * 192.168.56.18 analytics.codenvy
 * 192.168.56.13 api.codenvy
 * 192.168.56.15 runner1.codenvy
 * 192.168.56.20 runner2.codenvy
 * 192.168.56.21 runner3.codenvy
 *
 * @author Anatoliy Bazko
 */
public class TestInstallationManagerCli {

    private static final Logger LOG = LoggerFactory.getLogger(TestInstallationManagerCli.class);

    private Path baseDir;

    @BeforeClass
    public void setUp() throws Exception {
        baseDir = Paths.get(getClass().getClassLoader().getResource("bin").getFile());

        doExecute(baseDir.toFile(), "chmod", "+x", "lib.sh");
        doExecute(baseDir.toFile(), "chmod", "+x", "config.sh");
    }

    @Test
    public void testInstallMultiNodeAndChangeConfig() throws Exception {
        doTest("im-install/test-install-multi-nodes-and-change-config.sh");
    }

    @Test
    public void testInstallExceptionCases() throws Exception {
        doTest("im-install/test-install-exception-cases.sh");
    }

    @Test
    public void testInstallSingleNodeAndChangeConfig() throws Exception {
        doTest("im-install/test-install-single-node-and-change-config.sh");
    }

    @Test
    public void testInstallUpdateImCliClient() throws Exception {
        doTest("im-install/test-install-update-im-cli-client.sh");
    }

    @Test
    public void testInstallSudoPasswordRequired() throws Exception {
        doTest("im-install/test-install-sudo-password-required.sh");
    }

    @Test
    public void testUpdateSingleNodeFromBinary() throws Exception {
        doTest("test-update-single-node-from-binary.sh");
    }

    @Test
    public void testUpdateMultiNodesFromBinary() throws Exception {
        doTest("test-update-multi-nodes-from-binary.sh");
    }

    @Test
    public void testDownloadAllUpdates() throws Exception {
        doTest("im-download/test-download-all-updates.sh");
    }

    @Test
    public void testUsingSystemProxy() throws Exception {
        doTest("im-download/test-download-using-system-proxy.sh");
    }

    @Test
    public void testAddRemoveCodenvyNodes() throws Exception {
        doTest("test-add-remove-codenvy-nodes.sh");
    }

    @Test
    public void testBackupRestoreSingleNode() throws Exception {
        doTest("test-backup-restore-single-node.sh");
    }

    @Test
    public void testLoginWithUsernameAndPassword() throws Exception {
        doTest("im-login/test-login.sh");
    }

    @Test
    public void testCheckOnpremisesSubscription() throws Exception {
        doTest("im-subscription/test-check-onpremises-subscription.sh");
    }

    @Test
    public void testHelpCommand() throws Exception {
        doTest("help/test-help.sh");
    }

    @Test
    public void testCheckImConfigCommand() throws Exception {
        doTest("im-config/test-config-check-im-config.sh");
    }

    @Test(priority = 9)
    public void testMigrationData() throws Exception {
        doTest("test-migration-data.sh");
    }

    @Test(priority = 10)
    public void testBackupRestoreMultiNodes() throws Exception {
        doTest("test-backup-restore-multi-nodes.sh");
    }

    @Test
    public void testVersionCommand() throws Exception {
        doTest("im-version/test-im-version.sh");
    }

    private void doTest(String testScript) throws Exception {
        doExecute(baseDir.toFile(), "chmod", "+x", testScript);
        doExecute(baseDir.toFile(), "./" + testScript);
    }

    private void doExecute(File directory, String... commands) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(directory);
        Process process = builder.start();

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
