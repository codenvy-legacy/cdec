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

import com.codenvy.api.user.shared.dto.UserDescriptor;

import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createDtoFromJson;

/**
 * @author Dmytro Nochevnov
 */
public class UserUtils {

    /** Utility class so there is no public constructor. */
    private UserUtils() {
    }

    public static String getUserEmail(HttpTransport transport,
                                      String apiEndpoint,
                                      String accessToken) throws IOException {
        String requestUrl = combinePaths(apiEndpoint, "user");
        UserDescriptor descriptor = createDtoFromJson(transport.doGet(requestUrl, accessToken), UserDescriptor.class);

        return descriptor.getEmail();
    }
}
