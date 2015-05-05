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

import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableMap;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.JsonStringMapImpl;

import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;

/**
 * @author Dmytro Nochevnov
 */
public class CodenvyUtils {

    public static final String CANNOT_LOGIN = "Login impossible.";

    /** Utility class so there is no public constructor. */
    private CodenvyUtils() {
    }

    public static Token login(HttpTransport transport,
                       String apiEndpoint,
                       Credentials credentials) throws IOException, JsonParseException {

        String requestUrl = combinePaths(apiEndpoint, "/auth/login");
        Object body = new JsonStringMapImpl<>(ImmutableMap.of("username", credentials.getUsername(),
                                                              "password", credentials.getPassword()));
        String response = transport.doPost(requestUrl, body, null);
        return Commons.createDtoFromJson(response, Token.class);
    }
}
