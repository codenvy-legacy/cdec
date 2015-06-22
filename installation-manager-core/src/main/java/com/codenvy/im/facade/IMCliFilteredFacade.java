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
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.BasicArtifactInfo;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Get rids of any references to {@link com.codenvy.im.artifacts.InstallManagerArtifact} from REST service
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class IMCliFilteredFacade extends IMArtifactLabeledFacade {

    @Inject
    public IMCliFilteredFacade(@Named("installation-manager.download_dir") String downloadDir,
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
    public Collection<InstallArtifactInfo> getInstalledVersions() throws IOException {
        Collection<InstallArtifactInfo> installedVersions = new ArrayList<>(super.getInstalledVersions());
        removeImCliArtifact(installedVersions);
        return installedVersions;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<UpdatesArtifactInfo> getUpdates() throws IOException {
        Collection<UpdatesArtifactInfo> updates = new ArrayList<>(super.getUpdates());
        removeImCliArtifact(updates);
        return updates;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<DownloadArtifactInfo> getDownloads(@Nullable Artifact artifact, @Nullable Version version) throws IOException {
        Collection<DownloadArtifactInfo> downloads = new ArrayList<>(super.getDownloads(artifact, version));
        removeImCliArtifact(downloads);
        return downloads;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<ArtifactInfo> getArtifacts() throws IOException {
        Collection<ArtifactInfo> artifacts = new ArrayList<>(super.getArtifacts());
        removeImCliArtifact(artifacts);
        return artifacts;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<UpdatesArtifactInfo> getAllUpdates(@Nullable Artifact artifact) throws IOException, JsonParseException {
        Collection<UpdatesArtifactInfo> updates = new ArrayList<>(super.getAllUpdates(artifact));
        removeImCliArtifact(updates);
        return updates;
    }

    /** {@inheritDoc} */
    @Override
    public String install(@Nonnull Artifact artifact, @Nonnull Version version, @Nonnull InstallOptions installOptions) throws IOException {
        if (artifact.getName().equals(InstallManagerArtifact.NAME)) {
            throw new UnsupportedOperationException(InstallManagerArtifact.NAME + " can't be installed");
        }
        return super.install(artifact, version, installOptions);
    }

    /** {@inheritDoc} */
    @Override
    public String update(@Nonnull Artifact artifact, @Nonnull Version version, @Nonnull InstallOptions installOptions) throws IOException {
        if (artifact.getName().equals(InstallManagerArtifact.NAME)) {
            throw new UnsupportedOperationException(InstallManagerArtifact.NAME + " can't be updated");
        }
        return super.update(artifact, version, installOptions);
    }

    @Override
    public Version getLatestInstallableVersion(Artifact artifact) throws IOException {
        if (artifact.getName().equals(InstallManagerArtifact.NAME)) {
            throw new UnsupportedOperationException(InstallManagerArtifact.NAME + " can't be updated");
        }
        return super.getLatestInstallableVersion(artifact);
    }

    protected void removeImCliArtifact(Collection<? extends BasicArtifactInfo> infos) throws IOException {
        Iterator<? extends BasicArtifactInfo> iter = infos.iterator();

        while (iter.hasNext()) {
            BasicArtifactInfo info = iter.next();
            if (info.getArtifact().equals(InstallManagerArtifact.NAME)) {
                iter.remove();
            }
        }
    }
}
