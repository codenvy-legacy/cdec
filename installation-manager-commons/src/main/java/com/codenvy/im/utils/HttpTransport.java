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

import com.codenvy.commons.lang.IoUtil;
import com.google.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.commons.lang.IoUtil.deleteRecursive;
import static com.codenvy.im.utils.Commons.copyInterruptable;
import static java.nio.file.Files.newOutputStream;

/**
 * @author Anatoliy Bazko
 * @author Dimitry Kuleshov
 * @author Alexander Reshetnyak
 */
@Singleton
public class HttpTransport {
    private static final Pattern FILE_NAME = Pattern.compile("attachment; filename=(.*)");
    private static final String  MESSAGE   = "message";

    private final HttpTransportConfiguration transportConf;

    @Inject
    public HttpTransport(HttpTransportConfiguration transportConf) {
        this.transportConf = transportConf;
    }

    /**
     * Performs OPTION request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doOption(String path, String accessToken) throws IOException {
        return request(path, "OPTIONS", MediaType.APPLICATION_JSON, accessToken);
    }

    /**
     * Performs GET request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doGet(String path) throws IOException {
        return doGet(path, null);
    }

    /**
     * Performs GET request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doGet(String path, String accessToken) throws IOException {
        return request(path, "GET", MediaType.APPLICATION_JSON, accessToken);
    }

    private String request(String path, String method, String expectedContentType, @Nullable String accessToken) throws IOException {
        final HttpURLConnection conn = openConnection(path, accessToken);

        try {
            request(method, expectedContentType, conn);
            return IoUtil.readAndCloseQuietly(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Performs GET request and store response into file.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_OCTET_STREAM}
     */
    public Path download(String path, Path destinationDir) throws IOException {
        return download(path, destinationDir, null);
    }

    /**
     * Performs GET request and store response into file.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_OCTET_STREAM}
     */
    public Path download(String path, Path destinationDir, String accessToken) throws IOException {
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        return download(path, "GET", MediaType.APPLICATION_OCTET_STREAM, destinationDir, accessToken);
    }

    private Path download(String path, String method, String expectedContentType, Path destinationDir, String accessToken) throws IOException {
        final HttpURLConnection conn = openConnection(path, accessToken);

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

                Path file = destinationDir.resolve(fileName);
                try (OutputStream out = new BufferedOutputStream(newOutputStream(file))) {
                    copyInterruptable(in, out);
                } catch (CopyStreamInterruptedException e) {
                    deleteRecursive(destinationDir.toFile());
                    throw new IOException("Downloading was canceled");
                }

                return file;
            }
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

            throw new HttpException(responseCode, getErrorMessageIfPossible(IoUtil.readAndCloseQuietly(in)));
        }

        final String contentType = conn.getContentType();
        if (!contentType.startsWith(expectedContentType)) {
            throw new IOException("Unsupported type of response from remote server. ");
        }
    }

    /** Checks if a response has the specific error message and take it if possible. */
    private String getErrorMessageIfPossible(String json) {
        try {
            JSONObject jsonResponse = new JSONObject(json);
            String message = (String)jsonResponse.get(MESSAGE);

            return message != null ? message : json;
        } catch (JSONException e) {
            return json;
        }
    }

    protected HttpURLConnection openConnection(String path, @Nullable String accessToken) throws IOException {
        HttpURLConnection connection;
        if (transportConf.isProxyConfValid()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(transportConf.getProxyUrl(), transportConf.getProxyPort()));
            connection = (HttpURLConnection)new URL(path).openConnection(proxy);
        } else {
            connection = (HttpURLConnection)new URL(path).openConnection();
        }

        if (accessToken != null) {
            String accessTokenCookie = String.format("session-access-key=%s;", accessToken);
            connection.addRequestProperty("Cookie", accessTokenCookie);
        }

        return connection;
    }
}
