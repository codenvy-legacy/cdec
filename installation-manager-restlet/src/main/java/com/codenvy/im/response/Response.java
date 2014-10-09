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
package com.codenvy.im.response;

import com.codenvy.dto.server.JsonStringMapImpl;
import com.codenvy.im.artifacts.Artifact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class Response {
    private final Map<String, Object> params;

    private Response(Map<String, Object> params) {
        this.params = params;
    }

    public static Response valueOf(Exception e) {
        Builder builder = new Builder();
        return builder.withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).build();
    }

    public String toJson() {
        return new JsonStringMapImpl<>(params).toJson();
    }

    /** Response builder. */
    public static class Builder {
        private final Map<String, Object> params;

        public Builder() {
            params = new LinkedHashMap<>();
        }

        public Response build() {
            return new Response(params);
        }

        public Builder withArtifacts(Map<Artifact, String> m) {
            Set<Map.Entry<Artifact, String>> entries = m.entrySet();
            List<ArtifactInfo> infos = new ArrayList<>(entries.size());

            for (Map.Entry<Artifact, String> e : entries) {
                Artifact artifactName = e.getKey();
                String version = e.getValue();

                infos.add(new ArtifactInfo(artifactName, version));
            }

            return withArtifacts(infos);
        }

        public Builder withArtifacts(List<ArtifactInfo> l) {
            params.put(Property.ARTIFACTS.toString().toLowerCase(), l);
            return this;
        }

        public Builder withArtifact(ArtifactInfo info) {
            return withArtifacts(Arrays.asList(info));
        }

        public Builder withStatus(ResponseCode value) {
            return withParam(Property.STATUS, value.toString());
        }

        public Builder withMessage(String value) {
            return withParam(Property.MESSAGE, value);
        }

        public Builder withParam(String key, Object value) {
            params.put(key.toLowerCase(), value);
            return this;
        }

        private Builder withParam(Property key, String value) {
            params.put(key.toString().toLowerCase(), value);
            return this;
        }

    }
}
