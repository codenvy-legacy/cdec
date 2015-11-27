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
package com.codenvy.im.managers.helper;

import com.codenvy.im.managers.Config;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class LdapManagerHelperCodenvy4Impl extends LdapManagerHelper {

    public LdapManagerHelperCodenvy4Impl(Config config) {
        super(config);
    }

    @Override
    public String getRootPrincipal() {
        return format("cn=root,%s", config.getValue(Config.USER_LDAP_DN));  // TODO [ndp] verify
    }

    @Override
    public String getNameOfObjectToChangePassword() {
        return config.getValue(Config.USER_LDAP_USER_CONTAINER_DN);  // TODO [ndp] verify
    }
}
