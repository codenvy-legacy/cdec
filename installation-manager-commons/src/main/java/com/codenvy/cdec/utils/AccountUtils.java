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
package com.codenvy.cdec.utils;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.codenvy.cdec.utils.Commons.combinePaths;
import static com.codenvy.cdec.utils.Commons.createListDtoFromJson;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 *
 * TODO after login command
 */
public class AccountUtils {
    public static final String PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR = "Path to subscriptions hasn't found.";
    public static final String VALID_ACCOUNT_NOT_FOUND_ERROR         = "Valid account hasn't found.";
    public static final String ACCOUNT_OWNER_ROLE = "account/owner";

    /**
     * Indicates if the current user has a valid subscription.
     *
     * @throws java.lang.IllegalStateException
     * @throws java.io.IOException
     */
    public static boolean isValidSubscription(HttpTransport transport,
                                              String apiEndpoint,
                                              String requiredSubscription,
                                              String authToken) throws IOException, IllegalStateException {
        List<SubscriptionDescriptor> subscriptions = getSubscriptions(transport, apiEndpoint, authToken);

        for (SubscriptionDescriptor s : subscriptions) {
            if (s.getServiceId().equalsIgnoreCase(requiredSubscription)) {
                return true;
            }
        }

        return false;
    }

    protected static List<SubscriptionDescriptor> getSubscriptions(HttpTransport transport, String apiEndpoint, String authToken) throws IOException {
        MemberDescriptor account = getAccountWithProperRole(transport, apiEndpoint, ACCOUNT_OWNER_ROLE, authToken);
        if (account == null) {
            throw new IllegalStateException(VALID_ACCOUNT_NOT_FOUND_ERROR);
        }

        String subscriptionsHref = getSubscriptionsHref(account);
        if (subscriptionsHref == null) {
            String accountId = getAccountId(account);
            if (accountId != null) {
                subscriptionsHref = getSubscriptionsHref(apiEndpoint, accountId);
            }
        }

        if (subscriptionsHref == null) {
            throw new IllegalStateException(PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR);
        }

        return createListDtoFromJson(transport.doGetRequest(subscriptionsHref, authToken), SubscriptionDescriptor.class);
    }

    @Nullable
    protected static MemberDescriptor getAccountWithProperRole(HttpTransport transport,
                                                               String apiEndpoint,
                                                               String role,
                                                               String authToken) throws IOException {

        List<MemberDescriptor> accounts = createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account"), authToken),
                                                                MemberDescriptor.class);

        for (MemberDescriptor account : accounts) {
            if (hasRole(account, role)) {
                return account;
            }
        }

        return null;
    }

    @Nullable
    private static String getSubscriptionsHref(MemberDescriptor account) {
        List<Link> links = account.getLinks();
        for (Link link : links) {
            if (link.getRel().equals("subscriptions")) {
                return link.getHref();
            }
        }

        return null;
    }

    private static String getSubscriptionsHref(String apiEndpoint, String accountId) {
        return combinePaths(apiEndpoint, "account/" + accountId + "/subscriptions");
    }

    @Nullable
    private static String getAccountId(MemberDescriptor account) {
        AccountReference reference = account.getAccountReference();
        return reference != null ? account.getAccountReference().getId() : null; // Platform API issue
    }

    private static boolean hasRole(MemberDescriptor account, String role) {
        List<String> roles = account.getRoles();
        return roles != null && roles.contains(role);
    }
}