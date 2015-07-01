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
package com.codenvy.im.managers;

import com.codenvy.im.BaseTest;
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author Anatoliy Bazko
 */
public class PasswordManagerTest extends BaseTest {

    @Mock
    private HttpTransport transport;
    @Mock
    private ConfigManager configManager;

    private PasswordManager passwordManager;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        passwordManager = spy(new PasswordManager(configManager, transport));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionIfPasswordValidationFailed() throws Exception {
        byte[] curPwd = "curPwd".getBytes("UTF-8");
        byte[] newPwd = "newPwd".getBytes("UTF-8");
        doThrow(new IllegalStateException()).when(passwordManager).validateCurrentPassword(eq(curPwd), any(Config.class));

        passwordManager.changeAdminPassword(curPwd, newPwd);
    }
}