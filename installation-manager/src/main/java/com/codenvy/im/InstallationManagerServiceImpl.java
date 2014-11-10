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
import com.codenvy.im.installer.InstallInProgressException;
import com.codenvy.im.installer.InstallStartedException;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerConfig;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;

import static com.codenvy.im.DownloadingDescriptor.createDescriptor;
import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.response.ResponseCode.ERROR;
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

    private final DownloadingDescriptorHolder downloadingDescriptorHolder;

    private final String updateServerEndpoint;
    private final String apiEndpoint;

    public InstallationManagerServiceImpl() {
        this.manager = INJECTOR.getInstance(InstallationManagerImpl.class);
        this.transport = INJECTOR.getInstance(HttpTransport.class);
        this.downloadingDescriptorHolder = INJECTOR.getInstance(DownloadingDescriptorHolder.class);
        this.updateServerEndpoint = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
        this.apiEndpoint = getProperty("api.endpoint");
    }

    /** For testing purpose only. */
    @Deprecated
    protected InstallationManagerServiceImpl(InstallationManager manager,
                                             HttpTransport transport,
                                             DownloadingDescriptorHolder downloadingDescriptorHolder) {
        this.manager = manager;
        this.transport = transport;
        this.updateServerEndpoint = extractServerUrl(getProperty("installation-manager.update_server_endpoint"));
        this.apiEndpoint = getProperty("api.endpoint");
        this.downloadingDescriptorHolder = downloadingDescriptorHolder;
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
                                             .withSubscription(subscription)
                                             .withMessage("Subscription is valid")
                                             .build().toJson();
            } else {
                return new Response.Builder().withStatus(ERROR)
                                             .withSubscription(subscription)
                                             .withMessage("Subscription not found or outdated").build().toJson();
            }
        } catch (Exception e) {
            return new Response.Builder().withStatus(ERROR)
                                         .withSubscription(subscription)
                                         .withMessage(e.getMessage())
                                         .build().toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String startDownload(String downloadDescriptorId, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        return doStartDownload(null, null, downloadDescriptorId, userCredentialsRep);
    }

    /** {@inheritDoc} */
    @Override
    public String startDownload(String artifactName,
                                String downloadDescriptorId,
                                JacksonRepresentation<UserCredentials> userCredentialsRep) {
        return doStartDownload(artifactName, null, downloadDescriptorId, userCredentialsRep);
    }

    /** {@inheritDoc} */
    @Override
    public String startDownload(String artifactName,
                                String version,
                                String downloadDescriptorId,
                                JacksonRepresentation<UserCredentials> userCredentialsRep) {
        return doStartDownload(artifactName, version, downloadDescriptorId, userCredentialsRep);
    }

    private String doStartDownload(@Nullable final String artifactName,
                                   @Nullable final String version,
                                   final String downloadDescriptorId,
                                   final JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            final CountDownLatch latcher = new CountDownLatch(1);

            new Thread() {
                @Override
                public void run() {
                    download(artifactName, version, downloadDescriptorId, userCredentialsRep, latcher);
                }
            }.start();

            latcher.await();

            return new Response.Builder().withStatus(ResponseCode.OK).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    private void download(@Nullable String artifactName,
                          @Nullable String version,
                          String downloadDescriptorId,
                          JacksonRepresentation<UserCredentials> userCredentialsRep,
                          CountDownLatch latcher) {

        List<ArtifactInfo> infos = null;
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();

            Map<Artifact, String> updates = filter(artifactName, version, userCredentials);
            updates = skipDownloadedArtifacts(updates);

            infos = new ArrayList<>(updates.size());

            DownloadingDescriptor downloadingDescriptor = createDescriptor(updates, manager);
            downloadingDescriptorHolder.put(downloadDescriptorId, downloadingDescriptor);

            latcher.countDown();

            for (Map.Entry<Artifact, String> e : updates.entrySet()) {
                Artifact artToDownload = e.getKey();
                String verToDownload = e.getValue();

                try {
                    Path pathToBinaries = doDownload(userCredentials, artToDownload, verToDownload);
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, pathToBinaries.toString(), Status.SUCCESS));
                } catch (Exception exp) {
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, Status.FAILURE));
                    downloadingDescriptor.setDownloadResult(new Response.Builder().withStatus(ERROR)
                                                                                  .withMessage(exp.getMessage())
                                                                                  .withArtifacts(infos)
                                                                                  .build());
                    return;
                }
            }

            downloadingDescriptor.setDownloadResult(new Response.Builder().withStatus(ResponseCode.OK)
                                                                          .withArtifacts(infos)
                                                                          .build());
        } catch (Exception e) {
            DownloadingDescriptor descriptor = downloadingDescriptorHolder.get(downloadDescriptorId);

            if (descriptor == null) {
                descriptor = new DownloadingDescriptor(Collections.<Path, Long>emptyMap());
                descriptor.setDownloadResult(Response.valueOf(e));
                downloadingDescriptorHolder.put(downloadDescriptorId, descriptor);
            } else {
                descriptor.setDownloadResult(new Response.Builder().withStatus(ERROR)
                                                                   .withMessage(e.getMessage())
                                                                   .withArtifacts(infos)
                                                                   .build());
            }

            if (latcher.getCount() == 1) {
                latcher.countDown();
            }
        }
    }

    private Map<Artifact, String> skipDownloadedArtifacts(Map<Artifact, String> updates) throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloaded = manager.getDownloadedArtifacts();

        Map<Artifact, String> artifacts2Download = new LinkedHashMap<>();
        for (Map.Entry<Artifact, String> e : updates.entrySet()) {
            Artifact artifact = e.getKey();
            Version version = Version.valueOf(e.getValue());

            if (!downloaded.containsKey(artifact) || !downloaded.get(artifact).containsKey(version)) {
                artifacts2Download.put(artifact, version.toString());
            }
        }

        return artifacts2Download;
    }

    /** Filters what need to download, either all updates or a specific one. */
    private Map<Artifact, String> filter(@Nullable final String artifactName,
                                         @Nullable final String version,
                                         UserCredentials userCredentials) throws IOException {

        final Map<Artifact, String> updates = manager.getUpdates(userCredentials.getToken());

        if (artifactName != null) {
            final Artifact artifact = createArtifact(artifactName);

            if (updates.containsKey(artifact)) {
                if (version != null) {
                    return new HashMap<Artifact, String>() {{
                        put(artifact, version);
                    }};
                } else {
                    return new HashMap<Artifact, String>() {{
                        put(artifact, updates.get(artifact));
                    }};
                }
            } else {
                throw new ArtifactNotFoundException(artifactName);
            }
        }

        return updates;
    }

    protected Path doDownload(UserCredentials userCredentials,
                              Artifact artifact,
                              String version) throws IOException, IllegalStateException {
        return manager.download(userCredentials, artifact, version);
    }

    /** {@inheritDoc} */
    @Override
    public String downloadStatus(final String downloadDescriptorId) {
        try {
            DownloadingDescriptor descriptor = downloadingDescriptorHolder.get(downloadDescriptorId);

            if (descriptor == null) {
                return new Response.Builder().withStatus(ERROR)
                                             .withMessage("Can't get downloading descriptor ID").build().toJson();
            }

            Response downloadResult = descriptor.getDownloadResult();
            if ((downloadResult != null) && (downloadResult.getStatus() == ResponseCode.ERROR)) {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.FAILURE, 0, downloadResult);
                return new Response.Builder().withStatus(ResponseCode.ERROR).withDownloadInfo(info).build().toJson();
            }

            long downloadedSize = descriptor.getDownloadedSize();
            int percents = (int)Math.round((downloadedSize * 100D / descriptor.getTotalSize()));

            if (descriptor.isDownloadingFinished()) {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.DOWNLOADED, percents, downloadResult);
                return new Response.Builder().withStatus(ResponseCode.OK).withDownloadInfo(info).build().toJson();
            } else {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.DOWNLOADING, percents);
                return new Response.Builder().withStatus(ResponseCode.OK).withDownloadInfo(info).build().toJson();
            }
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloads(String artifactName, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();

            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            List<ArtifactInfo> infos = new ArrayList<>();
            SortedMap<Version, Path> versions = downloadedArtifacts.get(ArtifactFactory.createArtifact(artifactName));

            if (versions != null && !versions.isEmpty()) {
                for (Map.Entry<Version, Path> e : versions.entrySet()) {
                    Version version = e.getKey();
                    Path pathToBinaries = e.getValue();
                    Artifact artifact = ArtifactFactory.createArtifact(artifactName);
                    Status status = artifact.isInstallable(version, token) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

                    infos.add(new ArtifactInfo(artifactName, version.toString(), pathToBinaries.toString(), status));
                }
            }

            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloads(String artifactName,
                               String version, JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();

            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            Version v = Version.valueOf(version);

            List<ArtifactInfo> infos = new ArrayList<>();
            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            if (downloadedArtifacts.get(artifact) != null && downloadedArtifacts.get(artifact).containsKey(v)) {
                Path pathToBinaries = downloadedArtifacts.get(artifact).get(v);
                Status status = artifact.isInstallable(v, token) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

                infos.add(new ArtifactInfo(artifactName, version, pathToBinaries.toString(), status));

                return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
            } else {
                return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
            }
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc}
     * @param userCredentialsRep*/
    @Override
    public String getDownloads(JacksonRepresentation<UserCredentials> userCredentialsRep) {
        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            String token = userCredentials.getToken();

            Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

            List<ArtifactInfo> infos = new ArrayList<>();
            for (Map.Entry<Artifact, SortedMap<Version, Path>> artifact : downloadedArtifacts.entrySet()) {
                for (Map.Entry<Version, Path> e : artifact.getValue().entrySet()) {
                    Version version = e.getKey();
                    Path pathToBinaries = e.getValue();
                    Status status = artifact.getKey().isInstallable(version, token) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

                    infos.add(new ArtifactInfo(artifact.getKey(), version.toString(), pathToBinaries.toString(), status));
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

                    infos.add(new ArtifactInfo(artifact, version, Status.DOWNLOADED));
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
                infos.add(new ArtifactInfo(artifact, version, Status.SUCCESS));
            } catch (Exception e) {
                infos.add(new ArtifactInfo(artifact, version, Status.FAILURE));
                return new Response.Builder().withStatus(ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
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
        String token;
        Artifact artifact;
        String toInstallVersion;

        try {
            UserCredentials userCredentials = userCredentialsRep.getObject();
            token = userCredentials.getToken();

            artifact = createArtifact(artifactName);
            toInstallVersion = version != null ? version : manager.getUpdates(token).get(artifact);
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }

        if (toInstallVersion == null) {
            return Response.valueOf(new IllegalStateException("Artifact '" + artifactName + "' isn't available to update.")).toJson();
        }

        try {
            doInstall(artifact, toInstallVersion, token);
            ArtifactInfo info = new ArtifactInfo(artifactName, toInstallVersion, Status.SUCCESS);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();

        } catch (InstallStartedException e) {
            ArtifactInfo info = new ArtifactInfo(artifactName, toInstallVersion, Status.INSTALL_STARTED);
            info.setInstallCommandsInfo(e.getCommandsInfo());
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();

        } catch (InstallInProgressException e) {
            ArtifactInfo info = new ArtifactInfo(artifactName, toInstallVersion, Status.INSTALLING);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();

        } catch (Exception e) {
            ArtifactInfo info = new ArtifactInfo(artifactName, toInstallVersion, Status.FAILURE);
            return new Response.Builder().withStatus(ERROR).withMessage(e.getMessage()).withArtifact(info).build().toJson();
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
        return new Response.Builder().withStatus(ResponseCode.OK).withConfig(config).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String setConfig(JacksonRepresentation<InstallationManagerConfig> configRep) {
        try {
            manager.setConfig(configRep.getObject());
            return new Response.Builder().withStatus(ResponseCode.OK).build().toJson();
        } catch (Exception e) {
            return Response.valueOf(e).toJson();
        }
    }

    protected void doInstall(Artifact artifact, String version, String token) throws IOException, IllegalStateException {
        manager.install(token, artifact, version);
    }
}
