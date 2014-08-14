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

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.cdec.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.codenvy.cdec.utils.Commons.*;
import static com.codenvy.cdec.utils.Version.compare;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManager.class);

    private final String apiEndpoint;
    private final String updateEndpoint;
    private final Path   downloadDir;

    private final HttpTransport transport;
    private final Set<Artifact> artifacts;

    private final Map<Artifact, String> newVersions;


    @Inject
    public InstallationManagerImpl(@Named("api.endpoint") String apiEndpoint,
                                   @Named("codenvy.installation-manager.update_endpoint") String updateEndpoint,
                                   @Named("codenvy.installation-manager.download_dir") String downloadDir,
                                   HttpTransport transport,
                                   Set<Artifact> artifacts) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.apiEndpoint = apiEndpoint;
        this.downloadDir = Paths.get(downloadDir);
        this.transport = transport;
        this.artifacts = new TreeSet<>(artifacts); // keep order
        this.newVersions = new ConcurrentHashMap<>(artifacts.size());

        if (!Files.exists(this.downloadDir)) {
            Files.createDirectories(this.downloadDir);
        }

        LOG.info("Download directory " + downloadDir);
        LOG.info(artifacts.getClass().getName());
    }

    @Override
    public String installArtifact(Artifact artifact) throws IOException {
        Map<Artifact, String> installedArtifacts = getInstalledArtifacts();
        Map<Artifact, Path> downloadedArtifacts = getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact);
            String version = Commons.extractVersion(pathToBinaries);
            String installedVersion = installedArtifacts.get(artifact);

            if (installedVersion == null || Version.compare(version, installedVersion) > 1) {
                artifact.install(pathToBinaries);
            }

            return version;
        } else {
            throw new FileNotFoundException("Binaries to install artifact not found");
        }
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
        for (Map.Entry<Artifact, String> entry : getNewVersions().entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            boolean isValidSubscriptionRequired = artifact.isValidSubscriptionRequired();
            String requestUrl = combinePaths(updateEndpoint,
                                             "/repository/"
                                             + (isValidSubscriptionRequired ? "" : "public/")
                                             + "download/" + artifact.getName() + "/" + version);

            if (!isValidSubscriptionRequired || isValidSubscription()) {
                Path artifactDownloadDir = getArtifactDownloadedDir(artifact, version);
                FileUtils.deleteDirectory(artifactDownloadDir.toFile());

                transport.download(requestUrl, artifactDownloadDir);

                LOG.info("Downloaded '" + artifact + "' version " + version);
            } else {
                LOG.warn("Valid subscription is required to download " + artifact.getName());
            }
        }
    }

    /**
     * @return downloaded artifacts from the local repository
     * @throws IOException
     *         if an I/O error occurs
     */
    protected Map<Artifact, Path> getDownloadedArtifacts() throws IOException {
        Map<Artifact, Path> downloaded = new HashMap<>(artifacts.size());

        for (Artifact artifact : artifacts) {
            String version;
            try {
                version = getLatestVersion(artifact.getName(), downloadDir.resolve(artifact.getName()));
            } catch (ArtifactNotFoundException e) {
                continue;
            }

            Path artifactDownloadDir = getArtifactDownloadedDir(artifact, version);
            if (Files.exists(artifactDownloadDir)) {
                Iterator<Path> iter = Files.newDirectoryStream(artifactDownloadDir).iterator();

                if (iter.hasNext()) {
                    downloaded.put(artifact, iter.next());

                    if (iter.hasNext()) {
                        throw new IOException(
                                "Unexpected error. Found more than 1 downloaded artifact '" + artifact.getName() + "'. Clean the directory '" +
                                artifactDownloadDir.toString() + "' and redownload artifact.");
                    }
                }
            }
        }

        return downloaded;
    }

    private Path getArtifactDownloadedDir(Artifact artifact, String version) {
        return downloadDir.resolve(artifact.getName()).resolve(version);
    }

    protected boolean isValidSubscription() throws IOException {
        return Commons.isValidSubscription(transport, apiEndpoint, "On-Premises");
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

    @Override
    public Map<Artifact, String> getNewVersions() {
        return newVersions;
    }

    private void invalidateNewVersions() {
        newVersions.clear();
    }
}
