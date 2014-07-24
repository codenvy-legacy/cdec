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
package com.codenvy.cdec.update;

import com.codenvy.cdec.utils.VersionUtil;
import com.google.common.io.InputSupplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;

import static com.codenvy.cdec.utils.VersionUtil.compare;
import static com.codenvy.cdec.utils.VersionUtil.parse;
import static com.google.common.io.Files.copy;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class ArtifactHandler {

    public static final String ARTIFACT_PROPERTIES = ".properties";

    public static final String VERSION_PROPERTY    = "version";
    public static final String REVISION_PROPERTY   = "revision";
    public static final String BUILD_TIME_PROPERTY = "build-time";
    public static final String FILE_NAME_PROPERTY  = "file";

    private final String repositoryDir;

    @Inject
    public ArtifactHandler(@Named("codenvy.update-server.repository") String repositoryDir) throws IOException {
        this.repositoryDir = repositoryDir;
        Files.createDirectories(Paths.get(repositoryDir));
    }

    /**
     * @return the latest available version of the artifact
     * @throws java.io.IOException
     */
    public String getLastVersion(String artifact) throws IOException {
        VersionUtil.Version lastVersion = null;

        Path dir = getDirectory(artifact);
        if (!Files.exists(dir)) {
            throw new ArtifactNotFoundException(artifact);
        }

        Iterator<Path> pathIterator = Files.newDirectoryStream(dir).iterator();
        if (!pathIterator.hasNext()) {
            throw new ArtifactNotFoundException(artifact);
        }

        while (pathIterator.hasNext()) {
            try {
                VersionUtil.Version version = parse(pathIterator.next().getFileName().toString());
                if (lastVersion == null || compare(version, lastVersion) > 0) {
                    lastVersion = version;
                }
            } catch (IllegalArgumentException e) {
                // maybe it isn't a version directory
            }
        }

        if (lastVersion == null) {
            throw new ArtifactNotFoundException(artifact);
        }

        return lastVersion.toString();
    }

    /**
     * Uploads artifact into the repository.
     */
    public void upload(final InputStream in, String artifact, String version, String fileName, Properties props) throws IOException {
        props.put(FILE_NAME_PROPERTY, fileName);
        props.put(VERSION_PROPERTY, version);
        storeProperties(artifact, version, props);

        copy(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return new BufferedInputStream(in);
            }
        }, getArtifact(fileName, artifact, version).toFile());
    }

    /**
     * Downloads artifact from the repository.
     */
    public InputStream download(String artifact, String version) throws IOException {
        String fileName = getFileName(artifact, version);
        if (fileName == null) {
            throw new ArtifactNotFoundException(artifact, version);
        }

        Path path = getArtifact(fileName, artifact, version);
        if (!Files.exists(path)) {
            throw new ArtifactNotFoundException(artifact, version);
        }

        return new BufferedInputStream(Files.newInputStream(path));
    }

    /**
     * @return true if artifact exists in the repository, otherwise method returns false
     */
    public boolean exists(String artifact, String version) {
        try {
            Path path = getArtifact(getFileName(artifact, version), artifact, version);
            return Files.exists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Loads the properties of the artifact.
     */
    public Properties loadProperties(String artifact, String version) throws IOException {
        Properties props = new Properties();
        Path propertiesFile = getPropertiesFile(artifact, version);
        if (!Files.exists(propertiesFile)) {
            throw new ArtifactNotFoundException(artifact, version);
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(propertiesFile))) {
            props.load(in);
        }

        return props;
    }

    protected String getFileName(String artifact, String version) throws IOException {
        return (String)loadProperties(artifact, version).get(FILE_NAME_PROPERTY);
    }

    /**
     * Stores the properties of the artifact.
     */
    public void storeProperties(String artifact, String version, Properties props) throws IOException {
        Path propertiesFile = getPropertiesFile(artifact, version);
        Path dir = propertiesFile.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(propertiesFile))) {
            props.store(out, null);
        }
    }

    protected Path getArtifact(@Nullable String fileName, String artifact, String version) throws IOException {
        if (fileName == null) {
            throw new IOException("Unknown file name for artifact '" + artifact + "' of version '" + version + "'");
        }
        return Paths.get(repositoryDir, artifact, version, fileName);
    }

    protected Path getDirectory() {
        return Paths.get(repositoryDir);
    }

    protected Path getDirectory(String artifact) {
        return Paths.get(repositoryDir, artifact);
    }

    protected Path getDirectory(String artifact, String version) {
        return Paths.get(repositoryDir, artifact, version);
    }

    protected Path getPropertiesFile(String artifact, String version) {
        return getDirectory(artifact, version).resolve(ARTIFACT_PROPERTIES);
    }
}
