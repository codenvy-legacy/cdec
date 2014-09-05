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
import com.codenvy.cdec.exceptions.ArtifactNotFoundException;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.user.UserCredentials;
import com.codenvy.cdec.utils.AccountUtils;
import com.codenvy.cdec.utils.HttpTransport;
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

        if (!Files.exists(this.downloadDir)) {
            Files.createDirectories(this.downloadDir);
        }

        LOG.info("Download directory " + downloadDir);
        LOG.info(artifacts.getClass().getName());
    }

    /** {@inheritDoc} */
    @Override
    public void install(String authToken, Artifact artifact, String version) throws IOException {
        Map<Artifact, String> installedArtifacts = getInstalledArtifacts(authToken);
        Map<Artifact, Path> downloadedArtifacts = getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact);
            String availableVersion = extractVersion(pathToBinaries);

            if (!version.equals(availableVersion)) {
                throw new FileNotFoundException("Binaries to install artifact '" + artifact.getName() + "' version '" + version + "' not found");
            }

            String installedVersion = installedArtifacts.get(artifact);

            if (installedVersion == null || compare(version, installedVersion) > 0) {
                artifact.install(pathToBinaries);

            } else if (compare(version, installedVersion) < 0) {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version
                                                + "', because greater version is installed.");
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact not found");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getInstalledArtifacts(String authToken) throws IOException {
        Map<Artifact, String> installed = new HashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                installed.put(artifact, artifact.getCurrentVersion(authToken));
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** {@inheritDoc} */
    @Override
    public void download(UserCredentials userCredentials, Artifact artifact, String version) throws IOException, IllegalStateException {
        try {
            boolean isValidSubscriptionRequired = artifact.isValidSubscriptionRequired();

            String requestUrl = combinePaths(updateEndpoint,
                                             "/repository/"
                                             + (isValidSubscriptionRequired ? "" : "public/")
                                             + "download/" + artifact.getName() + "/" + version);

            if (!isValidSubscriptionRequired || isValidSubscription(userCredentials)) {
                Path artifactDownloadDir = getArtifactDownloadedDir(artifact, version);
                FileUtils.deleteDirectory(artifactDownloadDir.toFile());

                transport.download(requestUrl, artifactDownloadDir, userCredentials.getToken());
                LOG.info("Downloaded '" + artifact + "' version " + version);
            } else {
                throw new IllegalStateException("Valid subscription is required to download " + artifact.getName());
            }
        } catch (IOException e) {
            throw getProperException(e, artifact);
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

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getUpdates(String authToken) throws IOException {
        Map<Artifact, String> newVersions = new LinkedHashMap<>();

        Map<Artifact, String> installed = getInstalledArtifacts(authToken);
        Map<Artifact, String> available2Download = getLatestVersionsToDownload();

        for (Map.Entry<Artifact, String> entry : available2Download.entrySet()) {
            Artifact artifact = entry.getKey();
            String newVersion = entry.getValue();

            if (!installed.containsKey(artifact) || compare(newVersion, installed.get(artifact)) > 0) {
                newVersions.put(artifact, newVersion);
                LOG.info("New version '" + artifact + "' " + newVersions.get(artifact) + " available to download");
            }
        }

        return newVersions;
    }

    protected Path getArtifactDownloadedDir(Artifact artifact, String version) {
        return downloadDir.resolve(artifact.getName()).resolve(version);
    }

    protected boolean isValidSubscription(UserCredentials userCredentials) throws IOException {
        return AccountUtils.isValidSubscription(transport, apiEndpoint, "On-Premises", userCredentials); // TODO type of subscription is being stored in .properties file in artifact folder in update server
    }

    /** Retrieves the latest versions from the Update Server. */
    protected Map<Artifact, String> getLatestVersionsToDownload() throws IOException {
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
}