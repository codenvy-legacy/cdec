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

import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class SaasApiServiceProxy {
    private final String        saasApiEndpoint;
    private final HttpTransport transport;

    @Inject
    public SaasApiServiceProxy(@Named("saas.api.endpoint") String saasApiEndpoint,
                               HttpTransport transport) {
        this.saasApiEndpoint = saasApiEndpoint;
        this.transport = transport;
    }

    public int getUsersCount() {
        return 0;
    }
}
