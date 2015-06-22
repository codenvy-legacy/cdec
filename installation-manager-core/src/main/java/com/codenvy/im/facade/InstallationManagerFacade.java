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

import com.codenvy.api.subscription.shared.dto.SubscriptionDescriptor;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.InstallationNotStartedException;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.ArtifactStatus;
import com.codenvy.im.response.BackupInfo;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.response.DownloadProgressResponse;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.InstallArtifactStepInfo;
import com.codenvy.im.response.NodeInfo;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactStatus;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallationManagerFacade {
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
    public void startDownload(@Nullable Artifact artifact, @Nullable Version version) throws InterruptedException,
                                                                                             DownloadAlreadyStartedException,
                                                                                             IOException {
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
    public DownloadProgressResponse getDownloadProgress() throws DownloadNotStartedException, IOException {
        return downloadManager.getDownloadProgress();
    }

    /**
     * Adds trial subscription for user being logged into Codenvy SaaS.
     */
    public void addTrialSaasSubscription(@Nonnull SaasUserCredentials saasUserCredentials) throws IOException {
        String requestUrl = combinePaths(updateServerEndpoint, "/repository/subscription", saasUserCredentials.getAccountId());
        transport.doPost(requestUrl, null, saasUserCredentials.getToken());
    }

    /**
     * @see com.codenvy.im.saas.SaasAccountServiceProxy#hasValidSubscription(String, String, String)
     */
    public boolean hasValidSaasSubscription(@Nonnull String subscription,
                                            @Nonnull SaasUserCredentials saasUserCredentials) throws IOException {
        return saasAccountServiceProxy.hasValidSubscription(subscription,
                                                            saasUserCredentials.getToken(),
                                                            saasUserCredentials.getAccountId());
    }


    /**
     * @see com.codenvy.im.managers.DownloadManager#getDownloadIdInProgress()
     */
    public String getDownloadIdInProgress() throws DownloadNotStartedException {
        return downloadManager.getDownloadIdInProgress();
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#getDownloadedArtifacts()
     */
    public Collection<DownloadArtifactInfo> getDownloads(@Nullable final Artifact artifact, @Nullable final Version version) throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = downloadManager.getDownloadedArtifacts();

        return FluentIterable.from(downloadedArtifacts.entrySet()).transformAndConcat(
                new Function<Map.Entry<Artifact, SortedMap<Version, Path>>, Iterable<DownloadArtifactInfo>>() {
                    @Override
                    public Iterable<DownloadArtifactInfo> apply(Map.Entry<Artifact, SortedMap<Version, Path>> entry) {
                        SortedMap<Version, Path> versions = entry.getValue();

                        List<DownloadArtifactInfo> l = new ArrayList<>(versions.size());
                        for (Map.Entry<Version, Path> versionEntry : versions.entrySet()) {
                            DownloadArtifactInfo info = new DownloadArtifactInfo();
                            info.setArtifact(entry.getKey().getName());
                            info.setFile(versionEntry.getValue().toString());
                            info.setVersion(versionEntry.getKey().toString());

                            try {
                                if (installManager.isInstallable(entry.getKey(), versionEntry.getKey())) {
                                    info.setStatus(DownloadArtifactStatus.READY_TO_INSTALL);
                                } else {
                                    info.setStatus(DownloadArtifactStatus.DOWNLOADED);
                                }
                            } catch (IOException e) {
                                info.setStatus(DownloadArtifactStatus.DOWNLOADED);
                            }

                            l.add(info);
                        }

                        return l;
                    }
                }).filter(new Predicate<DownloadArtifactInfo>() {
            @Override
            public boolean apply(DownloadArtifactInfo downloadArtifactInfo) {
                return artifact == null || downloadArtifactInfo.getArtifact().equals(artifact.getName());
            }
        }).filter(new Predicate<DownloadArtifactInfo>() {
            @Override
            public boolean apply(DownloadArtifactInfo downloadArtifactInfo) {
                return version == null || downloadArtifactInfo.getVersion().equals(version.toString());
            }
        }).toList();
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#getUpdates()
     */
    public Collection<UpdatesArtifactInfo> getUpdates() throws IOException {
        Map<Artifact, Version> updates = downloadManager.getUpdates();

        return FluentIterable.from(updates.entrySet()).transform(new Function<Map.Entry<Artifact, Version>, UpdatesArtifactInfo>() {
            @Override
            public UpdatesArtifactInfo apply(Map.Entry<Artifact, Version> entry) {
                Artifact artifact = entry.getKey();
                Version version = entry.getValue();

                UpdatesArtifactInfo info = new UpdatesArtifactInfo();
                info.setArtifact(artifact.getName());
                info.setVersion(version.toString());
                try {
                    if (downloadManager.getDownloadedVersions(artifact).containsKey(version)) {
                        info.setStatus(UpdatesArtifactStatus.DOWNLOADED);
                    } else {
                        info.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
                    }
                } catch (IOException e) {
                    // simply ignore
                }

                return info;
            }
        }).toList();
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#getAllUpdates
     */
    public Collection<UpdatesArtifactInfo> getAllUpdates(Artifact artifact) throws IOException, JsonParseException {
        Collection<Map.Entry<Artifact, Version>> allUpdates = downloadManager.getAllUpdates(artifact);

        Set<UpdatesArtifactInfo> infos = new TreeSet<>();

        infos.addAll(FluentIterable.from(allUpdates).transform(new Function<Map.Entry<Artifact, Version>, UpdatesArtifactInfo>() {
            @Override
            public UpdatesArtifactInfo apply(Map.Entry<Artifact, Version> entry) {
                Artifact artifact = entry.getKey();
                Version version = entry.getValue();

                UpdatesArtifactInfo info = new UpdatesArtifactInfo();
                info.setArtifact(artifact.getName());
                info.setVersion(version.toString());
                try {
                    if (downloadManager.getDownloadedVersions(artifact).containsKey(version)) {
                        info.setStatus(UpdatesArtifactStatus.DOWNLOADED);
                    } else {
                        info.setStatus(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD);
                    }
                } catch (IOException e) {
                    // simply ignore
                }

                return info;
            }
        }).toList());

        return infos;
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getInstalledArtifacts()
     */
    public Collection<InstallArtifactInfo> getInstalledVersions() throws IOException {
        Map<Artifact, Version> installedArtifacts = installManager.getInstalledArtifacts();

        ImmutableList<InstallArtifactInfo> installedArtifactInfos =
            FluentIterable.from(installedArtifacts.entrySet())
                          .transform(new Function<Map.Entry<Artifact, Version>, InstallArtifactInfo>() {
                                @Override
                                public InstallArtifactInfo apply(Map.Entry<Artifact, Version> entry) {
                                    InstallArtifactInfo info = new InstallArtifactInfo();
                                    info.setArtifact(entry.getKey().getName());
                                    info.setVersion(entry.getValue().toString());
                                    info.setStatus(InstallArtifactStatus.SUCCESS);
                                    return info;
                                }
                            }).toList();

        return new ArrayList<>(installedArtifactInfos);
    }

    /**
     * Merges two lists of downloaded and installed artifacts and returns a single one.
     */
    public Collection<ArtifactInfo> getArtifacts() throws IOException {
        Set<ArtifactInfo> infos = new TreeSet<>();

        final Map<Artifact, Version> installedArtifacts = installManager.getInstalledArtifacts();
        final Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = downloadManager.getDownloadedArtifacts();

        infos.addAll(FluentIterable.from(installedArtifacts.entrySet()).transform(
                new Function<Map.Entry<Artifact, Version>, ArtifactInfo>() {
                    @Override
                    public ArtifactInfo apply(Map.Entry<Artifact, Version> entry) {
                        ArtifactInfo info = new ArtifactInfo();
                        info.setArtifact(entry.getKey().getName());
                        info.setVersion(entry.getValue().toString());
                        info.setStatus(ArtifactStatus.INSTALLED);
                        return info;
                    }
                }).toList());

        infos.addAll(FluentIterable.from(downloadedArtifacts.entrySet()).transformAndConcat(
                new Function<Map.Entry<Artifact, SortedMap<Version, Path>>, Iterable<ArtifactInfo>>() {
                    @Override
                    public Iterable<ArtifactInfo> apply(Map.Entry<Artifact, SortedMap<Version, Path>> entry) {
                        SortedMap<Version, Path> versions = entry.getValue();

                        List<ArtifactInfo> l = new ArrayList<>(versions.size());
                        for (Map.Entry<Version, Path> versionEntry : versions.entrySet()) {
                            if (!installedArtifacts.containsKey(entry.getKey())
                                || !installedArtifacts.get(entry.getKey()).equals(versionEntry.getKey())) {

                                ArtifactInfo info = new ArtifactInfo();

                                info.setArtifact(entry.getKey().getName());
                                info.setVersion(versionEntry.getKey().toString());

                                try {
                                    if (installManager.isInstallable(entry.getKey(), versionEntry.getKey())) {
                                        info.setStatus(ArtifactStatus.READY_TO_INSTALL);
                                    } else {
                                        info.setStatus(ArtifactStatus.DOWNLOADED);
                                    }
                                } catch (IOException e) {
                                    info.setStatus(ArtifactStatus.DOWNLOADED);
                                }

                                l.add(info);
                            }
                        }

                        return l;
                    }
                }).toList());

        return infos;
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
    public String install(@Nonnull Artifact artifact,
                          @Nonnull Version version,
                          @Nonnull InstallOptions installOptions) throws IOException {
        SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(artifact);
        if (!downloadedVersions.containsKey(version)) {
            throw new FileNotFoundException(format("Binaries to install %s:%s not found", artifact.getName(), version.toString()));
        }

        return installManager.performInstallStep(artifact, version, downloadedVersions.get(version), installOptions);
    }

    /**
     * @throws java.io.FileNotFoundException
     *         if binaries to install given artifact not found
     * @see com.codenvy.im.managers.InstallManager#performUpdateStep
     */
    public String update(@Nonnull Artifact artifact,
                         @Nonnull Version version,
                         @Nonnull InstallOptions installOptions) throws IOException {
        SortedMap<Version, Path> downloadedVersions = downloadManager.getDownloadedVersions(artifact);
        if (!downloadedVersions.containsKey(version)) {
            throw new FileNotFoundException(format("Binaries to install %s:%s not found", artifact.getName(), version.toString()));
        }

        return installManager.performUpdateStep(artifact, version, downloadedVersions.get(version), installOptions);
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#waitForStepCompleted(String)
     */
    public void waitForInstallStepCompleted(String stepId) throws InstallationNotStartedException, InterruptedException {
        installManager.waitForStepCompleted(stepId);
    }

    /**
     * @see com.codenvy.im.saas.SaasAccountServiceProxy#getAccountWhereUserIsOwner(String, String)
     */
    @Nullable
    public AccountReference getAccountWhereUserIsOwner(@Nullable String accountName, @Nonnull String authToken) throws IOException {
        return saasAccountServiceProxy.getAccountWhereUserIsOwner(accountName, authToken);
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
    public NodeInfo addNode(@Nonnull String dns) throws IOException {
        NodeConfig nodeConfig = nodeManager.add(dns);

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setType(nodeConfig.getType());
        nodeInfo.setHost(nodeConfig.getHost());

        return nodeInfo;
    }

    /**
     * @see com.codenvy.im.managers.NodeManager#remove(String)
     */
    public NodeInfo removeNode(@Nonnull String dns) throws IOException {
        NodeConfig nodeConfig = nodeManager.remove(dns);

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setType(nodeConfig.getType());
        nodeInfo.setHost(nodeConfig.getHost());

        return nodeInfo;
    }

    /**
     * @see com.codenvy.im.managers.BackupManager#backup(com.codenvy.im.managers.BackupConfig)
     */
    public BackupInfo backup(@Nonnull BackupConfig config) throws IOException {
        BackupConfig backupConfig = backupManager.backup(config);

        BackupInfo backupInfo = new BackupInfo();
        backupInfo.setArtifact(backupConfig.getArtifactName());
        backupInfo.setFile(backupConfig.getBackupFile());
        backupInfo.setVersion(backupConfig.getArtifactVersion());

        return backupInfo;
    }

    /**
     * @see com.codenvy.im.managers.BackupManager#restore(com.codenvy.im.managers.BackupConfig)
     */
    public BackupInfo restore(@Nonnull BackupConfig backupConfig) throws IOException {
        backupManager.restore(backupConfig);

        BackupInfo backupInfo = new BackupInfo();
        backupInfo.setArtifact(backupConfig.getArtifactName());
        backupInfo.setFile(backupConfig.getBackupFile());
        backupInfo.setVersion(backupConfig.getArtifactVersion());

        return backupInfo;
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
    public SubscriptionDescriptor getSaasSubscription(String subscriptionName, @Nonnull SaasUserCredentials saasUserCredentials) throws IOException {
        return saasAccountServiceProxy.getSubscription(subscriptionName,
                                                       saasUserCredentials.getToken(),
                                                       saasUserCredentials.getAccountId());
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
    public void storeStorageProperties(@Nullable Map<String, String> newProperties) throws IOException {
        storageManager.storeProperties(newProperties);
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#loadProperties()
     */
    public Map<String, String> loadStorageProperties() throws IOException {
        return storageManager.loadProperties();
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#loadProperty(String)
     */
    @Nullable
    public String loadStorageProperty(@Nullable String key) throws IOException {
        return storageManager.loadProperty(key);
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#storeProperty(String, String)
     */
    public void storeStorageProperty(@Nullable String key, @Nullable String value) throws IOException {
        storageManager.storeProperty(key, value);
    }

    /**
     * @see com.codenvy.im.managers.StorageManager#deleteProperty(String)
     */
    public void deleteStorageProperty(@Nullable String name) throws IOException {
        storageManager.deleteProperty(name);
    }

    /** Update artifact config property */
    public void updateArtifactConfig(Artifact artifact, Map<String, String> properties) throws IOException {
        doUpdateArtifactConfig(artifact, properties);
    }

    protected void doUpdateArtifactConfig(Artifact artifact, Map<String, String> properties) throws IOException {
        artifact.updateConfig(properties);
    }

    /**
     * @see com.codenvy.im.managers.DownloadManager#deleteArtifact(com.codenvy.im.artifacts.Artifact, com.codenvy.im.utils.Version)
     */
    public void deleteDownloadedArtifact(Artifact artifact, Version version) throws IOException {
        downloadManager.deleteArtifact(artifact, version);
    }

    /**
     * @see com.codenvy.im.managers.InstallManager#getUpdateStepInfo(String)
     */
    public InstallArtifactStepInfo getUpdateStepInfo(String stepId) throws InstallationNotStartedException {
        return installManager.getUpdateStepInfo(stepId);
    }

    /**
     * @see com.codenvy.im.artifacts.Artifact#getLatestInstallableVersion()
     */
    public Version getLatestInstallableVersion(Artifact artifact) throws IOException {
        return artifact.getLatestInstallableVersion();
    }
}
