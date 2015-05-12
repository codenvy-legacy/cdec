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
package com.codenvy.im.managers;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SIZE_PROPERTY;
import static com.codenvy.im.utils.Commons.ArtifactsSet;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.extractServerUrl;
import static com.codenvy.im.utils.Commons.getProperException;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager {
    public static final String STORAGE_FILE_NAME = "config.properties";

    private final String           updateEndpoint;
    private final InstallerManager installerManager;
    private final HttpTransport    transport;
    private final Set<Artifact>    artifacts;
    private final NodeManager      nodeManager;
    private final BackupManager    backupManager;
    private final PasswordManager  passwordManager;
    private final Path             downloadDir;
    private final Path storageDir;


    @Inject
    public InstallationManagerImpl(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                                   @Named("installation-manager.download_dir") String downloadDir,
                                   @Named("installation-manager.storage_dir") String storageDir,
                                   HttpTransport transport,
                                   InstallerManager installerManager,
                                   Set<Artifact> artifacts,
                                   NodeManager nodeManager,
                                   BackupManager backupManager,
                                   PasswordManager passwordManager) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
        this.installerManager = installerManager;
        this.passwordManager = passwordManager;
        this.artifacts = new ArtifactsSet(artifacts); // keep order

        this.storageDir = Paths.get(storageDir);
        this.downloadDir = Paths.get(downloadDir);
        checkRWPermissions(this.downloadDir);

        this.nodeManager = nodeManager;
        this.backupManager = backupManager;
    }

    private void checkRWPermissions(Path downloadDir) throws IOException {
        try {
            createDirectories(downloadDir);

            Path tmp = downloadDir.resolve("tmp.tmp");
            createFile(tmp);
            delete(tmp);
        } catch (IOException e) {
            throw new IOException("Installation Manager probably doesn't have r/w permissions to " + downloadDir.toString(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(Artifact artifact, Version version, InstallOptions options) throws IOException {
        return installerManager.getInstallInfo(artifact, version, options);
    }

    /** {@inheritDoc} */
    @Override
    public void install(String authToken, Artifact artifact, Version version, InstallOptions options) throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact) && downloadedArtifacts.get(artifact).containsKey(version)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact).get(version);
            if (pathToBinaries == null) {
                throw new IOException(String.format("Binaries for artifact '%s' version '%s' not found", artifact, version));
            }

            if (options.getStep() != 0 || artifact.isInstallable(version)) {
                installerManager.install(artifact, version, pathToBinaries, options);
            } else {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version + "'.");
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact '" + artifact.getName() + "' version '" + version + "' not found");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, Version> getInstalledArtifacts() throws IOException {
        Map<Artifact, Version> installed = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                Version installedVersion = artifact.getInstalledVersion();
                if (installedVersion != null) {
                    installed.put(artifact, installedVersion);
                }
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** {@inheritDoc} */
    @Override
    public Path download(Artifact artifact, Version version) throws IOException, IllegalStateException {
        try {
            String requestUrl = combinePaths(updateEndpoint, "/repository/public/download/" + artifact.getName() + "/" + version);

            Path artifactDownloadDir = getDownloadDirectory(artifact, version);
            deleteDirectory(artifactDownloadDir.toFile());

            return transport.download(requestUrl, artifactDownloadDir);
        } catch (IOException e) {
            throw getProperException(e, artifact);
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
    public LinkedHashMap<String, String> getConfig() {
        return new LinkedHashMap<String, String>() {{
            put("download directory", downloadDir.toString());
            put("base url", extractServerUrl(updateEndpoint));
        }};
    }

    protected void validatePath(Path newDownloadDir) throws IOException {
        if (!newDownloadDir.isAbsolute()) {
            throw new IOException("Path must be absolute.");
        }
    }

    @Override
    public Path getPathToBinaries(Artifact artifact, Version version) throws IOException {
        Map properties = artifact.getProperties(version);
        String fileName = properties.get(FILE_NAME_PROPERTY).toString();

        return getDownloadDirectory(artifact, version).resolve(fileName);
    }

    @Override
    public Long getBinariesSize(Artifact artifact, Version version) throws IOException, NumberFormatException {
        Map properties = artifact.getProperties(version);
        String size = properties.get(SIZE_PROPERTY).toString();

        return Long.valueOf(size);
    }

    @Override
    public Map<Artifact, SortedMap<Version, Path>> getDownloadedArtifacts() throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloaded = new LinkedHashMap<>(artifacts.size());

        for (Artifact artifact : artifacts) {
            SortedMap<Version, Path> versions = artifact.getDownloadedVersions(downloadDir);

            if (!versions.isEmpty()) {
                downloaded.put(artifact, versions);
            }
        }

        return downloaded;
    }

    @Override
    public SortedMap<Version, Path> getDownloadedVersions(Artifact artifact) throws IOException {
        return artifact.getDownloadedVersions(downloadDir);
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, Version> getUpdates() throws IOException {
        Map<Artifact, Version> newVersions = new LinkedHashMap<>();

        Version newVersion;
        for (Artifact artifact : artifacts) {
            try {
                newVersion = artifact.getLatestInstallableVersion();
            } catch (HttpException e) {
                // ignore update server error when there is no version of certain artifact there
                if (e.getStatus() == javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode()) {
                    continue;
                }
                throw e;
            }

            if (newVersion != null) {
                newVersions.put(artifact, newVersion);
            }
        }

        return newVersions;
    }

    /** {@inheritDoc} */
    @Override
    public Version getLatestInstallableVersion(String authToken, Artifact artifact) throws IOException {
        return artifact.getLatestInstallableVersion();
    }

    private Path getDownloadDirectory(Artifact artifact, Version version) {
        return downloadDir.resolve(artifact.getName()).resolve(version.toString());
    }

    /** Filters what need to download, either all updates or a specific one. */
    @Override
    public Map<Artifact, Version> getUpdatesToDownload(Artifact artifact, Version version) throws IOException {
        if (artifact == null) {
            Map<Artifact, Version> latestVersions = getUpdates();
            Map<Artifact, Version> updates = new TreeMap<>(latestVersions);
            for (Map.Entry<Artifact, Version> e : latestVersions.entrySet()) {

                Artifact eachArtifact = e.getKey();
                Version eachVersion = e.getValue();
                if (eachArtifact.getDownloadedVersions(downloadDir).containsKey(eachVersion)) {
                    updates.remove(eachArtifact);
                }
            }

            return updates;
        } else {
            if (version != null) {
                // verify if version had been already downloaded
                if (artifact.getDownloadedVersions(downloadDir).containsKey(version)) {
                    return Collections.emptyMap();
                }

                return ImmutableMap.of(artifact, version);
            }

            final Version versionToUpdate = artifact.getLatestInstallableVersion();
            if (versionToUpdate == null) {
                return Collections.emptyMap();
            }

            // verify if version had been already downloaded
            if (artifact.getDownloadedVersions(downloadDir).containsKey(versionToUpdate)) {
                return Collections.emptyMap();
            }

            return ImmutableMap.of(artifact, versionToUpdate);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstallable(Artifact artifact, Version version) throws IOException {
        return artifact.isInstallable(version);
    }

    /** {@inheritDoc} */
    @Override
    public NodeConfig addNode(String dns) throws IOException, IllegalArgumentException {
        return nodeManager.add(dns);
    }

    /** {@inheritDoc} */
    @Override
    public NodeConfig removeNode(String dns) throws IOException, IllegalArgumentException {
        return nodeManager.remove(dns);
    }

    /** {@inheritDoc} */
    @Override
    public BackupConfig backup(BackupConfig config) throws IOException {
        return backupManager.backup(config);
    }

    /** {@inheritDoc} */
    @Override
    public void restore(BackupConfig config) throws IOException {
        backupManager.restore(config);
    }

    /** {@inheritDoc} */
    @Override
    public void changeAdminPassword(byte[] newPassword) throws IOException {
        passwordManager.changeAdminPassword(newPassword);
    }

    /** {@inheritDoc} */
    @Override
    public void storeProperties(Map<String, String> newProperties) throws IOException {
        Path storageFile = getStorageFile();
        Properties properties = loadProperties(storageFile);

        for (Map.Entry<String, String> entry : newProperties.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        try (OutputStream out = new BufferedOutputStream(newOutputStream(storageFile))) {
            properties.store(out, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> readProperties(final Collection<String> names) throws IOException {
        Path storageFile = getStorageFile();
        Properties properties = loadProperties(storageFile);

        Map<String, String> result = new HashMap<>((Map)properties);

        Iterator<Map.Entry<String, String>> iter = result.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (!names.contains(entry.getKey())) {
                iter.remove();
            }
        }

        return result;
    }

    private Properties loadProperties(Path propertiesFile) throws IOException {
        Properties properties = new Properties();

        if (exists(propertiesFile)) {
            try (InputStream in = new BufferedInputStream(newInputStream(propertiesFile))) {
                properties.load(in);
            }
        }
        return properties;
    }

    private Path getStorageFile() throws IOException {
        Path storageFile = storageDir.resolve(STORAGE_FILE_NAME);
        if (!exists(storageFile.getParent())) {
            createDirectories(storageFile.getParent());
        }

        return storageFile;
    }
}
