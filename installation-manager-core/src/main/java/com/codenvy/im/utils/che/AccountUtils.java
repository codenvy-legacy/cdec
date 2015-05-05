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
package com.codenvy.im.utils.che;

import com.codenvy.im.utils.HttpTransport;
import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.MemberDescriptor;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createListDtoFromJson;
import static com.codenvy.im.utils.Commons.getProperException;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public final static String ON_PREMISES              = "OnPremises";
    public static final String ACCOUNT_OWNER_ROLE       = "account/owner";
    public static final String SUBSCRIPTION_DATE_FORMAT = "MM/dd/yy";

    public static final String CANNOT_RECOGNISE_ACCOUNT_NAME_MSG =
        "You are logged as a user which does not have an account/owner role in any account. " +
        "This likely means that you used the wrong credentials to access Codenvy.";

    public static final String USE_ACCOUNT_MESSAGE_TEMPLATE = "Your Codenvy account '%s' will be used to verify on-premises subscription.";

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
            for (SubscriptionDescriptor subscriptionDescriptor : subscriptions) {
                if (subscriptionDescriptor.getServiceId().equalsIgnoreCase(requiredSubscription)) {
                    return isSubscriptionUseAvailableByDate(subscriptionDescriptor);
                }
            }

            return false;
        } catch (IOException e) {
            throw getProperException(e);
        }
    }

    private static boolean isSubscriptionUseAvailableByDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
        try {
            Date startDate = getSubscriptionStartDate(subscriptionDescriptor);
            Date endDate = getSubscriptionEndDate(subscriptionDescriptor);

            Date currentDate = Calendar.getInstance().getTime();

            return startDate.getTime() <= currentDate.getTime() && currentDate.getTime() <= endDate.getTime();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Can't validate subscription. " + e.getMessage(), e);
        }
    }

    /** @return the subscription start date */
    public static Date getSubscriptionEndDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionDescriptor.getEndDate(), "End date");
    }

    /** @return the subscription end date */
    public static Date getSubscriptionStartDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionDescriptor.getStartDate(), "Start date");
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
}
