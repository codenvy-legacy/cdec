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
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.response.DownloadProgressDescriptor;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SIZE_PROPERTY;
import static com.codenvy.im.utils.Commons.calculateMD5Sum;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.getProperException;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class DownloadManager {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadManager.class);

    private final String        updateEndpoint;
    private final HttpTransport transport;
    private final Path          downloadDir;
    private final Set<Artifact> artifacts;

    private DownloadProgress downloadProgress;

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

    /** Starts downloading */
    public void startDownload(@Nullable final Artifact artifact, @Nullable final Version version)
            throws IOException,
                   InterruptedException,
                   DownloadAlreadyStartedException {

        validateIfDownloadInProgress();
        validateIfConnectionAvailable();

        final CountDownLatch latcher = new CountDownLatch(1);

        Thread downloadThread = new Thread("download thread") {
            public void run() {
                download(artifact, version, latcher);
            }
        };
        downloadThread.setDaemon(true);
        downloadThread.start();

        latcher.await();
    }

    private void validateIfDownloadInProgress() throws DownloadAlreadyStartedException {
        if (downloadProgress != null && !downloadProgress.isDownloadingFinished()) {
            throw new DownloadAlreadyStartedException();
        }
    }

    /** Interrupts downloading */
    public void stopDownload() throws DownloadNotStartedException, InterruptedException {
        try {
            if (downloadProgress == null || downloadProgress.isDownloadingFinished()) {
                throw new DownloadNotStartedException();
            }

            downloadProgress.getDownloadThread().interrupt();
            downloadProgress.getDownloadThread().join();
        } finally {
            invalidateDownloadDescriptor();
        }
    }

    /** @return the current status of downloading process */
    public DownloadProgressDescriptor getDownloadProgress() throws DownloadNotStartedException, IOException {
        if (downloadProgress == null) {
            throw new DownloadNotStartedException();
        }

        if (downloadProgress.getDownloadStatus() == DownloadArtifactStatus.FAILED) {
            return new DownloadProgressDescriptor(downloadProgress.getDownloadStatus(),
                                                  downloadProgress.getErrorMessage(),
                                                  downloadProgress.getProgress(),
                                                  downloadProgress.getDownloadedArtifacts());
        }

        return new DownloadProgressDescriptor(downloadProgress.getDownloadStatus(),
                                              downloadProgress.getProgress(),
                                              downloadProgress.getDownloadedArtifacts());
    }

    protected void download(@Nullable Artifact artifact,
                            @Nullable Version version,
                            CountDownLatch latcher) {
        invalidateDownloadDescriptor();

        try {
            Map<Artifact, Version> updatesToDownload = getLatestUpdatesToDownload(artifact, version);

            createDownloadDescriptor(updatesToDownload);
            checkEnoughDiskSpace(downloadProgress.getExpectedSize());

            latcher.countDown();

            for (Map.Entry<Artifact, Version> e : updatesToDownload.entrySet()) {
                Artifact artToDownload = e.getKey();
                Version verToDownload = e.getValue();

                try {
                    Path pathToBinaries = download(artToDownload, verToDownload);
                    DownloadArtifactInfo downloadArtifactDesc = new DownloadArtifactInfo(artToDownload,
                                                                                                     verToDownload,
                                                                                                     pathToBinaries,
                                                                                                     DownloadArtifactStatus.DOWNLOADED);
                    downloadProgress.addDownloadedArtifact(downloadArtifactDesc);
                } catch (Exception exp) {
                    LOG.error(exp.getMessage(), exp);
                    DownloadArtifactInfo downloadArtifactDesc = new DownloadArtifactInfo(artToDownload,
                                                                                                     verToDownload,
                                                                                                     DownloadArtifactStatus.FAILED);
                    downloadProgress.addDownloadedArtifact(downloadArtifactDesc);
                    downloadProgress.setDownloadStatus(DownloadArtifactStatus.FAILED, exp);
                    return;
                }
            }

            downloadProgress.setDownloadStatus(DownloadArtifactStatus.DOWNLOADED);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);

            if (downloadProgress == null) {
                downloadProgress = new DownloadProgress(Collections.<Path, Long>emptyMap());
            }
            downloadProgress.setDownloadStatus(DownloadArtifactStatus.FAILED, e);

            if (latcher.getCount() == 1) {
                latcher.countDown();
            }
        }
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
    protected Path download(Artifact artifact, Version version) throws IOException, IllegalStateException {
        try {
            String requestUrl = combinePaths(updateEndpoint, "/repository/public/download/" + artifact.getName() + "/" + version);

            Path artifactDownloadDir = getDownloadDirectory(artifact, version);
            deleteDirectory(artifactDownloadDir.toFile());

            return transport.download(requestUrl, artifactDownloadDir);
        } catch (IOException e) {
            throw getProperException(e, artifact);
        }
    }

    /** Checks if FS has enough free space, for instance to download artifacts */
    protected void checkEnoughDiskSpace(long requiredSize) throws IOException {
        long freeSpace = downloadDir.toFile().getFreeSpace();
        if (freeSpace < requiredSize) {
            throw new IOException(String.format("Not enough disk space. Required %d bytes but available only %d bytes", requiredSize, freeSpace));
        }
    }

    /** Checks connection to server is available */
    public void validateIfConnectionAvailable() throws IOException {
        transport.doGet(combinePaths(updateEndpoint, "repository/properties/" + InstallManagerArtifact.NAME));
    }

    /**
     * @return path to artifact into the local repository
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    protected Path getPathToBinaries(Artifact artifact, Version version) throws IOException {
        Map properties = artifact.getProperties(version);
        String fileName = properties.get(FILE_NAME_PROPERTY).toString();

        return getDownloadDirectory(artifact, version).resolve(fileName);
    }

    /**
     * @return size in bytes of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    protected Long getBinariesSize(Artifact artifact, Version version) throws IOException, NumberFormatException {
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
            SortedMap<Version, Path> versions = getDownloadedVersions(artifact);

            if (!versions.isEmpty()) {
                downloaded.put(artifact, versions);
            }
        }

        return downloaded;
    }

    /**
     * Determines the latest versions to download taking into account if artifacts have been already downloaded.
     */
    public Map<Artifact, Version> getLatestUpdatesToDownload(@Nullable Artifact artifact, @Nullable Version version) throws IOException {
        if (artifact == null) { // all artifacts the latest versions
            Map<Artifact, Version> latestUpdates = getUpdates();
            Map<Artifact, Version> artifact2Downloads = new TreeMap<>(latestUpdates);

            for (Map.Entry<Artifact, Version> e : latestUpdates.entrySet()) {
                Artifact eachArtifact = e.getKey();
                Version eachVersion = e.getValue();

                if (getDownloadedVersions(eachArtifact).containsKey(eachVersion)) {
                    artifact2Downloads.remove(eachArtifact);
                }
            }

            return artifact2Downloads;
        } else {
            if (version == null) {
                version = artifact.getLatestInstallableVersion();
                if (version == null) {
                    return Collections.emptyMap(); // nothing to download
                }
            }

            if (getDownloadedVersions(artifact).containsKey(version)) {
                return Collections.emptyMap();
            }

            return ImmutableMap.of(artifact, version);
        }
    }

    /**
     * Gets a list of artifacts to download.
     * Only newer versions if the installed artifacts will be returned.
     *
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

    private Path getDownloadDirectory(Artifact artifact) {
        return downloadDir.resolve(artifact.getName());
    }

    /**
     * @return sorted set of the downloaded version of the given artifact
     * @throws IOException
     *         if an I/O error occurs
     */
    public SortedMap<Version, Path> getDownloadedVersions(Artifact artifact) throws IOException {
        SortedMap<Version, Path> versions = new TreeMap<>(new Version.ReverseOrder());

        Path artifactDir = getDownloadDirectory(artifact);

        if (exists(artifactDir)) {
            try (DirectoryStream<Path> paths = newDirectoryStream(artifactDir)) {

                for (Path versionDir : paths) {
                    try {
                        if (isDirectory(versionDir)) {
                            Version version = valueOf(versionDir.getFileName().toString());

                            Map properties = artifact.getProperties(version);
                            String md5sum = properties.get(MD5_PROPERTY).toString();
                            String fileName = properties.get(FILE_NAME_PROPERTY).toString();

                            Path file = versionDir.resolve(fileName);
                            if (exists(file) && md5sum.equals(calculateMD5Sum(file))) {
                                versions.put(version, file);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // maybe it isn't a version directory
                    }
                }
            }
        }

        return versions;
    }

    /** Factory method */
    protected void createDownloadDescriptor(Map<Artifact, Version> artifacts) throws IOException {
        Map<Path, Long> m = new LinkedHashMap<>();

        for (Map.Entry<Artifact, Version> e : artifacts.entrySet()) {
            Artifact artifact = e.getKey();
            Version version = e.getValue();

            Path pathToBinaries = getPathToBinaries(artifact, version);
            Long binariesSize = getBinariesSize(artifact, version);

            m.put(pathToBinaries, binariesSize);
        }

        downloadProgress = new DownloadProgress(m);
    }

    protected void invalidateDownloadDescriptor() {
        downloadProgress = null;
    }
}
