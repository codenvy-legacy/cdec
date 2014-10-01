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
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.im.user.UserCredentials;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createListDtoFromJson;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public static final String PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR = "Path to subscriptions not found.";
    public static final String ACCOUNT_OWNER_ROLE                    = "account/owner";
    public static final String VALID_ACCOUNT_NOT_FOUND_ERROR = "User is not owner of his account";
    public static final String ACCOUNT_NOT_FOUND_ERROR               = "Account not found.";

    /**
     * Utility class so no public constructor.
     */
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
        List<SubscriptionDescriptor> subscriptions = getSubscriptions(transport, apiEndpoint, userCredentials);

        for (SubscriptionDescriptor s : subscriptions) {
            if (s.getServiceId().equalsIgnoreCase(requiredSubscription)) {
                return true;
            }
        }

        return false;
    }

    private static List<SubscriptionDescriptor> getSubscriptions(HttpTransport transport,
                                                                 String apiEndpoint,
                                                                 UserCredentials userCredentials) throws IOException {
        MemberDescriptor account = getAccount(transport, apiEndpoint, userCredentials);
        if (account == null) {
            throw new IllegalStateException(ACCOUNT_NOT_FOUND_ERROR);
        }

        if (!isValidateRole(account, ACCOUNT_OWNER_ROLE)) {
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

        return createListDtoFromJson(transport.doGetRequest(subscriptionsHref, userCredentials.getToken()), SubscriptionDescriptor.class);
    }

    @Nullable
    private static MemberDescriptor getAccount(HttpTransport transport,
                                              String apiEndpoint,
                                              UserCredentials userCredentials) throws IOException {
        List<MemberDescriptor> accounts =
                createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account"), userCredentials.getToken()),
                                      MemberDescriptor.class);

        for (MemberDescriptor account : accounts) {
            String id = getAccountId(account);
            if (id != null && id.equals(userCredentials.getAccountId())) {
                return account;
            }
        }

        return null;
    }

    @Nullable
    public static String getFirstValidAccountId(HttpTransport transport,
                                                String apiEndpoint,
                                                String userToken) throws IOException {
        List<MemberDescriptor> accounts =
            createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account"), userToken),
                                  MemberDescriptor.class);

        for (MemberDescriptor account : accounts) {
            String id = getAccountId(account);
            if (id != null && isValidateRole(account, ACCOUNT_OWNER_ROLE)) {
                return id;
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

        if (reference != null) {
            return account.getAccountReference().getId();
        }

        // Platform API issue.
        // Workaround: read id from subscriptions href
        String subscriptionsHref = getSubscriptionsHref(account);
        if (subscriptionsHref == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("account/([0-9a-z-]+)/subscriptions");
        Matcher m = pattern.matcher(subscriptionsHref);
        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    private static boolean isValidateRole(MemberDescriptor account, String role) throws IllegalStateException {
        List<String> roles = account.getRoles();
        return roles != null && roles.contains(role);
    }
}
