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
import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.MemberDescriptor;

import javax.annotation.Nullable;
import javax.inject.Named;
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
@Singleton
public class SaasAccountServiceProxy {
    public final static String ON_PREMISES              = "OnPremises";
    public static final String ACCOUNT_OWNER_ROLE       = "account/owner";
    public static final String SUBSCRIPTION_DATE_FORMAT = "MM/dd/yy";

    public static final String CANNOT_RECOGNISE_ACCOUNT_NAME_MSG =
            "You are logged as a user which does not have an account/owner role in any account. " +
            "This likely means that you used the wrong credentials to access Codenvy.";

    public static final String USE_ACCOUNT_MESSAGE_TEMPLATE = "Your Codenvy account '%s' will be used to verify on-premises subscription.";

    private final String        saasApiEndpoint;
    private final HttpTransport transport;

    @Inject
    public SaasAccountServiceProxy(@Named("saas.api.endpoint") String saasApiEndpoint,
                                   HttpTransport transport) {
        this.saasApiEndpoint = saasApiEndpoint;
        this.transport = transport;
    }

    /**
     * Indicates if the current user has a valid subscription.
     *
     * @throws java.lang.IllegalArgumentException
     * @throws java.io.IOException
     */
    public boolean hasValidSubscription(String requiredSubscription,
                                        String accessToken,
                                        String accountId) throws IOException, IllegalArgumentException {
        try {
            List<SubscriptionDescriptor> subscriptions = getSubscriptions(accessToken,
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

    private boolean isSubscriptionUseAvailableByDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
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
    public Date getSubscriptionEndDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionDescriptor.getEndDate(), "End date");
    }

    /** @return the subscription end date */
    public Date getSubscriptionStartDate(SubscriptionDescriptor subscriptionDescriptor) throws IllegalArgumentException {
        return getAttributeAsDate(subscriptionDescriptor.getStartDate(), "Start date");
    }

    private Date getAttributeAsDate(String attributeValue, String attributeName) throws IllegalArgumentException {
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
    public void deleteSubscription(String accessToken,
                                   String subscriptionId) throws IOException {
        String requestUrl = combinePaths(saasApiEndpoint, "subscription", subscriptionId);
        transport.doDelete(requestUrl, accessToken);
    }

    /** Get certain subscription descriptor * */
    @Nullable
    public SubscriptionDescriptor getSubscription(String subscriptionName,
                                                  String accessToken,
                                                  String accountId) throws IOException {

        List<SubscriptionDescriptor> subscriptions = getSubscriptions(accessToken, accountId);
        for (SubscriptionDescriptor subscriptionDescriptor : subscriptions) {
            if (subscriptionDescriptor.getServiceId().equalsIgnoreCase(subscriptionName)) {
                return subscriptionDescriptor;
            }
        }

        return null;
    }

    private List<SubscriptionDescriptor> getSubscriptions(String accessToken,
                                                          String accountId) throws IOException {
        String requestUrl = combinePaths(saasApiEndpoint, "subscription", "find", "account", accountId);
        return createListDtoFromJson(transport.doGet(requestUrl, accessToken), SubscriptionDescriptor.class);
    }


    /**
     * @return the specific account where user has {@link #ACCOUNT_OWNER_ROLE} role or the first one if accountName parameter is null.
     * @throws IOException
     */
    @Nullable
    public AccountReference getAccountWhereUserIsOwner(@Nullable String accountName, String accessToken) throws IOException {

        List<MemberDescriptor> members = createListDtoFromJson(transport.doGet(combinePaths(saasApiEndpoint, "account"), accessToken),
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
    public boolean checkIfUserIsOwnerOfAccount(String accessToken,
                                               String accountId) throws IOException {

        List<MemberDescriptor> members = createListDtoFromJson(transport.doGet(combinePaths(saasApiEndpoint, "account"), accessToken),
                                                               MemberDescriptor.class);

        for (MemberDescriptor m : members) {
            if (hasRole(m, ACCOUNT_OWNER_ROLE) && m.getAccountReference().getId().equals(accountId)) {
                return true;
            }
        }

        return false;
    }

    /** Checks if user has specific role. */
    private boolean hasRole(MemberDescriptor member, String role) throws IllegalStateException {
        List<String> roles = member.getRoles();
        return roles != null && roles.contains(role);
    }
}
