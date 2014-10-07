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

import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mockito;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestCheckSubscriptionInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       transport;

    @BeforeMethod
    public void init() {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl(mockInstallationManager, transport);
    }

    public void initMocks() {
        mockInstallationManager = mock(InstallationManagerImpl.class);
        transport = Mockito.mock(HttpTransport.class);
    }

    @Test
    public void testCheckValidSubscription() throws Exception {
        when(transport.doGetRequest(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGetRequest(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises}]");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("auth token", "accountId"));

        String response = installationManagerService.checkSubscription("OnPremises", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Subscription is valid\",\n" +
                                                      "  \"status\": \"OK\",\n" +
                                                      "  \"subscription\": \"OnPremises\"\n" +
                                                      "}");
    }

    @Test
    public void testCheckSubscriptionErrorIfSubscriptionIsAbsent() throws Exception {
        when(transport.doGetRequest(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGetRequest(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[]");

        JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<>(new UserCredentials("auth token", "accountId"));

        String response = installationManagerService.checkSubscription("OnPremises", userCredentialsRep);
        assertEquals(getPrettyPrintingJson(response), "{\n" +
                                                      "  \"message\": \"Subscription not found\",\n" +
                                                      "  \"status\": \"ERROR\",\n" +
                                                      "  \"subscription\": \"OnPremises\"\n" +
                                                      "}");
    }
}
