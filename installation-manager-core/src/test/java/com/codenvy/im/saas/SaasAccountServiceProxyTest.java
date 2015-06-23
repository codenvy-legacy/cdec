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

package com.codenvy.im.saas;

import com.codenvy.api.subscription.shared.dto.SubscriptionDescriptor;
import com.codenvy.im.BaseTest;
import com.codenvy.im.utils.AuthenticationException;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.commons.json.JsonParseException;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
public class SaasAccountServiceProxyTest extends BaseTest {

    private static final String TOKEN        = "auth token";
    private static final String ACCOUNT_ID   = "accountId";
    private static final String ACCOUNT_NAME = "accountName";
    private static final String SUBSCRIPTION = "OnPremises";

    @Mock
    private HttpTransport transport;

    private SaasAccountServiceProxy saasAccountServiceProxy;

    @BeforeMethod
    public void init() {
        initMocks(this);
        saasAccountServiceProxy = new SaasAccountServiceProxy("update/endpoint", transport);
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testCheckSubscriptionErrorIfAuthenticationFailed() throws Exception {
        doThrow(new AuthenticationException()).when(transport).doGet(endsWith("subscription/find/account?id=accountId"), eq(TOKEN));

        saasAccountServiceProxy.hasValidSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute is absent")
    public void testCheckSubscriptionErrorIfStartDateIsAbsent() throws Exception {
        when(transport.doGet(endsWith("account"), eq(TOKEN)))
            .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("subscription/find/account?id=accountId"), eq(TOKEN)))
            .thenReturn("[{serviceId:OnPremises,id:subscriptionId}]");

        saasAccountServiceProxy.hasValidSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute is absent")
    public void testCheckSubscriptionErrorIfEndDateIsAbsent() throws Exception {
        when(transport.doGet(endsWith("account"), eq(TOKEN)))
            .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("subscription/find/account?id=accountId"), eq(TOKEN)))
            .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"10/12/2012\"}]");

        saasAccountServiceProxy.hasValidSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute has wrong format: 2014.11.21")
    public void testCheckSubscriptionErrorIfStartDateIsWrongFormat() throws Exception {
        when(transport.doGet(endsWith("account"), eq(TOKEN)))
            .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("subscription/find/account?id=accountId"), eq(TOKEN)))
            .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"2014.11.21\",endDate:\"21/11/2015\"}]");

        saasAccountServiceProxy.hasValidSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute has wrong format: 2015.11.21")
    public void testCheckSubscriptionErrorIfEndDateIsWrongFormat() throws Exception {
        when(transport.doGet(endsWith("account"), eq(TOKEN)))
            .thenReturn("[{roles:[\"account/owner\"],accountReference:{id:accountId}}]");
        when(transport.doGet(endsWith("subscription/find/account?id=accountId"), eq(TOKEN)))
            .thenReturn("[{serviceId:OnPremises,id:subscriptionId,startDate:\"11/21/2014\",endDate:\"2015.11.21\"}]");

        saasAccountServiceProxy.hasValidSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test
    public void testGetSubscription() throws IOException, JsonParseException {
        final String SUBSCRIPTION_ID = "subscription_id1";
        SimpleDateFormat subscriptionDateFormat = new SimpleDateFormat(SaasAccountServiceProxy.SUBSCRIPTION_DATE_FORMAT);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        String testDescriptorJson = "{serviceId:" + SaasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                                    + ",startDate: \"" + startDate + "\",endDate:\"" + endDate + "\"}";
        doReturn("[" + testDescriptorJson + "]").when(transport)
                                                .doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(TOKEN));

        SubscriptionDescriptor descriptor = saasAccountServiceProxy.getSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
        assertNotNull(descriptor);
        assertEquals(descriptor.getServiceId(), "OnPremises");
        assertEquals(descriptor.getId(), "subscription_id1");
        assertEquals(descriptor.getStartDate(), startDate);
        assertEquals(descriptor.getEndDate(), endDate);
        assertTrue(descriptor.getProperties().isEmpty());
        assertTrue(descriptor.getLinks().isEmpty());
    }

    @Test
    public void testGetSubscriptionWhenDescriptorNull() throws IOException {
        doReturn("[]").when(transport).doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(TOKEN));

        SubscriptionDescriptor descriptor = saasAccountServiceProxy.getSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
        assertNull(descriptor);
    }

    @Test(expectedExceptions = HttpException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testGetSubscriptionWhenException() throws IOException {
        doThrow(new HttpException(500, "error")).when(transport)
                                                .doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(TOKEN));
        saasAccountServiceProxy.getSubscription(SUBSCRIPTION, TOKEN, ACCOUNT_ID);
    }

    @Test
    public void testGetAccountReference() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"" + ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                 + "}]").when(transport).doGet(endsWith("account"), eq(TOKEN));
        AccountReference accountRef = saasAccountServiceProxy.getAccountWhereUserIsOwner(null, TOKEN);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }

    @Test
    public void testGetAccountReferenceFromSeveral() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"" + ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                 + "},{roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"another-account-id\"}"
                 + "}]").when(transport).doGet(endsWith("account"), eq(TOKEN));
        AccountReference accountRef = saasAccountServiceProxy.getAccountWhereUserIsOwner(null, TOKEN);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }

    @Test
    public void testGetAccountReferenceReturnNullIfAccountWasNotFound() throws Exception {
        doReturn("[{"
                 + "roles:[\"account/member\"],"
                 + "accountReference:{id:\"" + ACCOUNT_ID + "\"}"
                 + "}]").when(transport).doGet(endsWith("account"), eq(TOKEN));
        AccountReference accountRef = saasAccountServiceProxy.getAccountWhereUserIsOwner(null, TOKEN);

        assertNull(accountRef);
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetAccountReferenceErrorIfAuthenticationFailed() throws Exception {
        doThrow(AuthenticationException.class).when(transport).doGet(endsWith("account"), eq(TOKEN));

        saasAccountServiceProxy.getAccountWhereUserIsOwner(null, TOKEN);
    }

    @Test
    public void testGetAccountReferenceWithSpecificName() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"" + ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                 + "}]").when(transport).doGet(endsWith("account"), eq(TOKEN));

        AccountReference accountRef = saasAccountServiceProxy.getAccountWhereUserIsOwner(ACCOUNT_NAME, TOKEN);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }


    @Test
    public void testGetAccountReferenceWithSpecificNameReturnNullIfAccountWasNotFound() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"" + ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                 + "}]").when(transport).doGet(endsWith("account"), eq(TOKEN));

        AccountReference accountRef = saasAccountServiceProxy.getAccountWhereUserIsOwner("another name", TOKEN);

        assertNull(accountRef);
    }


}
