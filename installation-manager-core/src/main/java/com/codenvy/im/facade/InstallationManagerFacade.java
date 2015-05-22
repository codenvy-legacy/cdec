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
package com.codenvy.im.facade;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.BackupInfo;
import com.codenvy.im.response.DownloadStatusInfo;
import com.codenvy.im.response.NodeInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ApiException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.response.ResponseCode.ERROR;
import static com.codenvy.im.saas.SaasAccountServiceProxy.ON_PREMISES;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallationManagerFacade {
    private static final Logger LOG = Logger.getLogger(InstallationManagerFacade.class.getSimpleName());

    protected final HttpTransport           transport;
    protected final SaasAuthServiceProxy    saasAuthServiceProxy;
    protected final SaasAccountServiceProxy saasAccountServiceProxy;
    protected final PasswordManager passwordManager;
    protected final NodeManager     nodeManager;
    protected final BackupManager   backupManager;
    protected final StorageManager  storageManager;
    protected final InstallManager  installManager;
    protected final DownloadManager downloadManager;

    private final String updateServerEndpoint;
    private final Path downloadDir;

    private DownloadDescriptor downloadDescriptor;

    @Inject
    public InstallationManagerFacade(@Named("installation-manager.download_dir") String downloadDir,
                                     @Named("installation-manager.update_server_endpoint") String updateServerEndpoint,
                                     HttpTransport transport,
                                     SaasAuthServiceProxy saasAuthServiceProxy,
                                     SaasAccountServiceProxy saasAccountServiceProxy,
                                     PasswordManager passwordManager,
                                     NodeManager nodeManager,
                                     BackupManager backupManager,
                                     StorageManager storageManager,
                                     InstallManager installManager,
                                     DownloadManager downloadManager) {
        this.installManager = installManager;
        this.downloadManager = downloadManager;
        this.downloadDir = Paths.get(downloadDir);
        this.transport = transport;
        this.updateServerEndpoint = updateServerEndpoint;
        this.saasAuthServiceProxy = saasAuthServiceProxy;
        this.saasAccountServiceProxy = saasAccountServiceProxy;
        this.passwordManager = passwordManager;
        this.nodeManager = nodeManager;
        this.backupManager = backupManager;
        this.storageManager = storageManager;
    }

    public String getUpdateServerEndpoint() {
        return updateServerEndpoint;
    }

    /** Adds trial subscription for user being logged in */
    public String addTrialSaasSubscription(@Nonnull Request request) throws IOException {
        try {
            transport
                    .doPost(combinePaths(updateServerEndpoint, "/repository/subscription/" + request.obtainAccountId()), null,
                            request.obtainAccessToken());
            return new Response().setStatus(ResponseCode.OK)
                                 .setSubscription(ON_PREMISES)
                                 .setMessage("Subscription has been added")
                                 .toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).toJson();
        }
    }

    /** Check user's subscription. */
    public String checkSubscription(String subscription,
                                    @Nonnull Request request) throws IOException {
        try {
            boolean subscriptionValidated = saasAccountServiceProxy.hasValidSubscription(subscription,
                                                                                         request.obtainAccessToken(),
                                                                                         request.obtainAccountId());

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
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return new Response().setStatus(ERROR)
                                 .setSubscription(subscription)
                                 .setMessage(e.getMessage())
                                 .toJson();
        }
    }


    /** Starts downloading */
    public String startDownload(@Nonnull final Request request) {
        try {
            downloadManager.checkIfConnectionIsAvailable();

            final CountDownLatch latcher = new CountDownLatch(1);

            Thread downloadThread = new Thread("download thread") {
                public void run() {
                    download(request, latcher, this);
                }
            };
            downloadThread.setDaemon(true);
            downloadThread.start();

            latcher.await();

            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).toJson();
        }
    }

    private void download(Request request,
                          CountDownLatch latcher,
                          Thread currentThread) {

        downloadDescriptor = null;
        List<ArtifactInfo> infos = null;
        try {
            Map<Artifact, Version> updatesToDownload = downloadManager.getUpdatesToDownload(request.createArtifact(), request.createVersion());

            infos = new ArrayList<>(updatesToDownload.size());

            downloadDescriptor = DownloadDescriptor.createDescriptor(updatesToDownload, downloadManager, currentThread);
            downloadManager.checkEnoughDiskSpace(downloadDescriptor.getTotalSize());

            latcher.countDown();

            for (Map.Entry<Artifact, Version> e : updatesToDownload.entrySet()) {
                Artifact artToDownload = e.getKey();
                Version verToDownload = e.getValue();

                try {
                    Path pathToBinaries = doDownload(artToDownload, verToDownload);
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, pathToBinaries, Status.SUCCESS));
                } catch (Exception exp) {
                    LOG.log(Level.SEVERE, exp.getMessage(), exp);
                    infos.add(new ArtifactInfo(artToDownload, verToDownload, Status.FAILURE));
                    downloadDescriptor.setDownloadResult(new Response().setStatus(ERROR)
                                                                       .setMessage(exp.getMessage())
                                                                       .setArtifacts(infos));
                    return;
                }
            }

            downloadDescriptor.setDownloadResult(new Response().setStatus(ResponseCode.OK).setArtifacts(infos));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);

            if (downloadDescriptor == null) {
                downloadDescriptor = new DownloadDescriptor(Collections.<Path, Long>emptyMap(), currentThread);
                downloadDescriptor.setDownloadResult(Response.error(e));
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

    protected Path doDownload(Artifact artifact, Version version) throws IOException, IllegalStateException {
        return downloadManager.download(artifact, version);
    }

    /** @return the current status of downloading process */
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
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).toJson();
        }
    }

    // @return the size of downloaded artifacts
    private long getDownloadedSize(DownloadDescriptor descriptor) throws IOException {
        long downloadedSize = 0;
        for (Path path : descriptor.getArtifactPaths()) {
            if (exists(path)) {
                downloadedSize += size(path);
            }
        }
        return downloadedSize;
    }

    /** Interrupts downloading */
    public String stopDownload() {
        try {
            if (downloadDescriptor == null) {
                return new Response().setStatus(ERROR).setMessage("Can't get downloading descriptor ID").toJson();
            }

            downloadDescriptor.getDownloadThread().interrupt();
            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).toJson();
        }
    }

    /** @return the list of downloaded artifacts */
    public String getDownloads(@Nonnull Request request) {
        try {
            try {
                List<ArtifactInfo> infos = new ArrayList<>();

                if (request.createArtifact() == null) {
                    Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = downloadManager.getDownloadedArtifacts();

                    for (Map.Entry<Artifact, SortedMap<Version, Path>> e : downloadedArtifacts.entrySet()) {
                        infos.addAll(getDownloadedArtifactsInfo(e.getKey(), e.getValue()));
                    }
                } else {
                    SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(request.createArtifact());

                    if ((downloadedVersions != null) && !downloadedVersions.isEmpty()) {
                        Version version = request.createVersion();
                        if ((version != null) && downloadedVersions.containsKey(version)) {
                            final Path path = downloadedVersions.get(version);
                            downloadedVersions = ImmutableSortedMap.of(version, path);
                        }

                        infos = getDownloadedArtifactsInfo(request.createArtifact(), downloadedVersions);
                    }
                }

                return new Response().setStatus(ResponseCode.OK).setArtifacts(infos).toJson();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                return Response.error(e).toJson();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    private List<ArtifactInfo> getDownloadedArtifactsInfo(Artifact artifact, SortedMap<Version, Path> downloadedVersions) throws IOException {
        List<ArtifactInfo> info = new ArrayList<>();

        for (Map.Entry<Version, Path> e : downloadedVersions.entrySet()) {
            Version version = e.getKey();
            Path pathToBinaries = e.getValue();
            Status status = installManager.isInstallable(artifact, version) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

            info.add(new ArtifactInfo(artifact, version, pathToBinaries, status));
        }

        return info;
    }

    /** @return update list from the server */
    public String getUpdates() {
        try {
            Map<Artifact, Version> updates = downloadManager.getUpdates();
            List<ArtifactInfo> infos = new ArrayList<>(updates.size());
            for (Map.Entry<Artifact, Version> e : updates.entrySet()) {
                Artifact artifact = e.getKey();
                Version version = e.getValue();

                if (downloadManager.getDownloadedVersions(artifact).containsKey(version)) {
                    infos.add(new ArtifactInfo(artifact, version, Status.DOWNLOADED));
                } else {
                    infos.add(new ArtifactInfo(artifact, version));
                }
            }

            return new Response().setStatus(ResponseCode.OK).setArtifacts(infos).toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).toJson();
        }
    }

    /** @return the list of installed artifacts and theirs versions */
    public Response getInstalledVersions() throws IOException {
        Map<Artifact, Version> installedArtifacts = installManager.getInstalledArtifacts();
        return new Response().setStatus(ResponseCode.OK).addArtifacts(installedArtifacts);
    }

    /** @return installation info */
    public String getInstallInfo(@Nonnull Request request) throws IOException {
        try {
            InstallOptions installOptions = request.getInstallOptions();
            Version version = doGetVersionToInstall(request);

            List<String> infos = installManager.getInstallInfo(request.createArtifact(), version, installOptions);
            return new Response().setStatus(ResponseCode.OK).setInfos(infos).toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    /** Installs artifact */
    public String install(@Nonnull Request request) throws IOException {
        try {
            InstallOptions installOptions = request.getInstallOptions();
            Version version = doGetVersionToInstall(request);

            try {
                install(request.createArtifact(), version, installOptions);
                ArtifactInfo info = new ArtifactInfo(request.createArtifact(), version, Status.SUCCESS);
                return new Response().setStatus(ResponseCode.OK).addArtifact(info).toJson();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                ArtifactInfo info = new ArtifactInfo(request.createArtifact(), version, Status.FAILURE);
                return new Response().setStatus(ERROR).setMessage(e.getMessage()).addArtifact(info).toJson();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    protected void install(Artifact artifact, Version version, InstallOptions options) throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = downloadManager.getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact) && downloadedArtifacts.get(artifact).containsKey(version)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact).get(version);
            if (pathToBinaries == null) {
                throw new IOException(String.format("Binaries for artifact '%s' version '%s' not found", artifact, version));
            }

            if (options.getStep() != 0 || artifact.isInstallable(version)) {
                installManager.install(artifact, version, pathToBinaries, options);
            } else {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version + "'.");
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact '" + artifact.getName() + "' version '" + version + "' not found");
        }
    }

    /** @return the version of the artifact that can be installed */
    public String getVersionToInstall(@Nonnull Request request) throws IOException {
        try {
            return doGetVersionToInstall(request).toString();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return new Response().setStatus(ERROR).setMessage(e.getMessage()).toJson();
        }
    }

    protected Version doGetVersionToInstall(@Nonnull Request request) throws IOException {
        int installStep = 0;
        if (request.getInstallOptions() != null) {
            installStep = request.getInstallOptions().getStep();
        }

        if (request.createVersion() != null) {
            return request.createVersion();

        } else if (installStep == 0) {
            Version version = installManager.getLatestInstallableVersion(request.createArtifact());

            if (version == null) {
                throw new IllegalStateException(format("There is no newer version to install '%s'.", request.createArtifact()));
            }

            return version;

        } else {
            SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(request.createArtifact());
            if (downloadedVersions.isEmpty()) {
                throw new IllegalStateException(format("Installation in progress but binaries for '%s' not found.", request.createArtifact()));
            }
            return downloadedVersions.keySet().iterator().next();
        }
    }

    /** @return account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @Nullable
    public AccountReference getAccountWhereUserIsOwner(@Nullable String accountName, @Nonnull Request request) throws IOException {
        return saasAccountServiceProxy.getAccountWhereUserIsOwner(request.obtainAccessToken(), accountName);
    }

    /**
     * @return configuration of the Installation Manager
     */
    public Map<String, String> getInstallationManagerProperties() {
        return new LinkedHashMap<String, String>() {{
            put("download directory", downloadDir.toString());
            put("base url", extractServerUrl(updateServerEndpoint));
        }};
    }

    protected String extractServerUrl(String url) {
        return Commons.extractServerUrl(url);
    }

    /**
     * @see com.codenvy.im.managers.NodeManager#add(String)
     */
    public Response addNode(@Nonnull String dns) {
        try {
            NodeConfig node = nodeManager.add(dns);
            return Response.ok().setNode(NodeInfo.createSuccessInfo(node));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e);
        }
    }

    /**
     * @see com.codenvy.im.managers.NodeManager#remove(String)
     */
    public Response removeNode(@Nonnull String dns) {
        try {
            NodeConfig node = nodeManager.remove(dns);
            return Response.ok().setNode(NodeInfo.createSuccessInfo(node));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e);
        }
    }

    /**
     * @see com.codenvy.im.managers.BackupManager#backup(com.codenvy.im.managers.BackupConfig)
     */
    public Response backup(@Nonnull BackupConfig config) {
        try {
            BackupConfig backupConfig = backupManager.backup(config);
            return Response.ok().setBackup(BackupInfo.createSuccessInfo(backupConfig));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).setBackup(BackupInfo.createFailureInfo(config));
        }
    }

    /**
     * @see com.codenvy.im.managers.BackupManager#restore(com.codenvy.im.managers.BackupConfig)
     */
    public Response restore(@Nonnull BackupConfig config) {
        try {
            backupManager.restore(config);
            return Response.ok().setBackup(BackupInfo.createSuccessInfo(config));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.error(e).setBackup(BackupInfo.createFailureInfo(config));
        }
    }

    /**
     * @see com.codenvy.im.saas.SaasAuthServiceProxy#login(org.eclipse.che.api.auth.shared.dto.Credentials)
     */
    public Token loginToCodenvySaaS(@Nonnull Credentials credentials) throws ApiException {
        return saasAuthServiceProxy.login(credentials);
    }

    /**
     * @see com.codenvy.im.saas.SaasAccountServiceProxy#getSubscription(String, String, String)
     */
    @Nullable
    public SubscriptionDescriptor getSubscriptionDescriptor(String subscriptionName, @Nonnull Request request) throws IOException {
        return saasAccountServiceProxy.getSubscription(subscriptionName,
                                                       request.obtainAccessToken(),
                                                       request.obtainAccountId());
    }

    /**
     * @see com.codenvy.im.managers.PasswordManager#changeAdminPassword(byte[], byte[])
     */
    public void changeAdminPassword(byte[] currentPassword, byte[] newPassword) throws IOException {
        passwordManager.changeAdminPassword(currentPassword, newPassword);
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#storeProperties(java.util.Map)
     */
    public void storeProperties(@Nullable Map<String, String> newProperties) throws IOException {
        storageManager.storeProperties(newProperties);
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#loadProperties(java.util.Collection)
     */
    public Map<String, String> loadProperties(@Nullable Collection<String> names) throws IOException {
        return storageManager.loadProperties(names);
    }

    /** Update artifact config property */
    public void updateArtifactConfig(String artifactName, String property, String value) throws IOException {
        Artifact artifact = ArtifactFactory.createArtifact(artifactName);
        doUpdateArtifactConfig(artifact, property, value);
    }

    protected void doUpdateArtifactConfig(Artifact artifact, String property, String value) throws IOException {
        artifact.updateConfig(property, value);
    }
}
