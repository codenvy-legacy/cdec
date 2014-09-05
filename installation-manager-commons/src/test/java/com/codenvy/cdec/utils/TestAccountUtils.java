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
package com.codenvy.cdec.utils;

import com.codenvy.api.account.shared.dto.MemberDescriptor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * @author Dmytro Nochevnov
 */
public class TestAccountUtils {

    private HttpTransport mockTransport;
    private final static String VALID_SUBSCRIPTION = "On-Premises";
    
    @BeforeMethod
    public void setup() {
        mockTransport = mock(HttpTransport.class);
    }

    @Test
    public void testGetProperAccount() throws IOException {
        when(mockTransport.doGetRequest(anyString(), anyString())).thenReturn("[{roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]}]");

        MemberDescriptor account = AccountUtils.getAccountWithProperRole(mockTransport, "apiEndpoint", AccountUtils.ACCOUNT_OWNER_ROLE, "authToken");
        assertNotNull(account);
    }
    
    @Test
    public void testGetInvalidAccount() throws IOException {
        when(mockTransport.doGetRequest(anyString(), anyString())).thenReturn("[{roles:[\"invalid-role\"]}]");
        assertNull(AccountUtils.getAccountWithProperRole(mockTransport, "apiEndpoint", AccountUtils.ACCOUNT_OWNER_ROLE, "authToken"));
    }
    
    @Test
    public void testValidSubscriptionByLink() throws IOException {
        when(mockTransport.doGetRequest("/account", "authToken")).thenReturn("[{"
                                                                + "links:[{\"rel\":\"subscriptions\",\"href\":\"/account/accountId/subscriptions\"}],"
                                                                + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]"
                                                                + "}]");
        when(mockTransport.doGetRequest("/account/accountId/subscriptions", "authToken")).thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + "}]");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, "authToken"));        
    }

    @Test
    public void testValidSubscriptionByAccountId() throws IOException {
        when(mockTransport.doGetRequest("/account", "authToken")).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:accountId}"
                                                                             + "}]");
        when(mockTransport.doGetRequest("/account/accountId/subscriptions", "authToken")).thenReturn("[{serviceId:" + VALID_SUBSCRIPTION + "}]");
        
        assertTrue(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, "authToken"));
    }
    
    @Test
    public void testInvalidSubscription() throws IOException {
        when(mockTransport.doGetRequest("/account", "authToken")).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"],"
                                                                             + "accountReference:{id:accountId}"
                                                                             + "}]");
        when(mockTransport.doGetRequest("/account/accountId/subscriptions", "authToken")).thenReturn("[{serviceId:invalid}]");
        
        assertFalse(AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, "authToken"));        
    }
    
    @Test
    public void testInvalidAccount() throws IOException {
        when(mockTransport.doGetRequest("/account", "authToken")).thenReturn("[{"
                                                                             + "roles:[\"invalid\"],"                                                    
                                                                             + "accountReference:{id:accountId}"
                                                                             + "}]");
        
        try {
            AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, "authToken");
        } catch(Exception e) {
            assertEquals(e.getClass().getCanonicalName(), IllegalStateException.class.getCanonicalName());
            assertEquals(e.getMessage(), AccountUtils.VALID_ACCOUNT_NOT_FOUND_ERROR);
        }
    }

    @Test
    public void testAbsentSubscriptionsLink() throws IOException {
        when(mockTransport.doGetRequest("/account", "authToken")).thenReturn("[{"
                                                                             + "roles:[\"" + AccountUtils.ACCOUNT_OWNER_ROLE + "\"]"
                                                                             + "}]");
        try {
            AccountUtils.isValidSubscription(mockTransport, "", VALID_SUBSCRIPTION, "authToken");
        } catch(Exception e) {
            assertEquals(e.getClass().getCanonicalName(), IllegalStateException.class.getCanonicalName());
            assertEquals(e.getMessage(), AccountUtils.PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR);
        }
    }
}
