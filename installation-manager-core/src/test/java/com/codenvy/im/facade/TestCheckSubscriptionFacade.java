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

import com.codenvy.im.BaseTest;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.request.Request;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.AuthenticationException;
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.codenvy.im.saas.SaasAccountServiceProxy.SUBSCRIPTION_DATE_FORMAT;
import static java.util.Calendar.getInstance;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestCheckSubscriptionFacade extends BaseTest {
    @Mock
    private SaasAuthServiceProxy saasAuthServiceProxy;
    @Mock
    private HttpTransport        transport;
    @Mock
    private Request              request;
    @Mock
    private PasswordManager      passwordManager;
    @Mock
    private NodeManager          nodeManager;
    @Mock
    private BackupManager        backupManager;
    @Mock
    private StorageManager       storageManager;
    @Mock
    private InstallManager       installManager;
    @Mock
    private DownloadManager      downloadManager;

    private InstallationManagerFacade installationManagerService;
    private SaasAccountServiceProxy   saasAccountServiceProxy;

    @BeforeMethod
    public void init() {
        initMocks(this);
        saasAccountServiceProxy = new SaasAccountServiceProxy("update/endpoint", transport);
        installationManagerService = new InstallationManagerFacade("target/download",
                                                                   "update/endpoint",
                                                                   transport,
                                                                   saasAuthServiceProxy,
                                                                   saasAccountServiceProxy,
                                                                   passwordManager,
                                                                   nodeManager,
                                                                   backupManager,
                                                                   storageManager,
                                                                   installManager,
                                                                   downloadManager);
        request = new Request().setSaasUserCredentials(new SaasUserCredentials("auth token", "accountId"));
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
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

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
        when(transport.doGet(endsWith("account"), eq("auth token")))
                .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("account/accountId/subscriptions"), eq("auth token")))
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");

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
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"2014.11.21\",endDate:\"21/11/2015\"}]");

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
                .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"11/21/2014\",endDate:\"2015.11.21\"}]");

        String response = installationManagerService.checkSubscription("OnPremises", request);
        assertEquals(response, "{\n" +
                               "  \"subscription\" : \"OnPremises\",\n" +
                               "  \"message\" : \"Can't validate subscription. End date attribute has wrong format: 2015.11.21\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testAddTrialSubscription() throws Exception {
        SaasUserCredentials saasUserCredentials = new SaasUserCredentials();
        saasUserCredentials.setToken("token");
        saasUserCredentials.setAccountId("id");

        installationManagerService.addTrialSaasSubscription(saasUserCredentials);

        verify(transport).doPost(endsWith("subscription/id"), isNull(), eq("token"));
    }
}
