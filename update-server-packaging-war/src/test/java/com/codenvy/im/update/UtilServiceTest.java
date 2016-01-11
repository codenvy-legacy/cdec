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
package com.codenvy.im.update;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class UtilServiceTest {
    private UtilService testUtilService;

    @BeforeMethod
    public void setup() {
        testUtilService = new UtilService();
    }

    @Test
    public void shouldReturnClientIp() {
        HttpServletRequest mockRequestContext = mock(HttpServletRequest.class);
        String testUserIp = "10.20.30.40";
        doReturn(testUserIp).when(mockRequestContext).getRemoteAddr();

        Response response = testUtilService.getClientIp(mockRequestContext);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity(), "10.20.30.40");
    }

    @Test
    public void shouldReturnErrorWhenGettingClientIpFailed() {
        HttpServletRequest mockRequestContext = mock(HttpServletRequest.class);
        doThrow(new RuntimeException("error")).when(mockRequestContext).getRemoteAddr();

        Response response = testUtilService.getClientIp(mockRequestContext);
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity(), "Unexpected error. error");
    }

}