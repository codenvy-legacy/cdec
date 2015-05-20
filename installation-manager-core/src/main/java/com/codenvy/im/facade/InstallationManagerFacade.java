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
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallationManager;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.PasswordManager;
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
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
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
import static com.codenvy.im.utils.Commons.toJson;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
// TODO [AB] return response everywhere
// TODO [AB] no exceptions
// TODO [AB] user readable error messages
@Singleton
public class InstallationManagerFacade {
    private static final Logger LOG = Logger.getLogger(InstallationManagerFacade.class.getSimpleName());

    protected final InstallationManager     manager;
    protected final HttpTransport           transport;
    protected final SaasAuthServiceProxy    saasAuthServiceProxy;
    protected final SaasAccountServiceProxy saasAccountServiceProxy;
    protected final PasswordManager passwordManager;

    private final String updateServerEndpoint;

    private DownloadDescriptor downloadDescriptor;

    @Inject
    public InstallationManagerFacade(@Named("installation-manager.update_server_endpoint") String updateServerEndpoint,
                                     InstallationManager manager,
                                     HttpTransport transport,
                                     SaasAuthServiceProxy saasAuthServiceProxy,
                                     SaasAccountServiceProxy saasAccountServiceProxy,
                                     PasswordManager passwordManager) {
        this.manager = manager;
        this.transport = transport;
        this.updateServerEndpoint = updateServerEndpoint;
        this.saasAuthServiceProxy = saasAuthServiceProxy;
        this.saasAccountServiceProxy = saasAccountServiceProxy;
        this.passwordManager = passwordManager;
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
            return Response.valueOf(e).toJson();
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
            manager.checkIfConnectionIsAvailable();

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
            return Response.valueOf(e).toJson();
        }
    }

    private void download(Request request,
                          CountDownLatch latcher,
                          Thread currentThread) {

        downloadDescriptor = null;
        List<ArtifactInfo> infos = null;
        try {
            Map<Artifact, Version> updatesToDownload = manager.getUpdatesToDownload(request.createArtifact(), request.createVersion());

            infos = new ArrayList<>(updatesToDownload.size());

            downloadDescriptor = DownloadDescriptor.createDescriptor(updatesToDownload, manager, currentThread);
            manager.checkEnoughDiskSpace(downloadDescriptor.getTotalSize());

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

    protected Path doDownload(Artifact artifact, Version version) throws IOException, IllegalStateException {
        return manager.download(artifact, version);
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
            return Response.valueOf(e).toJson();
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
            return Response.valueOf(e).toJson();
        }
    }

    /** @return the list of downloaded artifacts */
    public String getDownloads(@Nonnull Request request) {
        try {
            try {
                List<ArtifactInfo> infos = new ArrayList<>();

                if (request.createArtifact() == null) {
                    Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = manager.getDownloadedArtifacts();

                    for (Map.Entry<Artifact, SortedMap<Version, Path>> e : downloadedArtifacts.entrySet()) {
                        infos.addAll(getDownloadedArtifactsInfo(e.getKey(), e.getValue()));
                    }
                } else {
                    SortedMap<Version, Path> downloadedVersions = manager.getDownloadedVersions(request.createArtifact());

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
                return Response.valueOf(e).toJson();
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
            Status status = manager.isInstallable(artifact, version) ? Status.READY_TO_INSTALL : Status.DOWNLOADED;

            info.add(new ArtifactInfo(artifact, version, pathToBinaries, status));
        }

        return info;
    }

    /** @return update list from the server */
    public String getUpdates() {
        try {
            Map<Artifact, Version> updates = manager.getUpdates();
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
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** @return the list of installed artifacts and theirs versions */
    public Response getInstalledVersions() throws IOException {
        Map<Artifact, Version> installedArtifacts = manager.getInstalledArtifacts();
        return new Response().setStatus(ResponseCode.OK).addArtifacts(installedArtifacts);
    }

    /** @return installation info */
    public String getInstallInfo(@Nonnull Request request) throws IOException {
        try {
            InstallOptions installOptions = request.getInstallOptions();
            Version version = doGetVersionToInstall(request);

            List<String> infos = manager.getInstallInfo(request.createArtifact(), version, installOptions);
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
                manager.install(request.obtainAccessToken(), request.createArtifact(), version, installOptions);
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
            Version version = manager.getLatestInstallableVersion(request.obtainAccessToken(), request.createArtifact());

            if (version == null) {
                throw new IllegalStateException(format("There is no newer version to install '%s'.", request.createArtifact()));
            }

            return version;

        } else {
            SortedMap<Version, Path> downloadedVersions = manager.getDownloadedVersions(request.createArtifact());
            if (downloadedVersions.isEmpty()) {
                throw new IllegalStateException(format("Installation in progress but binaries for '%s' not found.", request.createArtifact()));
            }
            return downloadedVersions.keySet().iterator().next();
        }
    }

    /** @return account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @Nullable
    public String getAccountReferenceWhereUserIsOwner(@Nullable String accountName, @Nonnull Request request) throws IOException {
        AccountReference accountReference = saasAccountServiceProxy.getAccountReferenceWhereUserIsOwner(request.obtainAccessToken(),
                                                                                                        accountName);
        return accountReference == null ? null : toJson(accountReference);
    }

    /** @return the configuration of the Installation Manager */
    public String getConfig() {
        return new Response().setStatus(ResponseCode.OK).setConfig(manager.getConfig()).toJson();
    }

    /** Add node to multi-server Codenvy */
    public String addNode(@Nonnull String dns) {
        try {
            NodeConfig node = manager.addNode(dns);
            return new Response().setNode(NodeInfo.createSuccessInfo(node))
                                 .setStatus(ResponseCode.OK)
                                 .toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** Remove node from multi-server Codenvy */
    public String removeNode(@Nonnull String dns) {
        try {
            NodeConfig node = manager.removeNode(dns);
            return new Response().setNode(NodeInfo.createSuccessInfo(node))
                                 .setStatus(ResponseCode.OK)
                                 .toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }

    /** Perform backup according to certain backup config */
    public String backup(@Nonnull BackupConfig config) throws IOException {
        try {
            BackupConfig updatedConfig = manager.backup(config);
            return new Response().setBackup(BackupInfo.createSuccessInfo(updatedConfig))
                                 .setStatus(ResponseCode.OK)
                                 .toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e)
                           .setBackup(BackupInfo.createFailureInfo(config))
                           .toJson();
        }
    }

    /** Perform restore according to certain backup config */
    public String restore(@Nonnull BackupConfig config) throws IOException {
        try {
            manager.restore(config);
            return new Response().setBackup(BackupInfo.createSuccessInfo(config))
                                 .setStatus(ResponseCode.OK)
                                 .toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e)
                           .setBackup(BackupInfo.createFailureInfo(config))
                           .toJson();
        }
    }

    /** Login into SaaS Codenvy and return authToken */
    public String loginToCodenvySaaS(@Nonnull Credentials credentials) throws IOException, JsonParseException {
        Token authToken = saasAuthServiceProxy.login(credentials);
        return authToken == null ? null : toJson(authToken);
    }


    /** Get user's certain subscription descriptor */
    @Nullable
    public SubscriptionDescriptor getSubscriptionDescriptor(String subscriptionName,
                                                            @Nonnull Request request) throws IOException {
        return saasAccountServiceProxy.getSubscription(subscriptionName,
                                                       request.obtainAccessToken(),
                                                       request.obtainAccountId());
    }

    /** Perform restore according to certain backup config */
    public Response changeAdminPassword(byte[] currentPassword, byte[] newPassword) {
        try {
            passwordManager.changeAdminPassword(currentPassword, newPassword);
            return new Response().setStatus(ResponseCode.OK);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e);
        }
    }

    /**
     * @see com.codenvy.im.managers.InstallationManager#storeProperties(java.util.Map)
     */
    public Response storeProperties(Map<String, String> newProperties) {
        try {
            manager.storeProperties(newProperties);
            return new Response().setStatus(ResponseCode.OK);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e);
        }
    }

    /**
     * @see com.codenvy.im.managers.InstallationManager#readProperties(java.util.Collection)
     */
    public Response readProperties(@Nullable Collection<String> names) {
        if (names == null) {
            return new Response().setStatus(ResponseCode.OK);
        }

        try {
            LinkedHashMap<String, String> properties = new LinkedHashMap<>(manager.readProperties(names));
            return new Response().setStatus(ResponseCode.OK).setConfig(properties);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e);
        }
    }

    /** Change codenvy config property */
    public String changeCodenvyConfig(String property, String value) {
        try {
            manager.changeCodenvyConfig(property, value);
            return new Response().setStatus(ResponseCode.OK).toJson();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.valueOf(e).toJson();
        }
    }
}
