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
package com.codenvy.im.service;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import com.codenvy.im.utils.AccountUtils;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.InjectorBootstrap;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;

import static com.codenvy.im.response.ResponseCode.ERROR;
import static com.codenvy.im.service.DownloadDescriptor.createDescriptor;
import static com.codenvy.im.utils.AccountUtils.isValidSubscription;
import static com.codenvy.im.utils.Commons.toJson;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallationManagerServiceImpl implements InstallationManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerServiceImpl.class);

    protected final InstallationManager manager;
    protected final HttpTransport       transport;

    private final String updateServerEndpoint;
    private final String apiEndpoint;

    private DownloadDescriptor downloadDescriptor;

    static {
        InjectorBootstrap.INJECTOR.injectMembers(InstallationManagerService.class);
        InjectorBootstrap.INJECTOR.injectMembers(InstallationManagerServiceImpl.class);
    }

    @Inject
    public InstallationManagerServiceImpl(@Named("installation-manager.update_server_endpoint") String updateServerEndpoint,
                                          @Named("api.endpoint") String apiEndpoint,
                                          InstallationManager manager,
                                          HttpTransport transport) {
        this.manager = manager;
        this.transport = transport;
        this.updateServerEndpoint = updateServerEndpoint;
        this.apiEndpoint = apiEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdateServerEndpoint() {
        return updateServerEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String checkSubscription(String subscription, Request request) throws IOException {
        try {
            boolean subscriptionValidated = isValidSubscription(transport,
                                                                apiEndpoint,
                                                                subscription,
                                                                request.getAccessToken(),
                                                                request.getAccountId());

            if (subscriptionValidated) {
                return new Response().setStatus(ResponseCode.OK)
                                     .setSubscription(subscription)
                                     .setMessage("Subscription is valid")
                                     .toJson();
            } else {
                return new Response().setStatus(ERROR)
                                     .setSubscription(subscription)
                                     .setMessage("Subscription not found or outdated")
                                     .toJson();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new Response().setStatus(ERROR)
                                 .setSubscription(subscription)
                                 .setMessage(e.getMessage())
                                 .toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String startDownload(final Request request) {
        try {
            manager.checkIfConnectionIsAvailable();

            final CountDownLatch latcher = new CountDownLatch(1);

            new Thread() {
                @Override
                public void run() {
                    download(request, latcher, this);
                }
            }.start();

            latcher.await();

            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    private void download(Request request,
                          CountDownLatch latcher,
                          Thread currentThread) {

        downloadDescriptor = null;
        List<ArtifactInfo> infos = null;
        try {
            Map<Artifact, Version> updatesToDownload = manager.getUpdatesToDownload(request.getArtifact(),
                                                                                    request.getVersion(),
                                                                                    request.getAccessToken());

            infos = new ArrayList<>(updatesToDownload.size());

            downloadDescriptor = createDescriptor(updatesToDownload, manager, currentThread);
            manager.checkEnoughDiskSpace(downloadDescriptor.getTotalSize());

            latcher.countDown();

            for (Map.Entry<Artifact, Version> e : updatesToDownload.entrySet()) {
                Artifact artToDownload = e.getKey();
                Version verToDownload = e.getValue();

                try {
                    Path pathToBinaries = doDownload(request.getUserCredentials(), artToDownload, verToDownload);
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, pathToBinaries, Status.SUCCESS));
                } catch (Exception exp) {
                    LOG.error(exp.getMessage(), exp);
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, Status.FAILURE));
                    downloadDescriptor.setDownloadResult(new Response().setStatus(ERROR)
                                                                       .setMessage(exp.getMessage())
                                                                       .setArtifacts(infos));
                    return;
                }
            }

            downloadDescriptor.setDownloadResult(new Response().setStatus(ResponseCode.OK).setArtifacts(infos));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);

            if (downloadDescriptor == null) {
                downloadDescriptor = new DownloadDescriptor(Collections.<Path, Long>emptyMap(), currentThread);
                downloadDescriptor.setDownloadResult(Response.valueOf(e));
            } else {
                downloadDescriptor.setDownloadResult(new Response().setStatus(ERROR)
                                                                   .setMessage(e.getMessage())
                                                                   .setArtifacts(infos));
            }

            if (latcher.getCount() == 1) {
                latcher.countDown();
            }
        }
    }

    protected Path doDownload(UserCredentials userCredentials,
                              Artifact artifact,
                              Version version) throws IOException, IllegalStateException {
        return manager.download(userCredentials, artifact, version);
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloadStatus() {
        try {
            if (downloadDescriptor == null) {
                return new Response().setStatus(ERROR).setMessage("Can't get downloading descriptor ID").toJson();
            }

            Response downloadResult = downloadDescriptor.getDownloadResult();
            if ((downloadResult != null) && (downloadResult.getStatus() == ResponseCode.ERROR)) {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.FAILURE, 0, downloadResult);
                return new Response().setStatus(ResponseCode.ERROR).setDownloadInfo(info).toJson();
            }

            long downloadedSize = getDownloadedSize(downloadDescriptor);
            int percents = (int)Math.round((downloadedSize * 100D / downloadDescriptor.getTotalSize()));

            if (downloadDescriptor.isDownloadingFinished()) {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.DOWNLOADED, percents, downloadResult);
                return new Response().setStatus(ResponseCode.OK).setDownloadInfo(info).toJson();
            } else {
                DownloadStatusInfo info = new DownloadStatusInfo(Status.DOWNLOADING, percents);
                return new Response().setStatus(ResponseCode.OK).setDownloadInfo(info).toJson();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** @return the size of downloaded artifacts */
    private long getDownloadedSize(DownloadDescriptor descriptor) throws IOException {
        long downloadedSize = 0;
        for (Path path : descriptor.getArtifactPaths()) {
            if (exists(path)) {
                downloadedSize += size(path);
            }
        }
        return downloadedSize;
    }


    /** {@inheritDoc} */
    @Override
    public String stopDownload() {
        try {
            if (downloadDescriptor == null) {
                return new Response().setStatus(ERROR).setMessage("Can't get downloading descriptor ID").toJson();
            }

            downloadDescriptor.getDownloadThread().interrupt();
            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDownloads(Request request) {
        try {
            try {
                List<ArtifactInfo> infos = new ArrayList<>();

                if (request.getArtifact() == null) {
                    Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

                    for (Map.Entry<Artifact, SortedMap<Version, Path>> e : downloadedArtifacts.entrySet()) {
                        infos.addAll(getDownloadedArtifactsInfo(request.getAccessToken(), e.getKey(), e.getValue()));
                    }
                } else {
                    SortedMap<Version, Path> downloadedVersions = manager.getDownloadedVersions(request.getArtifact());

                    if ((downloadedVersions != null) && !downloadedVersions.isEmpty()) {
                        Version version = request.getVersion();
                        if ((version != null) && downloadedVersions.containsKey(version)) {
                            final Path path = downloadedVersions.get(version);
                            downloadedVersions = ImmutableSortedMap.of(version, path);
                        }

                        infos = getDownloadedArtifactsInfo(request.getAccessToken(), request.getArtifact(), downloadedVersions);
                    }
                }

                return new Response().setStatus(ResponseCode.OK).setArtifacts(infos).toJson();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return Response.valueOf(e).toJson();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    private List<ArtifactInfo> getDownloadedArtifactsInfo(String token,
                                                          Artifact artifact,
                                                          SortedMap<Version, Path> downloadedVersions) throws IOException {
        List<ArtifactInfo> info = new ArrayList<>();

        for (Map.Entry<Version, Path> e : downloadedVersions.entrySet()) {
            Version version = e.getKey();
            Path pathToBinaries = e.getValue();
            Status status = manager.isInstallable(artifact, version, token) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

            info.add(new ArtifactInfo(artifact, version, pathToBinaries, status));
        }

        return info;
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdates(Request request) {
        try {
            Map<Artifact, Version> updates = manager.getUpdates(request.getAccessToken());
            List<ArtifactInfo> infos = new ArrayList<>(updates.size());
            for (Map.Entry<Artifact, Version> e : updates.entrySet()) {
                Artifact artifact = e.getKey();
                Version version = e.getValue();

                if (manager.getDownloadedVersions(artifact).containsKey(version)) {
                    infos.add(new ArtifactInfo(artifact, version, Status.DOWNLOADED));
                } else {
                    infos.add(new ArtifactInfo(artifact, version));
                }
            }

            return new Response().setStatus(ResponseCode.OK).setArtifacts(infos).toJson();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getInstalledVersions(Request request) throws IOException {
        Map<Artifact, Version> installedArtifacts = manager.getInstalledArtifacts(request.getAccessToken());
        return new Response().setStatus(ResponseCode.OK).addArtifacts(installedArtifacts).toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String getInstallInfo(InstallOptions installOptions, Request request) throws IOException {
        try {
            List<String> infos = manager.getInstallInfo(request.getArtifact(), null, installOptions);
            return new Response().setStatus(ResponseCode.OK).setInfos(infos).toJson();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String install(InstallOptions installOptions, Request request) throws IOException {
        try {
            Version version = getVersionToInstall(request, installOptions.getStep());

            try {
                manager.install(request.getAccessToken(), request.getArtifact(), version, installOptions);
                ArtifactInfo info = new ArtifactInfo(request.getArtifact(), version, Status.SUCCESS);
                return new Response().setStatus(ResponseCode.OK).addArtifact(info).toJson();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                ArtifactInfo info = new ArtifactInfo(request.getArtifact(), version, Status.FAILURE);
                return new Response().setStatus(ERROR).setMessage(e.getMessage()).addArtifact(info).toJson();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    protected Version getVersionToInstall(Request request, int installStep) throws IOException {
        if (request.getVersion() != null) {
            return request.getVersion();

        } else if (installStep == 0) {
            Version version = manager.getLatestInstallableVersion(request.getUserCredentials().getToken(), request.getArtifact());

            if (version == null) {
                throw new IllegalStateException(format("There is no newer version to install '%s'.", request.getArtifact()));
            }

            return version;

        } else {
            SortedMap<Version, Path> downloadedVersions = manager.getDownloadedVersions(request.getArtifact());
            if (downloadedVersions.isEmpty()) {
                throw new IllegalStateException(format("Installation in progress but binaries for '%s' not found.", request.getArtifact()));
            }
            return downloadedVersions.keySet().iterator().next();
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getAccountReferenceWhereUserIsOwner(@Nullable String accountName,
                                                      Request request) throws IOException {

        AccountReference accountReference = AccountUtils.getAccountReferenceWhereUserIsOwner(transport,
                                                                                             apiEndpoint,
                                                                                             request.getAccessToken(),
                                                                                             accountName);
        return accountReference == null ? null : toJson(accountReference);
    }

    /** {@inheritDoc} */
    @Override
    public String getConfig() {
        return new Response().setStatus(ResponseCode.OK).setConfig(manager.getConfig()).toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String setConfig(InstallationManagerConfig config) {
        try {
            manager.setConfig(config);
            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }
}
