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
package com.codenvy.im;

import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.InjectorBootstrap;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestIsValidAccountIdServiceImpl {
    public static final String TEST_ACCOUNT_ID = "accountId";
    public static final String TEST_AUTH_TOKEN = "auth token";
    private InstallationManagerService installationManagerService;
    private String                     accountApiEndpoint;

    @Mock
    private HttpTransport transport;

    @Mock
    private InstallationManager mockInstallationManager;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, transport, new DownloadDescriptorHolder());
        accountApiEndpoint = combinePaths(InjectorBootstrap.getProperty("api.endpoint"), "account");
    }

    @Test
    public void testIsValidAccountId() throws Exception {
        UserCredentials testCredentials = new UserCredentials("auth token", TEST_ACCOUNT_ID);
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
            .thenReturn("[{"
                        + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                        + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\"}"
                        + "}]");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        assertTrue(installationManagerService.isValidAccountId(userCredentialsRep));
    }

    @Test
    public void testIsValidAccountIdFromSeveral() throws Exception {
        UserCredentials testCredentials = new UserCredentials("auth token", TEST_ACCOUNT_ID);

        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
            .thenReturn("[{"
                        + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                        + "accountReference:{id:\"another-account-id\"}"
                        + "},{roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                        + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\"}"
                        + "}]");
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        assertTrue(installationManagerService.isValidAccountId(userCredentialsRep));
    }

    @Test
    public void testIsValidAccountIdFromEmpty() throws Exception {
        UserCredentials testCredentials = new UserCredentials(TEST_AUTH_TOKEN, TEST_ACCOUNT_ID);
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
            .thenReturn("[{"
                        + "roles:[\"account/member\"],"
                        + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\"}"
                        + "}]");
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        assertFalse(installationManagerService.isValidAccountId(userCredentialsRep));
    }

    @Test(expectedExceptions = IOException.class)
    public void testIsValidAccountIdErrorIfAuthenticationFailed() throws Exception {
        UserCredentials testCredentials = new UserCredentials(TEST_AUTH_TOKEN, TEST_ACCOUNT_ID);
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken())).thenThrow(new AuthenticationException());
        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(testCredentials);
        
        installationManagerService.isValidAccountId(userCredentialsRep);
    }
}
