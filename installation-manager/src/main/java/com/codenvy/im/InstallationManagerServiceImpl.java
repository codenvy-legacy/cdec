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
package com.codenvy.im;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.ArtifactInfoEx;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ServerResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.utils.Commons.extractServerUrl;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static com.codenvy.im.utils.InjectorBootstrap.getProperty;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    protected final InstallationManager manager;

    private final String updateServerUrl;

    public InstallationManagerServiceImpl() {
        this.manager = INJECTOR.getInstance(InstallationManagerImpl.class);
        updateServerUrl = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
    }

    /** For testing purpose only. */
    @Deprecated
    protected InstallationManagerServiceImpl(InstallationManager manager) {
        this.manager = manager;
        updateServerUrl = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
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
                    Path file = doDownload(userCredentials, artifact, version);
                    infos.add(new DownloadArtifactInfo(artifact, version, file.toString(), Status.SUCCESS));
                } catch (Exception e) {
                    infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                    return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
        } catch (Exception e) {
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

            try {
                Path file = doDownload(userCredentials, artifact, version);
                ArtifactInfo info = new DownloadArtifactInfo(artifact, version, file.toString(), Status.SUCCESS);
                return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
            } catch (Exception e) {
                ArtifactInfoEx info = new ArtifactInfoEx(artifact, version, Status.FAILURE);
                return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifact(info).build().toJson();
            }
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName, String version, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            Path file = doDownload(userCredentials, artifactName, version);
            ArtifactInfo info = new DownloadArtifactInfo(artifactName, version, file.toString(), Status.SUCCESS);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
        } catch (Exception e) {
            ArtifactInfo info = new ArtifactInfoEx(artifactName, version, Status.FAILURE);
            return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifact(info).build().toJson();
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

    protected Path doDownload(UserCredentials userCredentials,
                              String artifactName,
                              @Nullable String version) throws IOException, IllegalStateException {
        return doDownload(userCredentials, ArtifactFactory.createArtifact(artifactName), version);
    }

    protected Path doDownload(UserCredentials userCredentials,
                              Artifact artifact,
                              @Nullable String version) throws IOException, IllegalStateException {
        return manager.download(userCredentials, artifact, version);
    }

    /** {@inheritDoc} */
    @Override
    public String install(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        String token = userCredentials.getToken();

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
    public String getVersions(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        Map<Artifact, String> installedArtifacts = manager.getInstalledArtifacts(userCredentials.getToken());
        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(installedArtifacts).build().toJson();
    }


    /** {@inheritDoc} */
    @Override
    public String install(String artifactName, JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        return install(artifactName, null, userCredentialsRep);
    }

    /** {@inheritDoc} */
    @Override
    public String install(String artifactName, @Nullable String version, JacksonRepresentation<UserCredentials> userCredentialsRep)
            throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        String token = userCredentials.getToken();

        Artifact artifact = ArtifactFactory.createArtifact(artifactName);
        String toInstallVersion = version != null ? version : manager.getUpdates(token).get(artifact);

        if (toInstallVersion == null) {
            return Response.valueOf(new IllegalStateException("Artifact '" + artifactName + "' isn't available to update.")).toJson();
        }

        try {
            doInstall(artifact, toInstallVersion, token);
            ArtifactInfo info = new ArtifactInfoEx(artifactName, toInstallVersion, Status.SUCCESS);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
        } catch (Exception e) {
            ArtifactInfo info = new ArtifactInfoEx(artifactName, toInstallVersion, Status.FAILURE);
            return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifact(info).build().toJson();
        }
    }

    protected void doInstall(Artifact artifact, String version, String token) throws IOException, IllegalStateException {
        manager.install(token, artifact, version);
    }

}