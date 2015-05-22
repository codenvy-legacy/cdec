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
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SIZE_PROPERTY;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.getProperException;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class DownloadManager {

    private final String        updateEndpoint;
    private final HttpTransport transport;
    private final Path          downloadDir;
    private final Set<Artifact> artifacts;

    @Inject
    public DownloadManager(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                           @Named("installation-manager.download_dir") String downloadDir,
                           HttpTransport transport,
                           Set<Artifact> artifacts) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
        this.downloadDir = Paths.get(downloadDir);
        this.artifacts = new Commons.ArtifactsSet(artifacts); // keep order
        checkRWPermissions(this.downloadDir);
    }

    /** Checks if FS has enough free space, for instance to download artifacts */
    public void checkEnoughDiskSpace(long requiredSize) throws IOException {
        long freeSpace = downloadDir.toFile().getFreeSpace();
        if (freeSpace < requiredSize) {
            throw new IOException(String.format("Not enough disk space. Required %d bytes but available only %d bytes", requiredSize, freeSpace));
        }
    }

    /** Checks connection to server is available */
    public void checkIfConnectionIsAvailable() throws IOException {
        transport.doGet(combinePaths(updateEndpoint, "repository/properties/" + InstallManagerArtifact.NAME));
    }

    /**
     * @return path to artifact into the local repository
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    public Path getPathToBinaries(Artifact artifact, Version version) throws IOException {
        Map properties = artifact.getProperties(version);
        String fileName = properties.get(FILE_NAME_PROPERTY).toString();

        return getDownloadDirectory(artifact, version).resolve(fileName);
    }

    /**
     * @return size in bytes of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    public Long getBinariesSize(Artifact artifact, Version version) throws IOException, NumberFormatException {
        Map properties = artifact.getProperties(version);
        String size = properties.get(SIZE_PROPERTY).toString();

        return Long.valueOf(size);
    }

    /**
     * @return downloaded artifacts from the local repository
     * @throws IOException
     *         if an I/O error occurs
     */
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

    /** Filters what need to download, either all updates or a specific one. */
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

    /**
     * @return the list of the artifacts to update.
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
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


    /**
     * Download the specific version of the artifact.
     *
     * @return path to downloaded artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     * @throws java.lang.IllegalStateException
     *         if the subscription is invalid or expired
     */
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

    /**
     * @return set of downloaded into local repository versions of artifact
     * @throws IOException
     *         if an I/O error occurs
     */
    public SortedMap<Version, Path> getDownloadedVersions(Artifact artifact) throws IOException {
        return artifact.getDownloadedVersions(downloadDir);
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

    private Path getDownloadDirectory(Artifact artifact, Version version) {
        return downloadDir.resolve(artifact.getName()).resolve(version.toString());
    }
}
