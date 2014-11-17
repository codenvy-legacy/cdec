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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.restlet.InstallationManager;
import com.codenvy.im.restlet.InstallationManagerConfig;
import com.codenvy.im.user.UserCredentials;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.HttpTransportConfiguration;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SIZE_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.ArtifactPropertiesUtils.isAuthenticationRequired;
import static com.codenvy.im.utils.Commons.ArtifactsSet;
import static com.codenvy.im.utils.Commons.asMap;
import static com.codenvy.im.utils.Commons.calculateMD5Sum;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.extractServerUrl;
import static com.codenvy.im.utils.Commons.getProperException;
import static com.codenvy.im.utils.Version.compare;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManager.class);

    private Path downloadDir; // not final, possibly to be configured

    private final HttpTransportConfiguration transportConf;

    private final String        updateEndpoint;
    private final HttpTransport transport;
    private final Set<Artifact> artifacts;

    @Inject
    public InstallationManagerImpl(@Named("api.endpoint") String apiEndpoint,
                                   @Named("installation-manager.update_server_endpoint") String updateEndpoint,
                                   @Named("installation-manager.download_dir") String downloadDir,
                                   HttpTransportConfiguration transportConf,
                                   HttpTransport transport,
                                   Set<Artifact> artifacts) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.transportConf = transportConf;
        this.transport = transport;
        this.artifacts = new ArtifactsSet(artifacts); // keep order

        try {
            createAndSetDownloadDir(Paths.get(downloadDir));
        } catch (IOException e) {
            createAndSetDownloadDir(Paths.get(System.getenv("HOME"), "codenvy-updates"));
        }

        LOG.info("Download directory: " + this.downloadDir.toString());
        LOG.info("Codenvy API endpoint: " + apiEndpoint);
        LOG.info("Codenvy Update Server API endpoint: " + updateEndpoint);
    }

    private void createAndSetDownloadDir(Path downloadDir) throws IOException {
        if (!exists(downloadDir)) {
            createDirectories(downloadDir);
            checkRWPermissions(downloadDir);
        }

        this.downloadDir = downloadDir;
    }

    private void checkRWPermissions(Path downloadDir) throws IOException {
        Path tmp = downloadDir.resolve("tmp.tmp");
        createFile(tmp);
        delete(tmp);
    }

    /** {@inheritDoc} */
    @Override
    public void install(String authToken, Artifact artifact, String version, InstallOptions options) throws IOException, CommandException {
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = getDownloadedArtifacts();

        Version v = Version.valueOf(version);

        if (downloadedArtifacts.containsKey(artifact)
            && downloadedArtifacts.get(artifact).containsKey(v)) {

            Path pathToBinaries = downloadedArtifacts.get(artifact).get(v);
            if (artifact.isInstallable(v, authToken)) {
                artifact.install(pathToBinaries, options);
            } else {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version + "'.");
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
                installed.put(artifact, artifact.getInstalledVersion(authToken));
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
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkEnoughDiskSpace(long requiredSize) throws IOException {
        long freeSpace = downloadDir.toFile().getFreeSpace();
        if (freeSpace < requiredSize) {
            throw new IOException(String.format("Not enough disk space. Required %d bytes but available only %d bytes", requiredSize, freeSpace));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkIfConnectionIsAvailable() throws IOException {
        transport.doGet(combinePaths(updateEndpoint, "repository/properties/" + InstallManagerArtifact.NAME));
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getConfig() {
        return new LinkedHashMap<String, String>() {{
            put("download directory", downloadDir.toString());
            put("base url", extractServerUrl(updateEndpoint));

            if (transportConf.getProxyUrl() != null && !transportConf.getProxyUrl().isEmpty()) {
                put("proxy url", transportConf.getProxyUrl());
            }

            if (transportConf.getProxyPort() > 0) {
                put("proxy port", String.valueOf(transportConf.getProxyPort()));
            }
        }};
    }

    /** {@inheritDoc} */
    @Override
    public void setConfig(InstallationManagerConfig config) throws IOException {
        if (config.getProxyPort() != null) {
            transportConf.setProxyPort(config.getProxyPort());
            storeProperty("installation-manager.proxy_port", String.valueOf(transportConf.getProxyPort()));
        }

        if (config.getProxyUrl() != null) {
            transportConf.setProxyUrl(config.getProxyUrl());
            storeProperty("installation-manager.proxy_url", transportConf.getProxyUrl());
        }

        if (config.getDownloadDir() != null) {
            Path currentDownloadDir = this.downloadDir;
            Path newDownloadDir = Paths.get(config.getDownloadDir());

            validatePath(newDownloadDir);

            try {
                createAndSetDownloadDir(newDownloadDir);
            } catch (IOException e) {
                this.downloadDir = currentDownloadDir;
                throw new IOException("Can't set new download directory. Installation Manager probably doesn't have r/w permissions.", e);
            }

            try {
                storeProperty("installation-manager.download_dir", newDownloadDir.toString());
            } catch (IOException e) {
                this.downloadDir = currentDownloadDir;
                throw e;
            }
        }
    }

    protected void validatePath(Path newDownloadDir) throws IOException {
        if (!newDownloadDir.isAbsolute()) {
            throw new IOException("Path must be absolute.");
        }
    }

    @Override
    public Path getPathToBinaries(Artifact artifact, String version) throws IOException {
        Map properties = getArtifactProperties(artifact, version);
        String fileName = properties.get(FILE_NAME_PROPERTY).toString();

        return getDownloadDirectory(artifact, version).resolve(fileName);
    }

    @Override
    public Long getBinariesSize(Artifact artifact, String version) throws IOException, NumberFormatException {
        Map properties = getArtifactProperties(artifact, version);
        String size = properties.get(SIZE_PROPERTY).toString();

        return Long.valueOf(size);
    }

    @Override
    public Map<Artifact, SortedMap<Version, Path>> getDownloadedArtifacts() throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloaded = new LinkedHashMap<>(artifacts.size());

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

    private Path getDownloadDirectory(Artifact artifact, String version) {
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

    protected Map getArtifactProperties(Artifact artifact) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + artifact.getName());
        Map m;
        try {
            m = asMap(transport.doGet(requestUrl));
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        validateArtifactProperties(m);
        return m;
    }

    protected Map getArtifactProperties(Artifact artifact, String version) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + artifact.getName() + "/" + version);
        Map m;
        try {
            m = asMap(transport.doGet(requestUrl));
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        validateArtifactProperties(m);
        return m;
    }

    protected void validateArtifactProperties(Map m) throws IOException {
        if (m == null) {
            throw new IOException("Can't get artifact properties.");
        }

        for (String p : ArtifactProperties.PUBLIC_PROPERTIES) {
            if (!m.containsKey(p)) {
                throw new IOException("Can't get artifact property: " + p);
            }
        }
    }

    protected void storeProperty(String property, String value) throws IOException {
        Path conf = Paths.get(System.getenv("CODENVY_CONF"), "im.properties");

        Properties props = new Properties();
        if (exists(conf)) {
            try (InputStream in = newInputStream(conf)) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new IOException("Can't store property into configuration");
                }
            }
        }

        props.put(property, value);
        try (OutputStream out = newOutputStream(conf)) {
            props.store(out, null);
        }
    }
}
