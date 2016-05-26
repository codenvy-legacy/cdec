/*
 *  2012-2016 Codenvy, S.A.
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

import org.eclipse.che.dto.server.DtoFactory;

import org.eclipse.che.commons.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.utils.Commons.copyInterruptable;
import static java.nio.file.Files.newOutputStream;
import static org.eclipse.che.commons.lang.IoUtil.deleteRecursive;
import static org.eclipse.che.commons.lang.IoUtil.readAndCloseQuietly;

/**
 * @author Anatoliy Bazko
 * @author Alexander Reshetnyak
 */
@Singleton
public class HttpTransport {
    private static final Pattern FILE_NAME = Pattern.compile("attachment; filename=(.*)");

    /**
     * Performs OPTION request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doOption(String path, String accessToken) throws IOException {
        return request(path, "OPTIONS", null, MediaType.APPLICATION_JSON, accessToken);
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
        return request(path, "GET", null, MediaType.APPLICATION_JSON, accessToken);
    }

    /**
     * Performs DELETE request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public void doDelete(String path, String accessToken) throws IOException {
        request(path, "DELETE", null, null, accessToken);
    }

    /**
     * Performs POST request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     */
    public String doPost(String path, Object body, String accessToken) throws IOException {
        return request(path, "POST", body, MediaType.APPLICATION_JSON, accessToken);
    }

    /**
     * POST request.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_JSON}
     *
     * @throws com.codenvy.im.utils.HttpException
     *         if request failed
     */
    public String doPost(String path, Object body) throws HttpException, IOException {
        return request(path, "POST", body, MediaType.APPLICATION_JSON, null);
    }

    private String request(String path,
                           String method,
                           @Nullable Object body,
                           @Nullable String expectedContentType,
                           @Nullable String accessToken) throws HttpException, IOException {
        HttpURLConnection conn = null;

        try {
            conn = openConnection(path, accessToken);
            request(method, body, expectedContentType, conn);
            return readAndCloseQuietly(conn.getInputStream());
        } catch (SocketTimeoutException e) { // catch exception and throw a new one with proper message
            URL url = new URL(path);
            throw new HttpException(-1, String.format("Can't establish connection with %s://%s", url.getProtocol(), url.getHost()));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Performs GET request and store response into file.
     * Expected content type {@link javax.ws.rs.core.MediaType#APPLICATION_OCTET_STREAM}
     */
    public Path download(String path, Path destinationDir) throws IOException {
        if (!Files.exists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        return download(path, "GET", MediaType.APPLICATION_OCTET_STREAM, destinationDir);
    }

    private Path download(String path, String method, String expectedContentType, Path destinationDir) throws IOException {
        final HttpURLConnection conn = openConnection(path, null);

        try {
            request(method, null, expectedContentType, conn);

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

    private void request(String method,
                         @Nullable Object body,
                         @Nullable String expectedContentType,
                         HttpURLConnection conn) throws HttpException, IOException {
        conn.setConnectTimeout(30 * 1000);
        conn.setRequestMethod(method);
        if (body != null) {
            conn.addRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream output = conn.getOutputStream()) {
                output.write(DtoFactory.getInstance().toJson(body).getBytes("UTF-8"));
            }
        }
        final int responseCode = conn.getResponseCode();

        if ((responseCode / 100) != 2) {
            InputStream in = conn.getErrorStream();
            if (in == null) {
                in = conn.getInputStream();
            }

            throw new HttpException(responseCode, readAndCloseQuietly(in));
        }

        final String contentType = conn.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase(expectedContentType)) {
            throw new IOException("Unsupported type of response from remote server.");
        }
    }

    protected HttpURLConnection openConnection(String path, @Nullable String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(path).openConnection();

        if (accessToken != null) {
            String accessTokenCookie = String.format("session-access-key=%s;", accessToken);
            connection.addRequestProperty("Cookie", accessTokenCookie);
        }

        return connection;
    }
}
