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
import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.InjectorBootstrap;
import com.codenvy.im.utils.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static com.codenvy.im.utils.Version.valueOf;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractArtifact implements Artifact {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractArtifact.class);

    private final String        name;
    private final ConfigFactory configFactory;

    public AbstractArtifact(String name) {
        this.name = name;
        this.configFactory = InjectorBootstrap.INJECTOR.getInstance(ConfigFactory.class);
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
    public boolean isInstallable(Version versionToInstall, String accessToken) throws IOException {
        Version installedVersion = getInstalledVersion(accessToken);
        return installedVersion == null || installedVersion.compareTo(versionToInstall) < 0;

    }

    /** Initialize SSH agent */
//    protected Agent initSshAgent() throws IOException {
//        AgentConfig config = configFactory.loadOrCreateAgentConfig();
//        return new SecureShellAgent(config.getHost(),
//                                    config.getPort(),
//                                    config.getUser(),
//                                    config.getPrivateKeyFileAbsolutePath(),
//                                    null
//        );
//    }

    /** Initialize local agent */
    protected Agent initLocalAgent() throws IOException {
        return new LocalAgent();
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

    /** @return the list of downloaded list */
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
                } catch (IllegalArgumentException e) {
                    // maybe it isn't a version directory
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

        validateProperties(m);
        return m;
    }

    /** @return artifact properties */
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

    protected void validateProperties(Map properties) throws IOException {
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
