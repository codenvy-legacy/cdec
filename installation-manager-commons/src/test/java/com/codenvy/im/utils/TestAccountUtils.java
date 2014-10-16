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
package com.codenvy.im.utils;

import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.user.UserCredentials;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestAccountUtils {

    private HttpTransport mockTransport;
    private final static String VALID_SUBSCRIPTION = "OnPremises";
    private UserCredentials testCredentials;
    
    @BeforeMethod
    public void setup() {
        mockTransport = mock(HttpTransport.class);
        testCredentials = new UserCredentials("auth token", "accountId");
    }

    @Test
    public void testValidSubscriptionByAccountReference() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:\"another-id\"}"
                                                                             + "},{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                                                                             + "}]");
        when(mockTransport.doGetRequest("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
        .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + "}]");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testGetAccountIdWhereUserIsOwner() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                            + "roles:[\"account/member\"],"
                                                                                            + "accountReference:{id:\"member-id\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"another-id\"}"
                                                                                            + "}]");

        assertEquals(AccountUtils.getAccountIdWhereUserIsOwner(mockTransport, "", testCredentials.getToken()), testCredentials.getAccountId());
    }

    @Test
    public void testValidateAccountIdTrue() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                            + "roles:[\"account/member\"],"
                                                                                            + "accountReference:{id:\"member-id\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"" +
                                                                                            testCredentials.getAccountId() + "\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"another-id\"}"
                                                                                            + "}]");

        assertTrue(AccountUtils.isValidAccountId(mockTransport, "", testCredentials));
    }

    @Test
    public void testValidateAccountIdFalseNoValidAccountId() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                            + "roles:[\"account/member\"],"
                                                                                            + "accountReference:{id:\"member-id\"}"
                                                                                            + "},{"
                                                                                            + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                                            + "accountReference:{id:\"another-id\"}"
                                                                                            + "}]");

        assertFalse(AccountUtils.isValidAccountId(mockTransport, "", testCredentials));
    }

    @Test
    public void testValidateAccountIdFalseNoValidRoles() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                                            + "roles:[\"account/member\"],"
                                                                                            + "accountReference:{id:\"" +
                                                                                            testCredentials.getAccountId() + "\"}"
                                                                                            + "}]");

        assertFalse(AccountUtils.isValidAccountId(mockTransport, "", testCredentials));
    }

    @Test
    public void testValidSubscriptionByLink() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                + "links:[{\"rel\":\"subscriptions\",\"href\":\"/account/"
                                                                + testCredentials.getAccountId()
                                                                + "/subscriptions\"}],"
                                                                + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]"
                                                                + "}]");
        when(mockTransport.doGetRequest("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
        .thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + "}]");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));
    }

    @Test
    public void testInvalidSubscription() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                                                                             + "}]");
        when(mockTransport.doGetRequest("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken()))
        .thenReturn("[{serviceId:invalid}]");
        
        assertFalse(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials));        
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testInvalidAuthentication() throws IOException {
        doThrow(new HttpException(403, "auth error"))
            .when(mockTransport)
            .doGetRequest("/account/" + testCredentials.getAccountId() + "/subscriptions", testCredentials.getToken());

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }
}
