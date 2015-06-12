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
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.InjectorBootstrap;
import com.codenvy.im.utils.Version;

import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.artifacts.ArtifactProperties.PREVIOUS_VERSION_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.Commons.asMap;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.nio.file.Files.newInputStream;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractArtifact implements Artifact {
    private final   String        name;
    protected final HttpTransport transport;
    protected final ConfigManager configManager;
    protected final String        updateEndpoint;
    private final Path            downloadDir;

    public AbstractArtifact(String name,
                            String updateEndpoint,
                            String downloadDir,
                            HttpTransport transport,
                            ConfigManager configManager) {
        this.downloadDir = Paths.get(downloadDir);
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

        String allowedPreviousVersions = getProperty(versionToInstall, PREVIOUS_VERSION_PROPERTY);
        if (allowedPreviousVersions != null) {
            return installedVersion.isSuitedFor(allowedPreviousVersions);
        }

        return installedVersion.compareTo(versionToInstall) < 0;
    }

    /** {@inheritDoc} */
    @Nullable
    public Version getLatestInstallableVersion() throws IOException {
        Version installedVersion = getInstalledVersion();
        if (installedVersion == null) {
            String versionNumber = getLatestVersionProperty(VERSION_PROPERTY);
            return versionNumber != null ? Version.valueOf(versionNumber) : null;
        }

        Version ver2Install = null;

        Collection<Map.Entry<Artifact, Version>> allUpdates = getAllUpdates();

        for (Map.Entry<Artifact, Version> entry : allUpdates) {
            Version version2Check = entry.getValue();

            String prevAllowedVersionNumber = getProperty(version2Check, PREVIOUS_VERSION_PROPERTY);
            if (installedVersion.isSuitedFor(prevAllowedVersionNumber)) {
                if (ver2Install == null || ver2Install.compareTo(version2Check) < 0) {
                    ver2Install = version2Check;
                }
            }
        }

        return ver2Install;
    }

    protected Collection<Map.Entry<Artifact, Version>> getAllUpdates() throws IOException {
        DownloadManager downloadManager = InjectorBootstrap.INJECTOR.getInstance(DownloadManager.class);
        return downloadManager.getAllUpdates(this);
    }


    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties(Version version) throws IOException {
//  TODO [ndp] comment to don't read properties from the local file within the downloading updates directory so as label field of artifact properties is mutable
//        Map<String, String> propertiesMap = fetchPropertiesFromLocalFile(version);
//        if (propertiesMap != null) {
//            return propertiesMap;
//        }

        return fetchPropertiesFromUpdateServer(version);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getProperty(Version version, String name) throws IOException {
        Map<String, String> properties = getProperties(version);
        return properties.get(name);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public String getLatestVersionProperty(String name) throws IOException {
        Map<String, String> properties = fetchPropertiesFromUpdateServer(null);
        return properties.get(name);
    }

    /**
     * Try to load properties from directory with binaries.
     * @return null if there is no properties file in the update directory, or there was an exception.
     */
    @Nullable
    protected Map<String, String> fetchPropertiesFromLocalFile(Version version) {
        Path pathToProperties = getDownloadDirectory(version).resolve(Artifact.ARTIFACT_PROPERTIES_FILE_NAME);
        if (!Files.exists(pathToProperties)) {
            return null;
        }

        try (InputStream in = newInputStream(pathToProperties)) {
            Properties properties = new Properties();
            properties.load(in);

            Map<String, String> propertiesMap = new HashMap<>();

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();

                propertiesMap.put(key, value);
            }

            return propertiesMap;
        } catch (IOException e) {
            return null;
        }
    }


    /**
     * If version is not specified then the properties of the latest version will be retrieved
     */
    private Map<String, String> fetchPropertiesFromUpdateServer(@Nullable Version version) throws IOException {
        String requestUrl = combinePaths(updateEndpoint, "repository/properties", getName() + (version != null ? "/" + version.toString()
                                                                                                               : ""));
        try {
            Map m = asMap(transport.doGet(requestUrl));
            return Collections.checkedMap(m, String.class, String.class);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    private Path getDownloadDirectory(Version version) {
        return downloadDir.resolve(getName()).resolve(version.toString());
    }
}
