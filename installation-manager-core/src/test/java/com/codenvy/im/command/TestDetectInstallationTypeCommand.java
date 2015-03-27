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
package com.codenvy.im.command;

import com.codenvy.im.BaseTest;
import com.codenvy.im.install.InstallType;

import org.testng.annotations.Test;

import static com.codenvy.im.command.DetectInstallationTypeCommand.detectInstallationType;
import static org.testng.Assert.assertEquals;

public class TestDetectInstallationTypeCommand extends BaseTest {

    @Test
    public void shouldReturnMultiIfFileAbsent() throws Exception {
        assertEquals(detectInstallationType(), InstallType.CODENVY_MULTI_SERVER);
    }

    @Test
    public void shouldReturnMultiIfSectionAbsent() throws Exception {
        prepareConfForMultiNodeInstallation();
        assertEquals(detectInstallationType(), InstallType.CODENVY_MULTI_SERVER);
    }

    @Test
    public void shouldReturnSingleIfSectionExist() throws Exception {
        prepareConfForSingleNodeInstallation();
        assertEquals(detectInstallationType(), InstallType.CODENVY_SINGLE_SERVER);
    }
}