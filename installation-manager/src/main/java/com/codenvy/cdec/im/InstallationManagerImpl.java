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

import static com.codenvy.cdec.utils.Commons.combinePaths;
import static com.codenvy.cdec.utils.Commons.fromJson;
import static com.codenvy.cdec.utils.Commons.getLatestVersion;
import static com.codenvy.cdec.utils.Version.compare;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.AuthenticationException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.HttpException;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.cdec.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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

    //TODO REMOVE
    /** {@inheritDoc} */
    @Override
    public String install(Artifact artifact) throws IOException {
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
    /** {@inheritDoc} */
    public void install(Artifact artifact, String version) throws IOException {
        Map<Artifact, String> installedArtifacts = getInstalledArtifacts();
        Map<Artifact, Path> downloadedArtifacts = getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact);
            String availableVersion = Commons.extractVersion(pathToBinaries);

            if (!version.equals(availableVersion)) {
                throw new FileNotFoundException("Binaries to install artifact " + artifact.getName() + ":" + version + " not found");
            }

            String installedVersion = installedArtifacts.get(artifact);

            if (installedVersion == null || Version.compare(version, installedVersion) > 1) {
                artifact.install(pathToBinaries);

            } else if (installedVersion != null && Version.compare(version, installedVersion) < 0) {
                throw new FileNotFoundException("Can not install the artifact '" + artifact.getName() + ":" + version
                                                + "', because we don't support downgrade artifacts." );
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact not found");
        }
    }

    // TODO remove duplicate of getInstalledArtifacts(String accessToken)
    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getInstalledArtifacts() throws IOException {
        return getInstalledArtifacts(null);
    }
    
    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getInstalledArtifacts(String accessToken) throws IOException {
        Map<Artifact, String> installed = new HashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                installed.put(artifact, artifact.getCurrentVersion(accessToken));
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** {@inheritDoc} */
    @Override
    public void download(Artifact artifact, String version) throws IOException, IllegalStateException {
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
            throw new IllegalStateException("Valid subscription is required to download " + artifact.getName());
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
    // TODO remove duplicate of getUpdates(String accessToken)
    @Override
    public Map<Artifact, String> getUpdates() throws IOException {
        return getUpdates(null);
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getUpdates(String accessToken) throws IOException {
        Map<Artifact, String> newVersions = new LinkedHashMap<>();

        Map<Artifact, String> installed = getInstalledArtifacts(accessToken);
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

    protected boolean isValidSubscription() throws IOException {
        return Commons.isValidSubscription(transport, apiEndpoint, "On-Premises");
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
    
    /**
     * Returns correct exception depending on initial type of exception.
     */
    protected IOException getProperException(IOException e, Artifact artifact) {
        if (e instanceof HttpException && ((HttpException) e).getStatus() == 404) {
            return new ArtifactNotFoundException(artifact.getName());
            
        } else if (e instanceof HttpException && ((HttpException) e).getStatus() == 302) {
            return new AuthenticationException();
            
        } else {
            return e;
        }
    }
}