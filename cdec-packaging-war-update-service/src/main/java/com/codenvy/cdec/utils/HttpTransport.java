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

import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @author Anatoliy Bazko
 * @author Dimitry Kuleshov
 * @author Alexander Reshetnyak
 */
@Singleton
public class HttpTransport {
    private static final Logger LOG = LoggerFactory.getLogger(HttpTransport.class);

    private final String apiEndpoint;

    @Inject
    public HttpTransport(@Named("api.endpoint") String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
        LOG.info("Http transport has been initialized, API endpoint: " + apiEndpoint);
    }

    /**
     * Performs GET request.
     */
    public <DTO> List<DTO> makeGetRequest(String path, Class<DTO> dtoInterface) throws IOException {
        String json = request(path, "GET");
        return DtoFactory.getInstance().createListDtoFromJson(json, dtoInterface);
    }

    protected String addAuthenticationToken(String baseUrl) throws IOException {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return baseUrl + (baseUrl.contains("?") ? "&" : "?") + "token=" + user.getToken();
        } else {
            throw new IOException("Authentication token not found");
        }
    }

    protected String request(String path, String method) throws IOException {
        String resourceUrl = addAuthenticationToken(combine(apiEndpoint, path));
        final HttpURLConnection conn = (HttpURLConnection)new URL(resourceUrl).openConnection();

        conn.setConnectTimeout(30 * 1000);
        try {
            conn.setRequestMethod(method);
            final int responseCode = conn.getResponseCode();

            if ((responseCode / 100) != 2) {
                InputStream in = conn.getErrorStream();
                if (in == null) {
                    in = conn.getInputStream();
                }
                throw new IOException(IoUtil.readAndCloseQuietly(in));
            }

            final String contentType = conn.getContentType();
            if (!contentType.startsWith("application/json")) {
                throw new IOException("Unsupported type of response from remote server. ");
            }

            return IoUtil.readAndCloseQuietly(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private String combine(String apiEndpoint, String path) {
        if (apiEndpoint.endsWith("/")) {
            if (path.startsWith("/")) {
                return apiEndpoint + path.substring(1);
            } else {
                return apiEndpoint + path;
            }
        } else {
            if (path.startsWith("/")) {
                return apiEndpoint + path;
            } else {
                return apiEndpoint + "/" + path;
            }
        }
    }
}
