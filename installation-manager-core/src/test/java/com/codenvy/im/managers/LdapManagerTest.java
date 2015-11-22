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

import com.codenvy.im.testhelper.ldap.BaseLdapTest;
import com.codenvy.im.utils.HttpTransport;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class LdapManagerTest extends BaseLdapTest {

    @Mock
    private HttpTransport mockTransport;
    @Mock
    private ConfigManager mockConfigManager;

    private LdapManager spyLdapManager;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        spyLdapManager = spy(new LdapManager(mockConfigManager, mockTransport));
    }

    @Test
    public void shouldChangeAdminPasswordOnSingleNode() throws Exception {
        prepareSingleNodeEnv(mockConfigManager, mockTransport);

        byte[] curPwd = "curPwd".getBytes("UTF-8");
        byte[] newPwd = "newPwd".getBytes("UTF-8");
        doNothing().when(spyLdapManager).validateCurrentPassword(eq(curPwd), any(Config.class));

        spyLdapManager.changeAdminPassword(curPwd, newPwd);
        // TODO [ndp] get admin password from ldap to verify it

    }

//    @Test  TODO [ndp] uncomment
    public void shouldChangeAdminPasswordOnMultiNode() throws Exception {
        prepareMultiNodeEnv(mockConfigManager, mockTransport);

        byte[] curPwd = "curPwd".getBytes("UTF-8");
        byte[] newPwd = "newPwd".getBytes("UTF-8");
        doNothing().when(spyLdapManager).validateCurrentPassword(eq(curPwd), any(Config.class));

        spyLdapManager.changeAdminPassword(curPwd, newPwd);

        // TODO [ndp] get admin password from ldap to verify it


        verify(spyLdapManager).validateCurrentPassword(eq(curPwd), any(Config.class));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionIfPasswordValidationFailed() throws Exception {
        byte[] curPwd = "curPwd".getBytes("UTF-8");
        byte[] newPwd = "newPwd".getBytes("UTF-8");
        doThrow(new IllegalStateException()).when(spyLdapManager).validateCurrentPassword(eq(curPwd), any(Config.class));

        spyLdapManager.changeAdminPassword(curPwd, newPwd);
    }

    @Test
    public void shouldReturnNumberOfUsersOnSingleNode() throws Exception {
        prepareSingleNodeEnv(mockConfigManager, mockTransport);

        assertEquals(spyLdapManager.getNumberOfUsers(), 2);
    }

    @Test
    public void shouldReturnNumberOfUsersOnMultiNode() throws Exception {
        prepareMultiNodeEnv(mockConfigManager, mockTransport);

        // TODO [ndp]
    }

}