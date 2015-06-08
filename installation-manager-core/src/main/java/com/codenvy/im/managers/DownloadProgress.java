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
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.response.DownloadArtifactInfo;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.utils.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;

/**
 * Contains current download progress.
 *
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
public class DownloadProgress {

    private final List<DownloadArtifactInfo> downloadedArtifacts;
    private final Map<Artifact, Version> artifacts2Download;
    private final Map<Path, Long>                         artifacts;
    private final AtomicReference<Exception>              exception;
    private final AtomicReference<DownloadArtifactStatus> status;
    private final String                 uuid;
    private final Thread                                  downloadThread;

    public DownloadProgress(Map<Path, Long> binaries, Map<Artifact, Version> artifacts) {
        this.downloadThread = Thread.currentThread();
        this.artifacts = new HashMap<>(binaries);
        this.status = new AtomicReference<>(DownloadArtifactStatus.DOWNLOADING);
        this.exception = new AtomicReference<>();
        this.downloadedArtifacts = new CopyOnWriteArrayList<>();
        this.artifacts2Download = new HashMap<>(artifacts);
        this.uuid = UUID.randomUUID().toString();
    }

    public void addDownloadedArtifact(DownloadArtifactInfo artifactInfo) throws ArtifactNotFoundException {
        downloadedArtifacts.add(artifactInfo);

        Iterator<Map.Entry<Artifact, Version>> iter = artifacts2Download.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Artifact, Version> e = iter.next();
            if (artifactInfo.getArtifact().equals(e.getKey().getName())
                && artifactInfo.getVersion().equals(e.getValue().toString())) {

                iter.remove();
            }
        }
    }

    public List<DownloadArtifactInfo> getDownloadedArtifacts() {
        return downloadedArtifacts;
    }

    /** @return the total size of all artifacts that will be downloaded */
    public long getExpectedSize() {
        long tSize = 0;
        for (long l : artifacts.values()) {
            tSize += l;
        }
        return tSize;
    }

    public int getProgress() throws IOException {
        return (int)Math.round((getDownloadedSize() * 100D / getExpectedSize()));
    }

    public long getDownloadedSize() throws IOException {
        long downloadedSize = 0;
        for (Path path : artifacts.keySet()) {
            if (exists(path)) {
                downloadedSize += size(path);
            }
        }
        return downloadedSize;
    }

    /** @return the downloading status. */
    public DownloadArtifactStatus getDownloadStatus() {
        return status.get();
    }

    @Nullable
    public String getErrorMessage() {
        return exception.get() == null ? null : exception.get().getMessage();
    }

    public void setDownloadStatus(DownloadArtifactStatus status) {
        this.status.set(status);
    }

    public void setDownloadStatus(DownloadArtifactStatus status, Exception exception) {
        this.status.set(status);
        this.exception.set(exception);
    }

    /** Indicates if downloading finished or didn't. */
    public boolean isDownloadingFinished() {
        return status.get() != DownloadArtifactStatus.DOWNLOADING;
    }

    /** Get thread which downloading artifacts. */
    public Thread getDownloadThread() {
        return downloadThread;
    }

    public String getUuid() {
        return uuid;
    }

    public Map<Artifact, Version> getArtifacts2Download() {
        return artifacts2Download;
    }
}
