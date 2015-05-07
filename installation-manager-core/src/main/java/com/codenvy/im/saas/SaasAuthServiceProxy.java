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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.JsonStringMapImpl;

import javax.inject.Named;
import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createDtoFromJson;

/**
 * @author Dmytro Nochevnov
 */
@Singleton
public class SaasAuthServiceProxy {
    public static final String CANNOT_LOGIN = "Login impossible.";

    private final String        saasApiEndpoint;
    private final HttpTransport transport;

    @Inject
    public SaasAuthServiceProxy(@Named("saas.api.endpoint") String saasApiEndpoint,
                                HttpTransport transport) {
        this.saasApiEndpoint = saasApiEndpoint;
        this.transport = transport;
    }

    /**
     * Login to Codenvy SaaS.
     */
    public Token login(Credentials credentials) throws IOException, JsonParseException {
        String requestUrl = combinePaths(saasApiEndpoint, "/auth/login");

        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", credentials.getUsername(),
                                                              "password", credentials.getPassword()));

        String response = transport.doPost(requestUrl, body);
        if (response == null) {
            return null;
        }
        return createDtoFromJson(response, Token.class);
    }
}
