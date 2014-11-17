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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.utils.Commons;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"downloadInfo", "config", "artifacts", "subscription", "message", "status"})
public class Response {
    private List<ArtifactInfo>        artifacts;
    private String                    message;
    private ResponseCode              status;
    private DownloadStatusInfo        downloadInfo;
    private JsonStringMapImpl<String> config;
    private String                    subscription;

    public Response() {
    }

    public static Response valueOf(Exception e) {
        Builder builder = new Builder();
        return builder.withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).build();
    }

    public String toJson() {
        return Commons.toJson(this);   // TODO add test
    }

    // TODO add test
    public static Response fromJson(String json) throws JsonParseException {
        return Commons.fromJson(json, Response.class);
    }

    /** Response builder. */
    public static class Builder {
        private Response response;

        public Builder() {
            response = new Response();
        }

        public Response build() {
            return response;
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

        public Builder withArtifacts(@Nullable List<ArtifactInfo> l) {
            response.setArtifacts(l);
            return this;
        }

        public Builder withArtifact(ArtifactInfo info) {
            return withArtifacts(Arrays.asList(info));
        }

        public Builder withStatus(ResponseCode value) {
            response.setStatus(value);
            return this;
        }

        public Builder withMessage(String value) {
            response.setMessage(value);
            return this;
        }

        public Builder withDownloadInfo(DownloadStatusInfo info) {
            response.setDownloadInfo(info);
            return this;
        }

        public Builder withConfig(JsonStringMapImpl<String> config) {
            response.setConfig(config);
            return this;
        }

        public Builder withSubscription(String subscription) {
            response.setSubscription(subscription);
            return this;
        }
    }

    public ResponseCode getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<ArtifactInfo> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ArtifactInfo> artifacts) {
        this.artifacts = artifacts;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(ResponseCode status) {
        this.status = status;
    }

    public DownloadStatusInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(DownloadStatusInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public JsonStringMapImpl<String> getConfig() {
        return config;
    }

    public void setConfig(JsonStringMapImpl<String> config) {
        this.config = config;
    }

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }
}
