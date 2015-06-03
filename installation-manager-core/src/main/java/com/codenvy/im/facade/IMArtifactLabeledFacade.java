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
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.VersionLabel;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/**
 * Sets {@link com.codenvy.im.artifacts.VersionLabel} to every artifact info.
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class IMArtifactLabeledFacade extends InstallationManagerFacade {

    @Inject
    public IMArtifactLabeledFacade(@Named("installation-manager.download_dir") String downloadDir,
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
        super(downloadDir,
              updateServerEndpoint,
              transport,
              saasAuthServiceProxy,
              saasAccountServiceProxy,
              passwordManager,
              nodeManager,
              backupManager,
              storageManager,
              installManager,
              downloadManager);
    }


    /** {@inheritDoc} */
    @Override
    public List<InstallArtifactInfo> getInstalledVersions() throws IOException {
        List<InstallArtifactInfo> installedVersions = super.getInstalledVersions();

        for (InstallArtifactInfo info : installedVersions) {
            VersionLabel versionLabel = fetchVersionLabel(info.getArtifact(), info.getVersion());
            info.setLabel(versionLabel);
        }

        return installedVersions;
    }

    /** {@inheritDoc} */
    @Override
    public List<UpdatesArtifactInfo> getUpdates() throws IOException {
        List<UpdatesArtifactInfo> updates = super.getUpdates();

        for (UpdatesArtifactInfo info : updates) {
            VersionLabel versionLabel = fetchVersionLabel(info.getArtifact(), info.getVersion());
            info.setLabel(versionLabel);

        }

        return updates;
    }

    /** {@inheritDoc} */
    @Override
    public List<DownloadArtifactInfo> getDownloads(@Nullable Artifact artifact, @Nullable Version version) throws IOException {
        List<DownloadArtifactInfo> downloads = super.getDownloads(artifact, version);

        for (DownloadArtifactInfo info : downloads) {
            VersionLabel versionLabel = fetchVersionLabel(info.getArtifact(), info.getVersion());
            info.setLabel(versionLabel);
        }

        return downloads;
    }

    @Nullable
    protected VersionLabel fetchVersionLabel(@Nonnull String artifactName, @Nonnull String versionNumber) throws IOException {
        Artifact artifact = createArtifact(artifactName);
        Version version = Version.valueOf(versionNumber);

        Map<String, String> properties;
        try {
            properties = artifact.getProperties(version);
        } catch (IOException e) {
            return null;
        }

        String label = properties.get(ArtifactProperties.LABEL_PROPERTY);
        return label != null ? VersionLabel.valueOf(label.toUpperCase()) : null;
    }
}
