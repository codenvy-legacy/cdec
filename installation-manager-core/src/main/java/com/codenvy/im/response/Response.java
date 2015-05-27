/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.eclipse.che.commons.json.JsonParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@JsonPropertyOrder({"properties", "artifacts", "subscription", "node", "backup", "authToken", "message", "status"})
@JsonIgnoreProperties({"CLI client version"})
public class Response {
    private List<ArtifactInfo>  artifacts;
    private String              message;
    private ResponseCode        status;
    private Map<String, String> properties;
    private List<String>        infos;
    private String              subscription;
    private NodeInfo            node;
    private BackupInfo          backup;

    public Response() {
    }

    public static Response ok() {
        return new Response().setStatus(ResponseCode.OK);
    }

    public static Response error(Throwable e) {
        return new Response().setStatus(ResponseCode.ERROR).setMessage(e.getMessage());
    }

    public static Response error() {
        return new Response().setStatus(ResponseCode.ERROR);
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public Response setProperties(Map<String, String> properties) {
        this.properties = properties;
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

    public NodeInfo getNode() {
        return node;
    }

    public Response setNode(NodeInfo node) {
        this.node = node;
        return this;
    }

    public BackupInfo getBackup() {
        return backup;
    }

    public Response setBackup(BackupInfo backup) {
        this.backup = backup;
        return this;
    }

    private void ensureArtifactsIsNotNull() {
        if (this.artifacts == null) {
            this.artifacts = new ArrayList<>();
        }
    }

    /**
     * Return true if only parameter 'response' is valid json with property "status": "ERROR".
     */
    public static boolean isError(String response) throws JsonParseException {
        Response responseObj = Response.fromJson(response);
        return responseObj.getStatus() == ResponseCode.ERROR;
    }
}
