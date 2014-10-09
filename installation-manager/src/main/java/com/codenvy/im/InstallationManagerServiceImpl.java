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

import com.codenvy.dto.server.JsonStringMapImpl;
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
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ServerResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.utils.AccountUtils.isValidSubscription;
import static com.codenvy.im.utils.Commons.extractServerUrl;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static com.codenvy.im.utils.InjectorBootstrap.getProperty;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    protected final InstallationManager manager;
    protected final HttpTransport       transport;

    private final String updateServerEndpoint;
    private final String apiEndpoint;

    public InstallationManagerServiceImpl() {
        this.manager = INJECTOR.getInstance(InstallationManagerImpl.class);
        this.transport = INJECTOR.getInstance(HttpTransport.class);
        this.updateServerEndpoint = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
        this.apiEndpoint = getProperty("api.endpoint");
    }

    /** For testing purpose only. */
    @Deprecated
    protected InstallationManagerServiceImpl(InstallationManager manager, HttpTransport transport) {
        this.manager = manager;
        this.transport = transport;
        this.updateServerEndpoint = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
        this.apiEndpoint = getProperty("api.endpoint");
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdateServerEndpoint() {
        return updateServerEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String checkSubscription(String subscription, JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        try {
            boolean subscriptionValidated = isValidSubscription(transport, apiEndpoint, subscription, userCredentials);

            if (subscriptionValidated) {
                return new Response.Builder().withStatus(ResponseCode.OK)
                                             .withParam("Subscription", subscription)
                                             .withMessage("Subscription is valid")
                                             .build().toJson();
            } else {
                return new Response.Builder().withStatus(ResponseCode.ERROR)
                                             .withParam("Subscription", subscription)
                                             .withMessage("Subscription not found").build().toJson();
            }
        } catch (Exception e) {
            return new Response.Builder().withStatus(ResponseCode.ERROR)
                                         .withParam("Subscription", subscription)
                                         .withMessage(e.getMessage())
                                         .build().toJson();
        }
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

            Artifact artifact = createArtifact(artifactName);
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
    public String getDownloads(String artifactName) {
        try {
            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            List<ArtifactInfo> infos = new ArrayList<>();
            SortedMap<Version, Path> versions = downloadedArtifacts.get(ArtifactFactory.createArtifact(artifactName));

            if (versions != null && !versions.isEmpty()) {
                for (Map.Entry<Version, Path> e : versions.entrySet()) {
                    Version version = e.getKey();
                    Path pathToBinaries = e.getValue();

                    infos.add(new DownloadArtifactInfo(artifactName, version.toString(), pathToBinaries.toString(), Status.DOWNLOADED));
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloads(String artifactName, String version) {
        try {
            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            Version v = Version.valueOf(version);

            List<ArtifactInfo> infos = new ArrayList<>();
            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            if (downloadedArtifacts.get(artifact) != null && downloadedArtifacts.get(artifact).containsKey(v)) {
                Path pathToBinaries = downloadedArtifacts.get(artifact).get(v);
                infos.add(new DownloadArtifactInfo(artifactName, version, pathToBinaries.toString(), Status.DOWNLOADED));

                return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
            } else {
                return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
            }
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloads() {
        try {
            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            List<ArtifactInfo> infos = new ArrayList<>();
            for (Map.Entry<Artifact, SortedMap<Version, Path>> artifact : downloadedArtifacts.entrySet()) {
                for (Map.Entry<Version, Path> e : artifact.getValue().entrySet()) {
                    Version version = e.getKey();
                    Path pathToBinaries = e.getValue();

                    infos.add(new DownloadArtifactInfo(artifact.getKey(), version.toString(), pathToBinaries.toString(), Status.DOWNLOADED));
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
        } catch (Exception e) {
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
            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            List<ArtifactInfo> infos = new ArrayList<>(updates.size());
            for (Map.Entry<Artifact, String> e : updates.entrySet()) {
                Artifact artifact = e.getKey();
                String version = e.getValue();

                if (downloadedArtifacts.containsKey(artifact)
                    && downloadedArtifacts.get(artifact).containsKey(Version.valueOf(version))) {

                    infos.add(new ArtifactInfoEx(artifact, version, Status.DOWNLOADED));
                } else {
                    infos.add(new ArtifactInfo(artifact, version));
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
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
        return doDownload(userCredentials, createArtifact(artifactName), version);
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
    public String install(String artifactName,
                          @Nullable String version,
                          JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        String token = userCredentials.getToken();

        Artifact artifact = createArtifact(artifactName);
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

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getAccountIdWhereUserIsOwner(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        String token = userCredentials.getToken();
        return AccountUtils.getAccountIdWhereUserIsOwner(transport, apiEndpoint, token);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean isValidAccountId(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException {
        UserCredentials userCredentials = userCredentialsRep.getObject();
        return AccountUtils.isValidAccountId(transport, apiEndpoint, userCredentials);
    }

    /** {@inheritDoc} */
    @Override
    public String getConfig() {
        JsonStringMapImpl<String> config = new JsonStringMapImpl<>(manager.getConfig());
        return new Response.Builder().withStatus(ResponseCode.OK).withParam("config", config).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String setConfig(String downloadDir) {
        try {
            manager.setConfig(downloadDir);
            return new Response.Builder().withStatus(ResponseCode.OK).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    protected void doInstall(Artifact artifact, String version, String token) throws IOException, IllegalStateException {
        manager.install(token, artifact, version);
    }
}
