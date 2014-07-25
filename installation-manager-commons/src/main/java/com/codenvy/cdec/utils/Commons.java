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

import com.codenvy.dto.server.DtoFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

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
}
