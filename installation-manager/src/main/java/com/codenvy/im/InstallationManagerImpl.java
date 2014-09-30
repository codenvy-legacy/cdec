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
package com.codenvy.im;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.ArtifactPropertiesUtils.isAuthenticationRequired;
import static com.codenvy.im.utils.Commons.calculateMD5Sum;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.fromJson;
import static com.codenvy.im.utils.Commons.getProperException;
import static com.codenvy.im.utils.Version.compare;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManager.class);

    private final String updateEndpoint;
    private final Path   downloadDir;

    private final HttpTransport transport;
    private final Set<Artifact> artifacts;

    @Inject
    public InstallationManagerImpl(@Named("api.endpoint") String apiEndpoint,
                                   @Named("installation-manager.update_server_endpoint") String updateEndpoint,
                                   @Named("installation-manager.download_dir") String downloadDir,
                                   HttpTransport transport,
                                   Set<Artifact> artifacts) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.downloadDir = Paths.get(downloadDir);
        this.transport = transport;
        this.artifacts = new TreeSet<>(artifacts); // keep order

        if (!exists(this.downloadDir)) {
            Files.createDirectories(this.downloadDir);
        }

        LOG.info("Download directory: " + downloadDir);
        LOG.info("Codenvy API endpoint: " + apiEndpoint);
        LOG.info("Codenvy Update Server API endpoint: " + updateEndpoint);
    }

    /** {@inheritDoc} */
    @Override
    public void install(String authToken, Artifact artifact, String version) throws IOException {
        Map<Artifact, String> installedArtifacts = getInstalledArtifacts(authToken);
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = getDownloadedArtifacts();

        Version v = Version.valueOf(version);

        if (downloadedArtifacts.containsKey(artifact)
            && downloadedArtifacts.get(artifact).containsKey(v)) { // TODO test

            Path pathToBinaries = downloadedArtifacts.get(artifact).get(v);
            String installedVersion = installedArtifacts.get(artifact);

            if (installedVersion == null || compare(version, installedVersion) > 0) {
                artifact.install(pathToBinaries);

            } else if (compare(version, installedVersion) < 0) {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version
                                                + "', because greater version is installed already.");
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact '" + artifact.getName() + "' version '" + version + "' not found");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, String> getInstalledArtifacts(String authToken) throws IOException {
        Map<Artifact, String> installed = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                if (!(artifact instanceof CDECArtifact)) {
                    installed.put(artifact, artifact.getInstalledVersion(authToken));
                }
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** {@inheritDoc} */
    @Override
    public Path download(UserCredentials userCredentials, Artifact artifact, String version) throws IOException, IllegalStateException {
        try {
            boolean isAuthenticationRequired = isAuthenticationRequired(artifact.getName(), version, transport, updateEndpoint);

            String requestUrl;
            if (isAuthenticationRequired) {
                if (userCredentials.getAccountId() == null) {
                    throw new IllegalStateException("Account ID is unknown. Please login using im:login command");
                }

                requestUrl = combinePaths(updateEndpoint,
                                          "/repository/download/" + artifact.getName() + "/" + version + "/" + userCredentials.getAccountId());
            } else {
                requestUrl = combinePaths(updateEndpoint,
                                          "/repository/public/download/" + artifact.getName() + "/" + version);
            }

            Path artifactDownloadDir = getDownloadDirectory(artifact, version);
            deleteDirectory(artifactDownloadDir.toFile());

            Path file = transport.download(requestUrl, artifactDownloadDir, userCredentials.getToken());
            LOG.info("Downloaded '" + artifact + "' version " + version);

            return file;
        } catch (IOException e) {
            throw getProperException(e, artifact);
        }
    }

    /**
     * @return downloaded artifacts from the local repository
     * @throws IOException
     *         if an I/O error occurs
     */
    public Map<Artifact, SortedMap<Version, Path>> getDownloadedArtifacts() throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloaded = new HashMap<>(artifacts.size());

        for (Artifact artifact : artifacts) {
            Path artifactDir = downloadDir.resolve(artifact.getName());

            if (exists(artifactDir)) {

                SortedMap<Version, Path> versions = new TreeMap<>();
                Iterator<Path> pathIterator = Files.newDirectoryStream(artifactDir).iterator();

                while (pathIterator.hasNext()) {
                    try {
                        Path versionDir = pathIterator.next();
                        if (isDirectory(versionDir)) {
                            Version version = valueOf(versionDir.getFileName().toString());

                            Map properties = getArtifactProperties(artifact, version.getAsString());
                            String md5sum = properties.get(MD5_PROPERTY).toString();
                            String fileName = properties.get(FILE_NAME_PROPERTY).toString();

                            Path file = versionDir.resolve(fileName);
                            if (exists(file) && md5sum.equals(calculateMD5Sum(file))) {
                                versions.put(version, file);
                            }
                        }
                    } catch (IllegalArgumentException | IOException e) {
                        // maybe it isn't a version directory
                    }
                }

                if (!versions.isEmpty()) {
                    downloaded.put(artifact, versions);
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
            }
        }

        return newVersions;
    }

    protected Path getDownloadDirectory(Artifact artifact, String version) {
        return downloadDir.resolve(artifact.getName()).resolve(version);
    }

    /** Retrieves the latest versions from the Update Server. */
    protected Map<Artifact, String> getLatestVersionsToDownload() throws IOException {
        Map<Artifact, String> available2Download = new LinkedHashMap<>();

        for (Artifact artifact : artifacts) {
            try {
                Map m = getArtifactProperties(artifact);
                available2Download.put(artifact, m.get(VERSION_PROPERTY).toString());
            } catch (IOException e) {
                LOG.error("Can't retrieve the last version of " + artifact, e);
            }
        }

        return available2Download;
    }

    // TODO test
    private Map getArtifactProperties(Artifact artifact) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + artifact.getName());
        Map m = fromJson(transport.doGetRequest(requestUrl), Map.class);

        validateProperties(m);
        return m;
    }

    // TODO test
    private Map getArtifactProperties(Artifact artifact, String version) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + artifact.getName() + "/" + version);
        Map m = fromJson(transport.doGetRequest(requestUrl), Map.class);

        validateProperties(m);
        return m;
    }

    // TODO test
    protected void validateProperties(Map m) throws IOException {
        if (m == null) {
            throw new IOException("Can't get artifact properties.");
        }

        for (String p : ArtifactProperties.PUBLIC_PROPERTIES) {
            if (!m.containsKey(p)) {
                throw new IOException("Can't get artifact property: " + p);
            }
        }
    }

}