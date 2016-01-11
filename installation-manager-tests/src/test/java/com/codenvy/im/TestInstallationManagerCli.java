/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
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


import org.testng.annotations.Test;

/** @author Anatoliy Bazko */
public class TestInstallationManagerCli extends BaseTest{
    
    @Test
    public void testInstallExceptionCases() throws Exception {
        doTest("im-install/test-install-exception-cases.sh");
    }

    @Test
    public void testInstallUpdateImCliClient() throws Exception {
        doTest("im-install/test-install-update-im-cli-client.sh");
    }

    @Test
    public void testLoginWithUsernameAndPassword() throws Exception {
        doTest("im-login/test-login.sh");
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
    public void testHelpCommand() throws Exception {
        doTest("help/test-help.sh");
    }

    @Test
    public void testCheckImConfigCommand() throws Exception {
        doTest("im-config/test-config-check-im-config.sh");
    }

}
