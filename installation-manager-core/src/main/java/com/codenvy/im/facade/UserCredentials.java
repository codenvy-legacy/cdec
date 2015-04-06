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
package com.codenvy.im.facade;

import javax.annotation.Nonnull;

/**
 * AccountID refers to the account where user has account/owner role.
 *
 * @author Dmytro Nochevnov
 */
public class UserCredentials {
    private String token;
    private String accountId;

    public UserCredentials(String token, String accountId) {
        this.token = token;
        this.accountId = accountId;
    }

    public UserCredentials(String token) {
        this.token = token;
    }

    /** Getter for #token */
    @Nonnull
    public String getToken() {
        return token;
    }

    /** Setter for #token */
    public void setToken(String token) {
        this.token = token;
    }

    /** Getter for #accountId */
    @Nonnull
    public String getAccountId() {
        return accountId;
    }

    /** Setter for #accountId */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserCredentials)) return false;

        UserCredentials that = (UserCredentials)o;

        if (!accountId.equals(that.accountId)) return false;
        if (!token.equals(that.token)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = token.hashCode();
        result = 31 * result + accountId.hashCode();
        return result;
    }
}
