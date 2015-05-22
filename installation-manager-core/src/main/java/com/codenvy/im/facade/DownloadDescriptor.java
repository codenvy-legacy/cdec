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
package com.codenvy.im.facade;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.response.Response;
import com.codenvy.im.utils.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** @author Alexander Reshetnyak */
public class DownloadDescriptor {

    private final Map<Path, Long>           artifacts;
    private final long                      totalSize;
    private final AtomicReference<Response> downloadResult;
    private final Thread                    downloadThread;

    public DownloadDescriptor(Map<Path, Long> artifacts, Thread downloadThread) {
        this.downloadThread = downloadThread;
        this.artifacts = new ConcurrentHashMap<>(artifacts);
        this.downloadResult = new AtomicReference<>();

        long tSize = 0;
        for (long l : this.artifacts.values()) {
            tSize += l;
        }
        this.totalSize = tSize;
    }

    /** @return the total size of all artifacts that will be downloaded */
    public long getTotalSize() {
        return totalSize;
    }

    /** @return the downloads paths for artifacts */
    public Collection<Path> getArtifactPaths() {
        return artifacts.keySet();
    }

    /** Factory method */
    public static DownloadDescriptor createDescriptor(Map<Artifact, Version> artifacts,
                                                      DownloadManager downloadManager,
                                                      Thread downloadThread) throws IOException {
        Map<Path, Long> m = new LinkedHashMap<>();

        for (Map.Entry<Artifact, Version> e : artifacts.entrySet()) {
            Artifact artifact = e.getKey();
            Version version = e.getValue();

            Path pathToBinaries = downloadManager.getPathToBinaries(artifact, version);
            Long binariesSize = downloadManager.getBinariesSize(artifact, version);

            m.put(pathToBinaries, binariesSize);
        }

        return new DownloadDescriptor(m, downloadThread);
    }

    /** @return the downloading status. */
    @Nullable
    public Response getDownloadResult() {
        return downloadResult.get();
    }

    /** Sets the download status. */
    public void setDownloadResult(Response downloadResult) {
        if (isDownloadingFinished()) {
            throw new IllegalStateException("Impossible to set download result twice.");
        }

        this.downloadResult.set(downloadResult);
    }

    /** Indicates if downloading finished or didn't. */
    public boolean isDownloadingFinished() {
        return downloadResult.get() != null;
    }

    /** Get thread which downloading artifacts. */
    public Thread getDownloadThread() {
        return downloadThread;
    }
}
