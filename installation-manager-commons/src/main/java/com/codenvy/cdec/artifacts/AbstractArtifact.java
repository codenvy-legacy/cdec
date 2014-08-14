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
package com.codenvy.cdec.artifacts;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    protected void unpack(Path pathToBinaries) throws IOException, URISyntaxException {
        Path installedPath = getInstalledPath();

        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(pathToBinaries))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                FileUtils.copyInputStreamToFile(in, installedPath.resolve(entry.getName()).toFile());
            }
        }

        LOG.info("Unpacked " + pathToBinaries + " into " + installedPath);
    }

    /**
     * @return path where artifact located
     */
    protected abstract Path getInstalledPath() throws URISyntaxException;
}
