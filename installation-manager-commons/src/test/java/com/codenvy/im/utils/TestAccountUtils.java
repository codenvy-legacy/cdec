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

import com.codenvy.im.user.UserCredentials;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        testCredentials = new UserCredentials("auth token", "7cd1aace299c-2a6a-9788-48da-560af433");
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

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = ".*" + AccountUtils.VALID_ACCOUNT_NOT_FOUND_ERROR + ".*")
    public void testSubscriptionWhenAccountWithInvalidRole() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"invalid-role\"],"
                                                                             + "accountReference:{id:\"" + testCredentials.getAccountId() + "\"}"
                                                                             + "}]");

        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*" + AccountUtils.ACCOUNT_NOT_FOUND_ERROR + ".*")
    public void testSubscriptionWhenAccountWithDifferentId() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:\"another-id\"}"
                                                                             + "}]");
        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*" + AccountUtils.ACCOUNT_NOT_FOUND_ERROR + ".*")
    public void testSubscriptionWhenAccountWithoutId() throws IOException {
        when(mockTransport.doGetRequest("/account", testCredentials.getToken())).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]"
                                                                             + "}]");
        AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, testCredentials);
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
}
