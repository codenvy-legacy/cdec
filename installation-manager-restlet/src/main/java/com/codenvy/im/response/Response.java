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
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"downloadInfo", "config", "artifacts", "subscription", "message", "status"})
@JsonIgnoreProperties({"CLI client version"})
public class Response {
    private List<ArtifactInfo>            artifacts;
    private String                        message;
    private ResponseCode                  status;
    private DownloadStatusInfo            downloadInfo;
    private LinkedHashMap<String, String> config;
    private List<String>                  infos;
    private String                        subscription;

    public Response() {
    }

    public static Response valueOf(Exception e) {
        return new Response().setStatus(ResponseCode.ERROR)
                             .setMessage(e.getMessage());
    }

    public String toJson() {
        try {
            return Commons.toJson(this);
        } catch (JsonProcessingException e) {
            return String.format("{status : ERROR, message : %s}", e.getMessage());
        }
    }

    public static Response fromJson(String json) throws JsonParseException {
        return Commons.fromJson(json, Response.class);
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

    public Response setArtifacts(List<ArtifactInfo> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public Response addArtifacts(Map<Artifact, Version> m) {
        Set<Map.Entry<Artifact, Version>> entries = m.entrySet();
        List<ArtifactInfo> infos = new ArrayList<>(entries.size());

        for (Map.Entry<Artifact, Version> e : entries) {
            Artifact artifactName = e.getKey();
            Version version = e.getValue();

            infos.add(new ArtifactInfo(artifactName, version));
        }

        ensureArtifactsIsNotNull();
        this.artifacts.addAll(infos);
        return this;
    }

    public Response addArtifact(ArtifactInfo info) {
        ensureArtifactsIsNotNull();
        artifacts.add(info);
        return this;
    }

    public Response setMessage(String message) {
        this.message = message;
        return this;
    }

    public Response setStatus(ResponseCode status) {
        this.status = status;
        return this;
    }

    public DownloadStatusInfo getDownloadInfo() {
        return downloadInfo;
    }

    public Response setDownloadInfo(DownloadStatusInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        return this;
    }

    public LinkedHashMap<String, String> getConfig() {
        return config;
    }

    public Response setConfig(LinkedHashMap<String, String> config) {
        this.config = config;
        return this;
    }

    public String getSubscription() {
        return subscription;
    }

    public Response setSubscription(String subscription) {
        this.subscription = subscription;
        return this;
    }

    public List<String> getInfos() {
        return infos;
    }

    public Response setInfos(List<String> infos) {
        this.infos = infos;
        return this;
    }

    private void ensureArtifactsIsNotNull() {
        if (this.artifacts == null) {
            this.artifacts = new ArrayList<>();
        }
    }
}
