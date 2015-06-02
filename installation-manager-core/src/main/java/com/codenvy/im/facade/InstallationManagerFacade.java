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
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.ArtifactStatus;
import com.codenvy.im.response.BackupInfo;
import com.codenvy.im.response.DownloadProgressDescriptor;
import com.codenvy.im.response.InstallArtifactResult;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.NodeInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.UpdatesArtifactResult;
import com.codenvy.im.response.UpdatesArtifactStatus;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.response.ResponseCode.ERROR;
import static com.codenvy.im.saas.SaasAccountServiceProxy.ON_PREMISES;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;

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

    /**
     * @see com.codenvy.im.managers.DownloadManager#startDownload(com.codenvy.im.artifacts.Artifact, com.codenvy.im.utils.Version)
     */
    public void startDownload(@Nullable Artifact artifact, @Nullable Version version)
            throws InterruptedException, DownloadAlreadyStartedException, IOException {
        downloadManager.startDownload(artifact, version);
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#stopDownload()
     */
    public void stopDownload() throws DownloadNotStartedException, InterruptedException {
        downloadManager.stopDownload();
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#getDownloadProgress()
     */
    public DownloadProgressDescriptor getDownloadProgress() throws DownloadNotStartedException, IOException {
        return downloadManager.getDownloadProgress();
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
            ArtifactStatus status = installManager.isInstallable(artifact, version) ? ArtifactStatus.READY_TO_INSTALL : ArtifactStatus.DOWNLOADED;

            info.add(new ArtifactInfo(artifact, version, pathToBinaries, status));
        }

        return info;
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#getUpdates()
     */
    public List<UpdatesArtifactResult> getUpdates() throws IOException {
        Map<Artifact, Version> updates = downloadManager.getUpdates();

        return FluentIterable.from(updates.entrySet()).transform(new Function<Map.Entry<Artifact, Version>, UpdatesArtifactResult>() {
            @Override
            public UpdatesArtifactResult apply(Map.Entry<Artifact, Version> entry) {
                Artifact artifact = entry.getKey();
                Version version = entry.getValue();

                UpdatesArtifactResult uaResult = new UpdatesArtifactResult();
                uaResult.setArtifact(artifact.getName());
                uaResult.setVersion(version.toString());
                try {
                    if (downloadManager.getDownloadedVersions(artifact).containsKey(version)) {
                        uaResult.setStatus(UpdatesArtifactStatus.DOWNLOADED);
                    }
                } catch (IOException e) {
                    // simply ignore
                }

                return uaResult;
            }
        }).toList();
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getInstalledArtifacts()
     */
    public List<InstallArtifactResult> getInstalledVersions() throws IOException {
        Map<Artifact, Version> installedArtifacts = installManager.getInstalledArtifacts();

        return FluentIterable.from(installedArtifacts.entrySet()).transform(new Function<Map.Entry<Artifact, Version>, InstallArtifactResult>() {
            @Override
            public InstallArtifactResult apply(Map.Entry<Artifact, Version> entry) {
                InstallArtifactResult iaResult = new InstallArtifactResult();
                iaResult.setArtifact(entry.getKey().getName());
                iaResult.setVersion(entry.getValue().toString());
                iaResult.setStatus(InstallArtifactStatus.SUCCESS);
                return iaResult;
            }
        }).toList();
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getInstallInfo
     */
    public List<String> getInstallInfo(Artifact artifact, InstallType installType) throws IOException {
        return installManager.getInstallInfo(artifact, installType);
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getUpdateInfo
     */
    public List<String> getUpdateInfo(Artifact artifact, InstallType installType) throws IOException {
        return installManager.getUpdateInfo(artifact, installType);
    }

    /**
     * @throws java.io.FileNotFoundException
     *         if binaries to install given artifact not found
     * @see com.codenvy.im.managers.InstallManager#performInstallStep
     */
    public void install(@Nonnull Artifact artifact,
                        @Nonnull Version version,
                        @Nonnull InstallOptions installOptions) throws IOException {
        SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(artifact);
        if (!downloadedVersions.containsKey(version)) {
            throw new FileNotFoundException(format("Binaries to install %s:%s not found", artifact.getName(), version.toString()));
        }

        installManager.performInstallStep(artifact, version, downloadedVersions.get(version), installOptions);
    }

    /**
     * @throws java.io.FileNotFoundException
     *         if binaries to install given artifact not found
     * @see com.codenvy.im.managers.InstallManager#performUpdateStep
     */
    public void update(@Nonnull Artifact artifact,
                       @Nonnull Version version,
                       @Nonnull InstallOptions installOptions) throws IOException {
        SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(artifact);
        if (!downloadedVersions.containsKey(version)) {
            throw new FileNotFoundException(format("Binaries to install %s:%s not found", artifact.getName(), version.toString()));
        }

        installManager.performUpdateStep(artifact, version, downloadedVersions.get(version), installOptions);
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getLatestInstallableVersion
     */
    @Nullable
    public Version getLatestInstallableVersion(Artifact artifact) throws IOException {
        return installManager.getLatestInstallableVersion(artifact);
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
    public Token loginToCodenvySaaS(@Nonnull Credentials credentials) throws IOException, AuthenticationException {
        return saasAuthServiceProxy.login(credentials);
    }

    /**
     * @see com.codenvy.im.saas.SaasAuthServiceProxy#logout(String)
     */
    public void logoutFromCodenvySaaS(@Nonnull String authToken) throws IOException {
        saasAuthServiceProxy.logout(authToken);
    }

    /**
     * @see com.codenvy.im.saas.SaasAccountServiceProxy#getSubscription(String, String, String)
     */
    @Nullable
    public SubscriptionDescriptor getSubscription(String subscriptionName, @Nonnull Request request) throws IOException {
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
     * @see com.codenvy.im.managers.StorageManager#loadProperties()
     */
    public Map<String, String> loadProperties() throws IOException {
        return storageManager.loadProperties();
    }

    @Nullable
    public String loadProperty(@Nullable String key) throws IOException {
        return storageManager.loadProperty(key);
    }

    public void storeProperty(@Nullable String key, @Nullable String value) throws IOException {
        storageManager.storeProperty(key, value);
    }

    public void deleteProperty(@Nullable String name) throws IOException {
        storageManager.deleteProperty(name);
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
