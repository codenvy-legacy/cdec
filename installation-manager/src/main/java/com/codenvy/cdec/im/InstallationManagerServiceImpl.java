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
package com.codenvy.cdec.im;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.exceptions.ArtifactNotFoundException;
import com.codenvy.cdec.response.*;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.user.UserCredentials;
import com.codenvy.cdec.utils.InjectorBootstrap;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ServerResource;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.cdec.utils.Commons.extractServerUrl;
import static com.codenvy.cdec.utils.InjectorBootstrap.INJECTOR;
import static java.util.Arrays.asList;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    protected final InstallationManager manager;

    private final String updateServerUrl;

    public InstallationManagerServiceImpl() {
        this.manager = INJECTOR.getInstance(InstallationManagerImpl.class);
        updateServerUrl = extractServerUrl(InjectorBootstrap.getProperty("codenvy.installation-manager.update_endpoint"));
    }

    /** For testing purpose only. */
    @Deprecated
    protected InstallationManagerServiceImpl(InstallationManager manager) {
        this.manager = manager;
        updateServerUrl = extractServerUrl(InjectorBootstrap.getProperty("codenvy.installation-manager.update_endpoint"));
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdateServerUrl() {
        return updateServerUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String download(JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();
            
            Map<Artifact, String> updates = manager.getUpdates(token);

            List<ArtifactInfo> infos = new ArrayList<>();

            for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
                Artifact artifact = entry.getKey();
                String version = entry.getValue();

                try {
                    doDownload(token, artifact, version);
                    infos.add(new ArtifactInfoEx(artifact, version, Status.SUCCESS));
                } catch (Exception e) {
                    infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                    return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();
            
            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            String version = manager.getUpdates(token).get(artifact);
            if (version == null) {
                throw new ArtifactNotFoundException(artifact.getName());
            }

            return download(artifactName, version, userCredentialsRep);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName, String version, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();            
            
            doDownload(token, artifactName, version);
            ArtifactInfo info = new ArtifactInfoEx(artifactName, version, Status.SUCCESS);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdates(JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();
            
            Map<Artifact, String> updates = manager.getUpdates(token);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(updates).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void obtainChallengeRequest() {
        // do nothing
    }

    protected void doDownload(String token, String artifactName, @Nullable String version) throws IOException, IllegalStateException {
        doDownload(token, ArtifactFactory.createArtifact(artifactName), version);
    }

    protected void doDownload(String token, Artifact artifact, @Nullable String version) throws IOException, IllegalStateException {
        manager.download(token, artifact, version);
    }

    /** {@inheritDoc} */
    @Override
    public String install(String token) throws IOException {
        Map<Artifact, String> updates = manager.getUpdates(token);

        List<ArtifactInfo> infos = new ArrayList<>();

        for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            try {
                doInstall(artifact, version, token);
                infos.add(new ArtifactInfoEx(artifact, version, Status.SUCCESS));
            } catch (Exception e) {
                infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
            }
        }

        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String install(String artifactName, String token) throws IOException {
        return install(artifactName, null, token);
    }

    /** {@inheritDoc} */
    @Override
    public String install(String artifactName, @Nullable String version, String token) throws IOException {

        Artifact artifact = ArtifactFactory.createArtifact(artifactName);
        String toInstallVersion = version != null ? version : manager.getUpdates(token).get(artifact);

        if (toInstallVersion == null) {
            return Response.valueOf(new IllegalStateException("Artifact '" + artifactName + "' isn't available to update.")).toJson();
        }

        try {
            doInstall(artifact, toInstallVersion, token);
            ArtifactInfo info = new ArtifactInfoEx(artifactName, toInstallVersion, Status.SUCCESS);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(asList(new ArtifactInfo[]{info})).build().toJson();
        } catch (Exception e) {
            ArtifactInfo info = new ArtifactInfoEx(artifactName, toInstallVersion, Status.FAILURE);
            return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(asList(new ArtifactInfo[]{info}))
                                         .build().toJson();
        }
    }

    protected void doInstall(Artifact artifact, String version, String token) throws IOException, IllegalStateException {
        manager.install(token, artifact, version);
    }
    
}