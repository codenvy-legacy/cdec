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
package com.codenvy.cdec.im;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.codenvy.cdec.utils.Commons.*;
import static com.codenvy.cdec.utils.Version.compare;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManager.class);

    private final String apiEndpoint;
    private final String updateEndpoint;
    private final String downloadDir;

    protected final HttpTransport transport;
    protected final Set<Artifact> artifacts;

    private final Map<Artifact, String> newVersions;

    @Inject
    public InstallationManagerImpl(@Named("api.endpoint") String apiEndpoint,
                                   @Named("codenvy.installation-manager.update_endpoint") String updateEndpoint,
                                   @Named("codenvy.installation-manager.download_dir") String downloadDir,
                                   HttpTransport transport,
                                   Set<Artifact> artifacts) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.apiEndpoint = apiEndpoint;
        this.downloadDir = downloadDir;
        this.transport = transport;
        this.artifacts = artifacts;
        this.newVersions = new ConcurrentHashMap<>(artifacts.size());

        if (!Files.exists(Paths.get(downloadDir))) {
            Files.createDirectories(Paths.get(downloadDir));
        }

        LOG.info("Download directory " + downloadDir);
    }

    @Override
    public Map<Artifact, String> getInstalledArtifacts() throws IOException {
        Map<Artifact, String> installed = new HashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                installed.put(artifact, artifact.getCurrentVersion());
            } catch (IOException e) {
                throw new IOException("Can't find out current version of " + artifact, e);
            }
        }

        return installed;
    }

    @Override
    public Map<Artifact, String> getAvailable2DownloadArtifacts() throws IOException {
        Map<Artifact, String> available2Download = new HashMap<>();

        for (Artifact artifact : artifacts) {
            try {
                Map m = fromJson(transport.doGetRequest(combinePaths(updateEndpoint, "repository/version/" + artifact.getName())), Map.class);
                if (m != null && m.containsKey("version")) {
                    available2Download.put(artifact, (String)m.get("version"));
                }
            } catch (IOException e) {
                LOG.error("Can't retrieve the last version of " + artifact, e);
            }
        }

        return available2Download;
    }

    @Override
    public void downloadUpdates() throws IOException {
        for (Map.Entry<Artifact, String> entry : newVersions.entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            if (!artifact.isValidSubscriptionRequired() || isValidSubscription(transport, apiEndpoint, "On-Premises")) {
                transport
                        .download(combinePaths(updateEndpoint, "/repository/download/" + artifact.getName() + "/" + version), Paths.get(downloadDir));
                LOG.info("Downloaded '" + artifact + "' version " + version);
            } else {
                LOG.warn("Valid subscription is required to download " + artifact.getName());
            }
        }
    }

    @Override
    public Map<Artifact, String> getNewVersions() {
        return newVersions;
    }

    @Override
    public void checkNewVersions() throws IOException {
        invalidateNewVersions();

        Map<Artifact, String> installed = getInstalledArtifacts();
        Map<Artifact, String> available2Download = getAvailable2DownloadArtifacts();

        for (Map.Entry<Artifact, String> entry : available2Download.entrySet()) {
            Artifact artifact = entry.getKey();
            String newVersion = entry.getValue();

            if (!installed.containsKey(artifact) || compare(newVersion, installed.get(artifact)) > 0) {
                newVersions.put(artifact, newVersion);
                LOG.info("New version '" + artifact + "' " + newVersions.get(artifact) + " available to download");
            }
        }
    }

    private void invalidateNewVersions() {
        newVersions.clear();
    }
}
