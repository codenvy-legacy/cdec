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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.Commons.asMap;
import static com.codenvy.im.utils.Commons.calculateMD5Sum;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Version.compare;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.*;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static org.apache.commons.io.IOUtils.copy;

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
        try (TarArchiveInputStream in = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(newInputStream(pathToBinaries))))) {

            TarArchiveEntry tarEntry;
            while ((tarEntry = in.getNextTarEntry()) != null) {
                Path destPath = unpackToDir.resolve(tarEntry.getName());

                if (tarEntry.isDirectory()) {
                    if (!Files.exists(destPath)) {
                        createDirectories(destPath);
                    }
                } else {
                    if (!Files.exists(destPath.getParent())) {
                        createDirectories(destPath.getParent());
                    }

                    try (BufferedOutputStream out = new BufferedOutputStream(newOutputStream(destPath))) {
                        copy(in, out);
                        setLastModifiedTime(destPath, FileTime.fromMillis(tarEntry.getModTime().getTime()));
                    }
                }
            }
        }

        LOG.info("Unpacked " + pathToBinaries + " into " + unpackToDir);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstallable(Version versionToInstall, String accessToken) throws IOException {
        Version installedVersion = getInstalledVersion(accessToken);
        if (installedVersion == null) {
            return true;
        }

        return installedVersion.compareTo(versionToInstall) < 0;
    }

    /** @return path where artifact located */
    protected abstract Path getInstalledPath() throws URISyntaxException;

    @Override
    @Nullable
    public Version getLatestInstallableVersionToDownload(String authToken, String updateEndpoint, HttpTransport transport) throws IOException {
        Version latestVersionToDownload = getLatestVersionToDownload(updateEndpoint, transport);

        if (latestVersionToDownload == null || isInstallable(latestVersionToDownload, authToken)) {
            return latestVersionToDownload;
        }

        return null;
    }

    @Override
    public SortedMap<Version, Path> getDownloadedVersions(Path downloadDir, String updateEndpoint, HttpTransport transport) throws IOException {
        SortedMap<Version, Path> versions = new TreeMap<>();

        Path artifactDir = downloadDir.resolve(getName());

        if (exists(artifactDir)) {
            Iterator<Path> pathIterator = Files.newDirectoryStream(artifactDir).iterator();

            while (pathIterator.hasNext()) {
                try {
                    Path versionDir = pathIterator.next();
                    if (isDirectory(versionDir)) {
                        Version version = valueOf(versionDir.getFileName().toString());

                        Map properties = getProperties(version, updateEndpoint, transport);
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
        }

        return versions;
    }

    Map getLatestVersionProperties(String updateEndpoint, HttpTransport transport) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + getName());
        Map m;
        try {
            m = asMap(transport.doGet(requestUrl));
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        validateProperties(m);
        return m;
    }

    @Override
    public Map getProperties(Version version, String updateEndpoint, HttpTransport transport) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + getName() + "/" + version.toString());
        Map m;
        try {
            m = asMap(transport.doGet(requestUrl));
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        validateProperties(m);
        return m;
    }

    @Override
    public void validateProperties(Map properties) throws IOException {
        if (properties == null) {
            throw new IOException("Can't get artifact properties.");
        }

        for (String p : ArtifactProperties.PUBLIC_PROPERTIES) {
            if (!properties.containsKey(p)) {
                throw new IOException("Can't get artifact property: " + p);
            }
        }
    }

    protected Version getLatestVersionToDownload(String updateEndpoint, HttpTransport transport) throws IOException {
        Map m = getLatestVersionProperties(updateEndpoint, transport);
        return valueOf(m.get(VERSION_PROPERTY).toString());
    }
}
