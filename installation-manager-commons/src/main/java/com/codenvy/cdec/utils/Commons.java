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

import com.codenvy.api.account.shared.dto.MemberDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.dto.server.DtoFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.jackson.JacksonRepresentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static com.codenvy.cdec.utils.Version.compare;
import static com.codenvy.cdec.utils.Version.valueOf;

/**
 * @author Anatoliy Bazko
 */
public class Commons {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    /**
     * Simplifies the way to combine paths. Takes care about normalization.
     */
    public static String combinePaths(String apiEndpoint, String path) {
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

    /**
     * Adds query parameter to url.
     */
    public static String addQueryParam(String path, String key, String value) {
        return path + (path.contains("?") ? "&" : "?") + key + "=" + value;
    }

    /**
     * Translates JSON to the list of DTO objects.
     */
    public static <DTO> List<DTO> createListDtoFromJson(String json, Class<DTO> dtoInterface) throws IOException {
        return DtoFactory.getInstance().createListDtoFromJson(json, dtoInterface);
    }

    /**
     * Translates JSON to object.
     */
    public static <T> T fromJson(String json, Class<T> t) {
        return gson.fromJson(json, t);
    }

    /**
     * @return the version of the artifact out of path
     */
    public static String extractVersion(Path pathToBinaries) {
        return pathToBinaries.getParent().getFileName().toString();
    }

    /**
     * @return the artifact name out of path
     */
    public static String extractArtifactName(Path pathToBinaries) {
        return pathToBinaries.getParent().getParent().getFileName().toString();
    }

    /**
     * @return the last version of the artifact in the directory
     * @throws com.codenvy.cdec.ArtifactNotFoundException
     *         if artifact is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public static String getLatestVersion(String artifact, Path dir) throws IOException {
        Version latestVersion = null;

        if (!Files.exists(dir)) {
            throw new ArtifactNotFoundException(artifact);
        }

        Iterator<Path> pathIterator = Files.newDirectoryStream(dir).iterator();
        while (pathIterator.hasNext()) {
            try {
                Path next = pathIterator.next();
                if (Files.isDirectory(next)) {
                    Version version = valueOf(next.getFileName().toString());
                    if (latestVersion == null || compare(version, latestVersion) > 0) {
                        latestVersion = version;
                    }
                }
            } catch (IllegalArgumentException e) {
                // maybe it isn't a version directory
            }
        }

        if (latestVersion == null) {
            throw new ArtifactNotFoundException(artifact);
        }

        return latestVersion.toString();
    }

    /**
     * Indicates of current user has valid subscription.
     *
     * @throws java.lang.IllegalStateException
     */
    public static boolean isValidSubscription(HttpTransport transport, String apiEndpoint, String requiredSubscription)
            throws IOException, IllegalStateException {

        List<MemberDescriptor> accounts =
                createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account")), MemberDescriptor.class);

        if (accounts.size() != 1) {
            throw new IllegalStateException("User must have only one account");
        }

        String accountId = accounts.get(0).getAccountReference().getId();
        List<SubscriptionDescriptor> subscriptions =
                createListDtoFromJson(transport.doGetRequest(combinePaths(apiEndpoint, "account/" + accountId + "/subscriptions")),
                                      SubscriptionDescriptor.class);

        for (SubscriptionDescriptor s : subscriptions) {
            if (s.getServiceId().equals(requiredSubscription)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convert one-line json string to pretty formatted multiline string.
     *
     * @throws JSONException
     */
    public static String getPrettyPrintingJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return obj.toString(2);
    }

    /**
     * Convert one-line json string to pretty formatted multiline string.
     *
     * @throws JSONException
     * @throws java.io.IOException
     */
    public static <T> String getPrettyPrintingJson(T obj) throws JSONException, IOException {
        return getPrettyPrintingJson(getJson(obj));
    }

    /**
     * Produce json from object.
     *
     * @throws java.io.IOException
     */
    public static <T> String getJson(T obj) throws IOException {
        JacksonRepresentation<T> jackson = new JacksonRepresentation<>(obj);
        return jackson.getText();
    }
}
