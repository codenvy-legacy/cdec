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
package com.codenvy.im.service;

import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.request.Request;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetAccountIdServiceImpl {
    public static final String TEST_ACCOUNT_ID = "accountId";
    public static final String ACCOUNT_NAME    = "accountName";
    private InstallationManagerService installationManagerService;
    private final UserCredentials testCredentials = new UserCredentials("auth token", null);
    private String  accountApiEndpoint;
    private Request request;

    @Mock
    private HttpTransport transport;

    @Mock
    private InstallationManager mockInstallationManager;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        installationManagerService = new InstallationManagerServiceImpl("update/endpoint", "api/endpoint", mockInstallationManager, transport);
        accountApiEndpoint = combinePaths("api/endpoint", "account");
        request = new Request().setUserCredentials(testCredentials);
    }

    @Test
    public void testGetAccountReference() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "}]");
        String response = installationManagerService.getAccountReferenceWhereUserIsOwner(null, request);
        assertEquals(response, "{\n"
                               + "  \"links\" : [ ],\n"
                               + "  \"name\" : \"" + ACCOUNT_NAME + "\",\n"
                               + "  \"id\" : \"" + TEST_ACCOUNT_ID + "\"\n"
                               + "}");
    }

    @Test
    public void testGetAccountReferenceFromSeveral() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "},{roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"another-account-id\"}"
                            + "}]");
        String response = installationManagerService.getAccountReferenceWhereUserIsOwner(null, request);
        assertEquals(response, "{\n"
                               + "  \"links\" : [ ],\n"
                               + "  \"name\" : \"" + ACCOUNT_NAME + "\",\n"
                               + "  \"id\" : \"" + TEST_ACCOUNT_ID + "\"\n"
                               + "}");
    }

    @Test
    public void testGetAccountReferenceReturnNullIfAccountWasNotFound() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"account/member\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\"}"
                            + "}]");
        String response = installationManagerService.getAccountReferenceWhereUserIsOwner(null, request);
        assertNull(response);
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetAccountReferenceErrorIfAuthenticationFailed() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken())).thenThrow(new AuthenticationException());

        installationManagerService.getAccountReferenceWhereUserIsOwner(null, request);
    }

    @Test
    public void testGetAccountReferenceWithSpecificName() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "}]");
        String response = installationManagerService.getAccountReferenceWhereUserIsOwner(ACCOUNT_NAME, request);
        assertEquals(response, "{\n"
                               + "  \"links\" : [ ],\n"
                               + "  \"name\" : \"" + ACCOUNT_NAME + "\",\n"
                               + "  \"id\" : \"" + TEST_ACCOUNT_ID + "\"\n"
                               + "}");
    }

    @Test
    public void testGetAccountReferenceWithSpecificNameReturnNullIfAccountWasNotFound() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "}]");
        String response = installationManagerService.getAccountReferenceWhereUserIsOwner("some name", request);
        assertNull(response);
    }

}
