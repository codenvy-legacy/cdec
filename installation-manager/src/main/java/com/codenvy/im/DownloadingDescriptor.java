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
import com.codenvy.im.restlet.InstallationManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;

/**
 * @author Alexander Reshetnyak
 */
public class DownloadingDescriptor {

    private final Map<Path, Long>         artifacts;
    private final long                    totalSize;
    private final AtomicReference<String> downloadResult;

    public DownloadingDescriptor(Map<Path, Long> artifacts) {
        this.artifacts = artifacts;
        this.downloadResult = new AtomicReference<>();

        long tSize = 0;
        for (Long l : this.artifacts.values()) {
            tSize += l;
        }
        this.totalSize = tSize;
    }

    /**
     * Get total size in bytes of artifacts these will downloaded.
     *
     * @return long
     */
    public long getTotalSize() { return totalSize; }

    /**
     *  Get downloaded bytes of artifacts.
     *
     * @return long
     * @throws IOException
     */
    public long getDownloadedSize() throws IOException {
        long downloadedSize = 0;
        for (Path path : artifacts.keySet()) {
            if (exists(path)) {
                downloadedSize += size(path);
            }
        }

        return downloadedSize;
    }


    /**
     * Create DownloadingDescriptor for specific artifact.
     *
     * @param artifact
     *          Artifact
     * @param version
     *          String
     * @param manager
     *          InstallationManager
     * @return DownloadingDescriptor
     * @throws IOException
     */
    public static DownloadingDescriptor valueOf(Artifact artifact, String version, InstallationManager manager) throws IOException {
        Map<Path, Long> m = new LinkedHashMap<>();
        m.put(manager.getLocalPath(artifact, version), manager.getSize(artifact, version));

        return new DownloadingDescriptor(m);
    }

    /**
     * Create DownloadingDescriptor for specific artifact.
     *
     * @param artifacts
     *          Map with artifacts.
     * @param manager
     *          InstallationManager
     * @return DownloadingDescriptor
     * @throws IOException
     */
    public static DownloadingDescriptor valueOf(Map<Artifact, String> artifacts, InstallationManager manager) throws IOException {
        Map<Path, Long> m = new LinkedHashMap<>();
        for (Map.Entry<Artifact, String> e : artifacts.entrySet()) {
            Artifact artifact = e.getKey();
            String version = e.getValue();

            m.put(manager.getLocalPath(artifact, version), manager.getSize(artifact, version));
        }

        return new DownloadingDescriptor(m);
    }

    /**
     * @return String
     *           The JSON with result of download.
     */
    @Nullable
    public String getDownloadResult() {
        return downloadResult.get();
    }

    /**
     * Set Result of download.
     *
     * @param downloadResult
     *          String, the JSON with result of download.
     */
    public void setDownloadResult(String downloadResult) {
        this.downloadResult.set(downloadResult);
    }
}
