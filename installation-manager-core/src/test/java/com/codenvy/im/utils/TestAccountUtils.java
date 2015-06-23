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
package com.codenvy.im.utils;

import com.codenvy.im.saas.SaasAccountServiceProxy;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestAccountUtils {

    private final static String SUBSCRIPTION_ID = "subscription_id1";
    private final static String ACCESS_TOKEN    = "accessToken";
    private final static String ACCOUNT_ID      = "accountId";

    @Mock
    private HttpTransport           transport;
    private SaasAccountServiceProxy saasAccountServiceProxy;

    private SimpleDateFormat subscriptionDateFormat;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        saasAccountServiceProxy = new SaasAccountServiceProxy("", transport);
        subscriptionDateFormat = new SimpleDateFormat(SaasAccountServiceProxy.SUBSCRIPTION_DATE_FORMAT);
    }

    @Test
    public void testValidSubscriptionByAccountReference() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"another-id\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"" +
                                                                   ACCOUNT_ID + "\"}"
                                                                   + "}]");
        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");
        assertTrue(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test
    public void testGetAccountIdWhereUserIsOwner() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"member-id\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                   + "}]");
        AccountReference accountReference = saasAccountServiceProxy.getAccountWhereUserIsOwner(null, ACCESS_TOKEN);
        assertNotNull(accountReference);
        assertEquals(accountReference.getId(), "id1");
        assertEquals(accountReference.getName(), "name1");
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerReturnNull() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"member-id\"}"
                                                                   + "},{"
                                                                   + "roles:[\"account/member \"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "},{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                   + "}]");
        AccountReference accountReference = saasAccountServiceProxy.getAccountWhereUserIsOwner(null, ACCESS_TOKEN);
        assertNull(accountReference);
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerSpecificAccountName() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"member-id\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                   + "}]");
        AccountReference accountReference = saasAccountServiceProxy.getAccountWhereUserIsOwner("name2", ACCESS_TOKEN);
        assertNotNull(accountReference);
        assertEquals(accountReference.getId(), "id2");
        assertEquals(accountReference.getName(), "name2");
    }

    @Test
    public void testGetAccountIdWhereUserIsOwnerSpecificAccountNameReturnNullIfAccountWasNotFound() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"member-id\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "},{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id2\",name:\"name2\"}"
                                                                   + "}]");
        AccountReference accountReference = saasAccountServiceProxy.getAccountWhereUserIsOwner("name3", ACCESS_TOKEN);
        assertNull(accountReference);
    }

    @Test
    public void testValidSubscriptionByLink() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "links:[{\"rel\":\"subscriptions\",\"href\":\"/account/"
                                                                   + ACCOUNT_ID
                                                                   + "/subscriptions\"}],"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"]"
                                                                   + "}]");

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

        assertTrue(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test
    public void testInvalidSubscription() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"" + ACCOUNT_ID +
                                                                   "\"}"
                                                                   + "}]");
        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:invalid}]");

        assertFalse(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test(expectedExceptions = AuthenticationException.class,
          expectedExceptionsMessageRegExp = "Authentication error. Authentication token might be expired or invalid.")
    public void testInvalidAuthentication() throws IOException {
        doThrow(new HttpException(403, "auth error"))
            .when(transport)
            .doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN));

        saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID);
    }

    @Test
    public void testValidSubscriptionByDate() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");
        assertTrue(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test
    public void testOutdatedSubscription() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

        assertFalse(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test
    public void testInvalidSubscriptionStartDateIsTomorrow() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

        assertFalse(saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute is absent")
    public void testValidSubscriptionByDateStartDateIsAbsent() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",endDate:\"" + endDate + "\"}]");

        saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute is absent")
    public void testValidSubscriptionByDateEndDateIsAbsent() throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\"}]");

        saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. Start date attribute has wrong format: .*")
    public void testInvalidSubscriptionStartDateIsWrong() throws IOException {
        SimpleDateFormat subscriptionDateWrongFormat = new SimpleDateFormat("yyyy.MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateWrongFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

        saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Can't validate subscription. End date attribute has wrong format: .*")
    public void testInvalidSubscriptionEndDateIsWrong() throws IOException {
        SimpleDateFormat subscriptionDateWrongFormat = new SimpleDateFormat("yyyy.MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        String startDate = subscriptionDateFormat.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        String endDate = subscriptionDateWrongFormat.format(cal.getTime());

        when(transport.doGet(endsWith("subscription/find/account?id=" + ACCOUNT_ID), eq(ACCESS_TOKEN)))
            .thenReturn("[{serviceId:" + saasAccountServiceProxy.ON_PREMISES + ",id:" + SUBSCRIPTION_ID
                        + ",startDate:\"" + startDate + "\",endDate:\"" + endDate + "\"}]");

        saasAccountServiceProxy.hasValidSubscription(saasAccountServiceProxy.ON_PREMISES, ACCESS_TOKEN, ACCOUNT_ID);
    }

    @Test
    public void checkIfUserIsOwnerOfAccountShouldReturnTrue() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"" + saasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "}]");
        assertTrue(saasAccountServiceProxy.checkIfUserIsOwnerOfAccount(ACCESS_TOKEN, "id1"));
    }

    @Test
    public void checkIfUserIsOwnerOfAccountReturnFalse() throws IOException {
        when(transport.doGet("/account", ACCESS_TOKEN)).thenReturn("[{"
                                                                   + "roles:[\"account/member\"],"
                                                                   + "accountReference:{id:\"id1\",name:\"name1\"}"
                                                                   + "}]");
        assertFalse(saasAccountServiceProxy.checkIfUserIsOwnerOfAccount(ACCESS_TOKEN, "id1"));
    }

    @Test
    public void testDeleteSubscription() throws Exception {
        saasAccountServiceProxy.deleteSubscription(ACCESS_TOKEN, "subscriptionId");

        verify(transport).doDelete(endsWith("subscription/subscriptionId"), eq(ACCESS_TOKEN));
    }
}
