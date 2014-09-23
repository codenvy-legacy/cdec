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
package com.codenvy.im.artifacts;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractArtifact implements Artifact {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractArtifact.class);

    private final String name;

    public AbstractArtifact(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractArtifact)) return false;

        AbstractArtifact that = (AbstractArtifact)o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Artifact o) {
        return getPriority() - o.getPriority();
    }

    public void unpack(Path pathToBinaries, Path unpackToDir) throws IOException, URISyntaxException {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(
                        new BufferedInputStream(new FileInputStream(pathToBinaries.toFile()))))) {
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarIn.getNextTarEntry()) != null) {
                Path destPath = unpackToDir.resolve(tarEntry.getName());
                if (tarEntry.isDirectory()) {
                    if (!Files.exists(destPath.getParent())) {
                        Files.createDirectories(destPath);
                    }
                } else {
                    if (!Files.exists(destPath.getParent())) {
                        Files.createDirectories(destPath.getParent());
                    }

                    try (FileOutputStream out = new FileOutputStream(destPath.toFile())) {
                        IOUtils.copy(tarIn, out);
                        Files.setLastModifiedTime(destPath, FileTime.fromMillis(tarEntry.getModTime().getTime()));
                    }
                }
            }
        }

        LOG.info("Unpacked " + pathToBinaries + " into " + unpackToDir);
    }

    /**
     * @return path where artifact located
     */
    protected abstract Path getInstalledPath() throws URISyntaxException;
}
