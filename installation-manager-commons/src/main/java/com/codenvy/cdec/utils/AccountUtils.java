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

import java.io.IOException;
import java.util.List;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.rest.shared.dto.Link;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class AccountUtils {
    public static final String PATH_TO_SUBSCRIPTIONS_NOT_FOUND_ERROR = "Path to subscriptions hasn't found.";
    public static final String PROPER_ACOUNT_NOT_FOUND_ERROR = "Proper account hasn't found.";
    
    
    /**
     * Indicates of current user has valid subscription.
     *
     * @throws java.lang.IllegalStateException, IOException
     */
    public static boolean isValidSubscription(HttpTransport transport, String apiEndpoint, String requiredSubscription) throws IllegalStateException,
                                                                                                                       IOException {
        return isValidSubscription(transport, apiEndpoint, requiredSubscription, null);
    }

    
    /**
     * Indicates of current user has valid subscription.
     *
     * @throws java.lang.IllegalStateException
     */
    public static boolean isValidSubscription(HttpTransport transport, String apiEndpoint, String requiredSubscription, String authToken)
            throws IOException, IllegalStateException {

        List<SubscriptionDescriptor> subscriptions = getSubscriptions(transport, apiEndpoint, authToken);

        for (SubscriptionDescriptor s : subscriptions) {
            if (s.getServiceId().equals(requiredSubscription)) {
                return true;
            }
        }

        return false;
    }

    public static List<SubscriptionDescriptor> getSubscriptions(HttpTransport transport, String apiEndpoint, String authToken) throws IOException {
        MemberDescriptor account = getProperAccount(transport, apiEndpoint, authToken);
        if (account == null) {
            throw new IllegalStateException(PROPER_ACOUNT_NOT_FOUND_ERROR);
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
        
        return Commons.createListDtoFromJson(transport.doGetRequest(subscriptionsHref, authToken), SubscriptionDescriptor.class);            
    }
    
    public static MemberDescriptor getProperAccount(HttpTransport transport, String apiEndpoint, String authToken) throws IOException {
        List<MemberDescriptor> accounts =
            Commons.createListDtoFromJson(transport.doGetRequest(Commons.combinePaths(apiEndpoint, "account"), authToken), MemberDescriptor.class);
        
        for (MemberDescriptor account: accounts) {
            if (account.getRoles().contains("account/owner")) {   // only account owner can use account subscription to install CDEC
                return account;
            }
        }

        return null;
    }
    
    public static String getSubscriptionsHref(MemberDescriptor account) {
        List<Link> links = account.getLinks();
        for (Link link: links) {
            if (link.getRel().equals("subscriptions")) {
                return link.getHref();
            }
        }
        
        return null;
    }

    public static String getSubscriptionsHref(String apiEndpoint, String accountId) {
        return Commons.combinePaths(apiEndpoint, "account/" + accountId + "/subscriptions");
    }
    
    public static String getAccountId(MemberDescriptor account) {
        AccountReference reference = account.getAccountReference();
        if (reference != null) {
            return account.getAccountReference().getId();
        }
        
        return null;
    }
}