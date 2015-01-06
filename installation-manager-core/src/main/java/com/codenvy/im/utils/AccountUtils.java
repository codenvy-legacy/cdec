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

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionAttributesDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createDtoFromJson;
import static com.codenvy.im.utils.Commons.createListDtoFromJson;
import static com.codenvy.im.utils.Commons.getProperException;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public final static String ON_PREMISES = "OnPremises";
    public static final String ACCOUNT_OWNER_ROLE       = "account/owner";
    public static final String SUBSCRIPTION_DATE_FORMAT = "MM/dd/yy";

    /** Utility class so there is no public constructor. */
    private AccountUtils() {
    }

    /**
     * Indicates if the current user has a valid subscription.
     *
     * @throws java.lang.IllegalArgumentException
     * @throws java.io.IOException
     */
    public static boolean hasValidSubscription(HttpTransport transport,
                                               String apiEndpoint,
                                               String requiredSubscription,
                                               String accessToken,
                                               String accountId) throws IOException, IllegalArgumentException {
        try {
            List<SubscriptionDescriptor> subscriptions = getSubscriptions(transport,
                                                                          apiEndpoint,
                                                                          accessToken,
                                                                          accountId);
            for (SubscriptionDescriptor s : subscriptions) {
                if (s.getServiceId().equalsIgnoreCase(requiredSubscription)) {
                    SubscriptionAttributesDescriptor attributes = getSubscriptionAttributes(s.getId(), transport, apiEndpoint, accessToken);
                    return isSubscriptionUseAvailableByDate(attributes);
                }
            }

            return false;
        } catch (IOException e) {
            throw getProperException(e);
        }
    }

    private static boolean isSubscriptionUseAvailableByDate(SubscriptionAttributesDescriptor subscriptionAttributes) throws IllegalArgumentException {
        try {
            Date startDate = getSubscriptionStartDate(subscriptionAttributes);
            Date endDate = getSubscriptionEndDate(subscriptionAttributes);

            Date currentDate = Calendar.getInstance().getTime();

            return startDate.getTime() <= currentDate.getTime() && currentDate.getTime() <= endDate.getTime();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Can't validate subscription. " + e.getMessage(), e);
        }
    }

    /** @return the subscription start date */
    public static Date getSubscriptionEndDate(SubscriptionAttributesDescriptor subscriptionAttributes) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionAttributes.getEndDate(), "End date");
    }

    /** @return the subscription end date */
    public static Date getSubscriptionStartDate(SubscriptionAttributesDescriptor subscriptionAttributes) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionAttributes.getStartDate(), "Start date");
    }

    private static Date getAttributeAsDate(String attributeValue, String attributeName) throws IllegalArgumentException {
        DateFormat subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);

        try {
            if (attributeValue == null) {
                throw new IllegalArgumentException(format("%s attribute is absent", attributeName));
            }

            return subscriptionDateFormat.parse(attributeValue);
        } catch (ParseException e) {
            throw new IllegalArgumentException(format("%s attribute has wrong format: %s", attributeName, attributeValue), e);
        }
    }

    /** Deletes subscription */
    public static void deleteSubscription(HttpTransport transport,
                                          String apiEndpoint,
                                          String accessToken,
                                          String subscriptionId) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/subscriptions/" + subscriptionId);
        transport.doDelete(requestUrl, accessToken);
    }

    private static List<SubscriptionDescriptor> getSubscriptions(HttpTransport transport,
                                                                 String apiEndpoint,
                                                                 String accessToken,
                                                                 String accountId) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/" + accountId + "/subscriptions");
        return createListDtoFromJson(transport.doGet(requestUrl, accessToken), SubscriptionDescriptor.class);
    }

    /** @return subscription attributes */
    public static SubscriptionAttributesDescriptor getSubscriptionAttributes(String subscriptionId,
                                                                             HttpTransport transport,
                                                                             String apiEndpoint,
                                                                             String accessToken) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/subscriptions/" + subscriptionId + "/attributes");
        return createDtoFromJson(transport.doGet(requestUrl, accessToken), SubscriptionAttributesDescriptor.class);
    }


    /**
     * @return the specific account where user has {@link #ACCOUNT_OWNER_ROLE} role or the first one if accountName parameter is null.
     * @throws IOException
     */
    @Nullable
    public static AccountReference getAccountReferenceWhereUserIsOwner(HttpTransport transport,
                                                                       String apiEndpoint,
                                                                       String accessToken,
                                                                       @Nullable String accountName) throws IOException {

        List<MemberDescriptor> members = createListDtoFromJson(transport.doGet(combinePaths(apiEndpoint, "account"), accessToken),
                                                               MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (hasRole(m, ACCOUNT_OWNER_ROLE) && (accountName == null || accountName.equals(m.getAccountReference().getName()))) {
                return m.getAccountReference();
            }
        }

        return null;
    }

    /**
     * @return true if user is owner if the given account, otherwise method returns false
     * @throws IOException
     */
    public static boolean checkIfUserIsOwnerOfAccount(HttpTransport transport,
                                                      String apiEndpoint,
                                                      String accessToken,
                                                      String accountId) throws IOException {

        List<MemberDescriptor> members = createListDtoFromJson(transport.doGet(combinePaths(apiEndpoint, "account"), accessToken),
                                                               MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (hasRole(m, ACCOUNT_OWNER_ROLE) && m.getAccountReference().getId().equals(accountId)) {
                return true;
            }
        }

        return false;
    }

    /** Checks if user has specific role. */
    private static boolean hasRole(MemberDescriptor member, String role) throws IllegalStateException {
        List<String> roles = member.getRoles();
        return roles != null && roles.contains(role);
    }

    public static class SubscriptionInfo {
        private final String   accountId;
        private final String   subscriptionId;
        private final String   serviceId;
        private final Calendar startDate;
        private final Calendar endDate;

        public SubscriptionInfo(String accountId, String subscriptionId, String serviceId, Calendar startDate, Calendar endDate) {
            this.subscriptionId = subscriptionId;
            this.accountId = accountId;
            this.serviceId = serviceId;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public String getServiceId() {
            return serviceId;
        }

        public Calendar getStartDate() {
            return startDate;
        }

        public Calendar getEndDate() {
            return endDate;
        }
    }
}
