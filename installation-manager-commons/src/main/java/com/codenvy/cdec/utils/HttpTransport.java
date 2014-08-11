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

import org.apache.commons.io.IOUtils;

import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.cdec.utils.Commons.addQueryParam;

/**
 * @author Anatoliy Bazko
 * @author Dimitry Kuleshov
 * @author Alexander Reshetnyak
 */
@Singleton
public class HttpTransport {
    private static final Pattern FILE_NAME = Pattern.compile("attachment; filename=(.*)");

    /**
     * Performs GET request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doGetRequest(String path) throws IOException {
        return request(path, "GET", MediaType.APPLICATION_JSON);
    }

    /**
     * Performs GET request and store response into file.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_OCTET_STREAM}
     */
    public void download(String path, Path destinationDir) throws IOException {
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        download(path, "GET", MediaType.APPLICATION_OCTET_STREAM, destinationDir);
    }

    private void download(String path, String method, String expectedContentType, Path destinationDir) throws IOException {
        final HttpURLConnection conn = openConnection(path);

        try {
            request(method, expectedContentType, conn);

            String headerField = conn.getHeaderField("Content-Disposition");
            if (headerField == null) {
                throw new IOException("File name is unknown");
            }

            Matcher matcher = FILE_NAME.matcher(headerField);
            if (!matcher.find()) {
                throw new IOException("File name is unknown");
            }

            try (InputStream in = conn.getInputStream()) {
                String fileName = matcher.group(1);
                if (fileName.isEmpty()) {
                    throw new IOException("File name is unknown");
                }

                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(destinationDir.resolve(fileName)))) {
                    IOUtils.copy(in, out);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String request(String path, String method, String expectedContentType) throws IOException {
        final HttpURLConnection conn = openConnection(path);
        try {
            request(method, expectedContentType, conn);
            return IoUtil.readAndCloseQuietly(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private void request(String method, String expectedContentType, HttpURLConnection conn) throws IOException {
        conn.setConnectTimeout(30 * 1000);
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
        if (!contentType.startsWith(expectedContentType)) {
            throw new IOException("Unsupported type of response from remote server. ");
        }
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        String resourceUrl = addAuthenticationToken(path);
        return (HttpURLConnection)new URL(resourceUrl).openConnection();
    }

    private String addAuthenticationToken(String baseUrl) {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return addQueryParam(baseUrl, "token", user.getToken());
        } else {
            return baseUrl;
        }
    }
}
