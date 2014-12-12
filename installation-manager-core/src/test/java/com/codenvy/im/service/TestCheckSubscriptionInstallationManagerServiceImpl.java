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
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.codenvy.im.utils.AccountUtils.SUBSCRIPTION_DATE_FORMAT;
import static java.util.Calendar.getInstance;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestCheckSubscriptionInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       transport;
    private Request             request;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl("update/endpoint", "api/endpoint", mockInstallationManager, transport);
        request = new Request().setUserCredentials(new UserCredentials("auth token", "accountId"));
    }

    public void initMocks() {
        mockInstallationManager = PowerMockito.mock(InstallationManagerImpl.class);
        transport = Mockito.mock(HttpTransport.class);
    }

    @Test
    public void testCheckValidSubscription() throws Exception {
        SimpleDateFormat subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
        Calendar cal = getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");
        when(transport.doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("auth token")))
                .thenReturn("{startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Subscription is valid\",\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfSubscriptionIsAbsent() throws Exception {
        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[]");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Subscription not found or outdated\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfAuthenticationFailed() throws Exception {
        doThrow(new AuthenticationException()).when(transport).doGet(endsWith("account/accountId/subscriptions"), eq("auth token"));

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Authentication error. Authentication token might be expired or invalid.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfStartDateIsAbsent() throws Exception {
        SimpleDateFormat subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
        Calendar cal = getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");
        when(transport.doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("auth token")))
                .thenReturn("{endDate:\"" + endDate + "\"}");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Can't validate subscription. Start date attribute is absent\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfEndDateIsAbsent() throws Exception {
        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");
        when(transport.doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("auth token")))
                .thenReturn("{}");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Can't validate subscription. Start date attribute is absent\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfStartDateIsWrongFormat() throws Exception {
        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");
        when(transport.doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("auth token")))
                .thenReturn("{startDate:\"2014.11.21\",endDate:\"21/11/2015\"}");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Can't validate subscription. Start date attribute has wrong format: 2014.11.21\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfEndDateIsWrongFormat() throws Exception {
        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");
        when(transport.doGet(endsWith("/account/subscriptions/subscriptionId/attributes"), eq("auth token")))
                .thenReturn("{startDate:\"11/21/2014\",endDate:\"2015.11.21\"}");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Can't validate subscription. End date attribute has wrong format: 2015.11.21\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
