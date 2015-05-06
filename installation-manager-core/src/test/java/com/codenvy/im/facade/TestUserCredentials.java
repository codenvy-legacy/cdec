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
package com.codenvy.im.facade;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/** @author Dmytro Nochevnov */
public class TestUserCredentials {

    @Test
    public void testInstantiationWithTokenAndAccountId() {
        UserCredentials credentials = new UserCredentials("token", "id");

        assertEquals(credentials.getToken(), "token");
        assertEquals(credentials.getAccountId(), "id");
    }

    @Test
    public void testInstantiationWithToken() {
        UserCredentials credentials = new UserCredentials("token");

        assertEquals(credentials.getToken(), "token");
        assertNull(credentials.getAccountId());
    }

    @Test
    public void testNoArgInstantiation() {
        UserCredentials credentials = new UserCredentials();

        assertNull(credentials.getToken());
        assertNull(credentials.getAccountId());
    }

    @Test
    public void testClone() {
        UserCredentials credentials = new UserCredentials("token", "id");

        UserCredentials clone = credentials.clone();

        credentials.setToken("another");
        credentials.setAccountId("another");

        assertEquals(clone.getToken(), "token");
        assertEquals(clone.getAccountId(), "id");
    }
}
