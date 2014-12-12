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

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public static final String ACCOUNT_OWNER_ROLE       = "account/owner";
    public static final String SUBSCRIPTION_DATE_FORMAT = "MM/dd/yy";

    /** Utility class so there is no public constructor. */
    private AccountUtils() {
    }

    /**
     * Indicates if the current user has a valid subscription.
     *
     * @throws java.lang.IllegalStateException
     * @throws java.io.IOException
     */
    public static boolean isValidSubscription(HttpTransport transport,
                                              String apiEndpoint,
                                              String requiredSubscription,
                                              String accessToken,
                                              String accountId) throws IOException, IllegalStateException {
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

    private static boolean isSubscriptionUseAvailableByDate(SubscriptionAttributesDescriptor subscriptionAttributes) throws IllegalStateException {
        DateFormat subscriptionDateFormat = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);

        Date startDate;
        Date endDate;

        String startDateStr = subscriptionAttributes.getStartDate();
        try {
            if (startDateStr == null) {
                throw new IllegalStateException("Can't validate subscription. Start date attribute is absent");
            }

            startDate = subscriptionDateFormat.parse(startDateStr);
        } catch (ParseException e) {
            throw new IllegalStateException("Can't validate subscription. Start date attribute has wrong format: " + startDateStr, e);
        }

        String endDateStr = subscriptionAttributes.getEndDate();
        try {
            if (endDateStr == null) {
                throw new IllegalStateException("Can't validate subscription. End date attribute is absent");
            }

            endDate = subscriptionDateFormat.parse(endDateStr);
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "Can't validate subscription. End date attribute has wrong format: " + endDateStr, e);
        }

        Date currentDate = Calendar.getInstance().getTime();

        return startDate.getTime() <= currentDate.getTime() && currentDate.getTime() <= endDate.getTime();
    }

    private static List<SubscriptionDescriptor> getSubscriptions(HttpTransport transport,
                                                                 String apiEndpoint,
                                                                 String accessToken,
                                                                 String accountId) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/" + accountId + "/subscriptions");
        return createListDtoFromJson(transport.doGet(requestUrl, accessToken), SubscriptionDescriptor.class);
    }

    private static SubscriptionAttributesDescriptor getSubscriptionAttributes(String subscriptionId,
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
                                                                       String userToken,
                                                                       @Nullable String accountName) throws IOException {

        List<MemberDescriptor> members = createListDtoFromJson(transport.doGet(combinePaths(apiEndpoint, "account"), userToken),
                                                               MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (hasRole(m, ACCOUNT_OWNER_ROLE) && (accountName == null || accountName.equals(m.getAccountReference().getName()))) {
                return m.getAccountReference();
            }
        }

        return null;
    }

    /** Checks if user has specific role. */
    private static boolean hasRole(MemberDescriptor member, String role) throws IllegalStateException {
        List<String> roles = member.getRoles();
        return roles != null && roles.contains(role);
    }
}
