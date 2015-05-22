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
package com.codenvy.im.artifacts;

import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.MD5_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.PREVIOUS_VERSION_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.Commons.asMap;
import static com.codenvy.im.utils.Commons.calculateMD5Sum;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractArtifact implements Artifact {
    private final   String        name;
    protected final HttpTransport transport;
    protected final ConfigManager configManager;
    protected final String        updateEndpoint;

    public AbstractArtifact(String name,
                            String updateEndpoint,
                            HttpTransport transport,
                            ConfigManager configManager) {
        this.name = name;
        this.transport = transport;
        this.configManager = configManager;
        this.updateEndpoint = updateEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractArtifact)) return false;

        AbstractArtifact that = (AbstractArtifact)o;

        return name.equals(that.name);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Artifact o) {
        return getPriority() - o.getPriority();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstallable(Version versionToInstall) throws IOException {
        Version installedVersion = getInstalledVersion();

        if (installedVersion == null) {  // check if there is installed version of artifact
            return true;
        }

        Version previousVersion = getAllowedPreviousVersion(versionToInstall);
        if (previousVersion != null) {
            return previousVersion.equals(installedVersion);
        }

        return installedVersion.compareTo(versionToInstall) < 0;
    }

    private Version getAllowedPreviousVersion(Version versionToInstall) throws IOException {
        Map<String, String> properties = getProperties(versionToInstall);
        if (properties.containsKey(PREVIOUS_VERSION_PROPERTY)) {
            return Version.valueOf(properties.get(PREVIOUS_VERSION_PROPERTY));
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Version getLatestInstallableVersion() throws IOException {
        Version version = getLatestVersion(updateEndpoint, transport);

        if (version != null && isInstallable(version)) {
            return version;
        } else {
            return null;
        }
    }

    /** @return the list of downloaded list */
    @Override
    public SortedMap<Version, Path> getDownloadedVersions(Path downloadDir) throws IOException {
        SortedMap<Version, Path> versions = new TreeMap<>(new Version.ReverseOrder());

        Path artifactDir = downloadDir.resolve(getName());

        if (exists(artifactDir)) {
            try (DirectoryStream<Path> paths = newDirectoryStream(artifactDir)) {
                Iterator<Path> pathIterator = paths.iterator();

                while (pathIterator.hasNext()) {
                    try {
                        Path versionDir = pathIterator.next();
                        if (isDirectory(versionDir)) {
                            Version version = valueOf(versionDir.getFileName().toString());

                            Map properties = getProperties(version);
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

    protected Map getLatestVersionProperties(String updateEndpoint, HttpTransport transport) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + getName());
        Map m;
        try {
            m = asMap(transport.doGet(requestUrl));
        } catch (JsonParseException e) {
            throw new IOException(e);
        }

        return m;
    }

    /** @return artifact properties */
    @Override
    public Map<String, String> getProperties(Version version) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties/" + getName() + "/" + version.toString());
        try {
            Map m = asMap(transport.doGet(requestUrl));
            return Collections.checkedMap(m, String.class, String.class);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    protected Version getLatestVersion(String updateEndpoint, HttpTransport transport) throws IOException {
        Map m = getLatestVersionProperties(updateEndpoint, transport);
        return valueOf(m.get(VERSION_PROPERTY).toString());
    }
}
