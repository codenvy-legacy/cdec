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
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.im.user.UserCredentials;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.utils.Commons.*;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public static final  String           ACCOUNT_OWNER_ROLE       = "account/owner";
    public static final  String           SUBSCRIPTION_DATE_FORMAT = "MM/dd/yyyy";
    private static final SimpleDateFormat subscriptionDateFormat   = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);

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
                                              UserCredentials userCredentials) throws IOException, IllegalStateException {
        try {
            List<SubscriptionDescriptor> subscriptions = getSubscriptions(transport,
                                                                          apiEndpoint,
                                                                          userCredentials);
            for (SubscriptionDescriptor s : subscriptions) {
                if (s.getServiceId().equalsIgnoreCase(requiredSubscription)) {
                    SubscriptionAttributesDescriptor attributes = getSubscriptionAttributes(s.getId(), transport, apiEndpoint, userCredentials);
                    return isSubscriptionUseAvailableByDate(attributes);
                }
            }

            return false;
        } catch (IOException e) {
            throw getProperException(e);
        }
    }

    private static boolean isSubscriptionUseAvailableByDate(SubscriptionAttributesDescriptor subscriptionAttributes) throws IllegalStateException {
        Date startDate;
        Date endDate;

        String startDateStr = subscriptionAttributes.getStartDate();
        try {
            if (startDateStr == null) {
                throw new IllegalStateException("Can't validate subscription. Start date attribute isn't exist");
            }

            startDate = subscriptionDateFormat.parse(startDateStr);
        } catch (ParseException e) {
            throw new IllegalStateException("Can't validate subscription. Start date attribute has wrong format: " + startDateStr, e);
        }

        String endDateStr = subscriptionAttributes.getEndDate();
        try {
            if (endDateStr == null) {
                throw new IllegalStateException("Can't validate subscription. End date attribute isn't exist");
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
                                                                 UserCredentials userCredentials) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/" + userCredentials.getAccountId() + "/subscriptions");
        return createListDtoFromJson(transport.doGetRequest(requestUrl, userCredentials.getToken()), SubscriptionDescriptor.class);
    }

    private static SubscriptionAttributesDescriptor getSubscriptionAttributes(String subscriptionId,
                                                                              HttpTransport transport,
                                                                              String apiEndpoint,
                                                                              UserCredentials userCredentials) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "account/subscriptions/" + subscriptionId + "/attributes");
        return createDtoFromJson(transport.doGetRequest(requestUrl, userCredentials.getToken()), SubscriptionAttributesDescriptor.class);
    }


    /**
     * This method iterates over all user's accounts and returns the first one where user in question is owner.
     *
     * @return String accountId
     * @throws IOException
     */
    @Nullable
    public static String getAccountIdWhereUserIsOwner(HttpTransport transport,
                                                      String apiEndpoint,
                                                      String userToken) throws IOException {
        List<MemberDescriptor> members =
                createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account"), userToken),
                                      MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (hasRole(m, ACCOUNT_OWNER_ROLE)) {
                return getAccountId(m);
            }
        }

        return null;
    }


    /** Checks if user is owner of specific account. */
    public static boolean isValidAccountId(HttpTransport transport,
                                           String apiEndpoint,
                                           UserCredentials userCredentials) throws IOException {
        List<MemberDescriptor> members =
                createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account"), userCredentials.getToken()),
                                      MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (userCredentials.getAccountId().equals(getAccountId(m)) && hasRole(m, ACCOUNT_OWNER_ROLE)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static String getSubscriptionsHref(MemberDescriptor member) {
        List<Link> links = member.getLinks();
        for (Link link : links) {
            if (link.getRel().equals("subscriptions")) {
                return link.getHref();
            }
        }

        return null;
    }

    @Nullable
    private static String getAccountId(MemberDescriptor member) {
        AccountReference accountRef = member.getAccountReference();

        if (accountRef != null) {
            return member.getAccountReference().getId();
        }

        // Platform API issue.
        // Workaround: read id from subscriptions href
        String subscriptionsHref = getSubscriptionsHref(member);
        if (subscriptionsHref == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("account/([0-9a-z-]+)/subscriptions");
        Matcher m = pattern.matcher(subscriptionsHref);
        if (m.find()) {
            return m.group(1);
        }

        return null; // almost impossible case
    }

    /** Checks if user has specific role. */
    private static boolean hasRole(MemberDescriptor member, String role) throws IllegalStateException {
        List<String> roles = member.getRoles();
        return roles != null && roles.contains(role);
    }
}
