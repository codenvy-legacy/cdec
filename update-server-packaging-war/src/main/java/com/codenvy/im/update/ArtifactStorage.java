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
package com.codenvy.im.update;

import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.utils.Version;
import com.google.common.io.InputSupplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.TreeSet;

import static com.codenvy.im.artifacts.ArtifactProperties.ARTIFACT_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.VERSION_PROPERTY;
import static com.codenvy.im.utils.Commons.getVersionsList;
import static com.google.common.io.Files.copy;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class ArtifactStorage {

    public static final String PROPERTIES_FILE = ".properties";

    private final String repositoryDir;

    @Inject
    public ArtifactStorage(@Named("update-server.repository.path") String repositoryDir) throws IOException {
        this.repositoryDir = repositoryDir;
        Files.createDirectories(Paths.get(repositoryDir));
    }

    /**
     * @return the latest available version of the artifact in the repository
     * @throws com.codenvy.im.exceptions.ArtifactNotFoundException
     *         if artifact is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public String getLatestVersion(String artifact) throws IOException {
        Path dir = getArtifactDir(artifact);
        TreeSet<Version> versions = getVersionsList(dir);
        if (versions.isEmpty()) {
            throw ArtifactNotFoundException.from(artifact);
        }

        return versions.last().toString();
    }

    /**
     * Uploads artifact into the repository.
     *
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public void upload(final InputStream in, String artifact, String version, String fileName, Properties props) throws IOException {
        props.put(FILE_NAME_PROPERTY, fileName);
        props.put(VERSION_PROPERTY, version);
        props.put(ARTIFACT_PROPERTY, artifact);
        storeProperties(artifact, version, props);

        copy(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return new BufferedInputStream(in);
            }
        }, getArtifact(artifact, version, fileName).toFile());
    }

    /**
     * Downloads artifact from the repository.
     *
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws com.codenvy.im.exceptions.ArtifactNotFoundException
     *         if artifact is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public InputStream download(String artifact, String version) throws IOException {
        Path path = getArtifact(artifact, version);
        if (!Files.exists(path)) {
            throw new ArtifactNotFoundException(artifact, version);
        }

        return new BufferedInputStream(Files.newInputStream(path));
    }

    /**
     * Loads the properties of the artifact.
     *
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    public Properties loadProperties(String artifact, String version) throws IOException {
        Properties props = new Properties();
        Path propertiesFile = getPropertiesFile(artifact, version);
        if (!Files.exists(propertiesFile)) {
            throw new PropertiesNotFoundException(artifact, version);
        }

        try (InputStream in = new BufferedInputStream(Files.newInputStream(propertiesFile))) {
            props.load(in);
        }

        return props;
    }

    /**
     * Stores the properties of the artifact.
     *
     * @throws java.io.IOException
     *         if an I/O error occurs
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

    /**
     * Indicates if to download artifact user has to be authenticated.
     *
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    protected boolean isAuthenticationRequired(String artifact, String version) throws IOException {
        return "true".equalsIgnoreCase((String)loadProperties(artifact, version).get(AUTHENTICATION_REQUIRED_PROPERTY));
    }

    /**
     * @return the subscription name which is required to download artifact
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    protected String getRequiredSubscription(String artifact, String version) throws IOException {
        return (String)loadProperties(artifact, version).get(SUBSCRIPTION_PROPERTY);
    }


    /**
     * @return the file name under which artifact is stored in the repository, method doesn't check if artifact exists
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    protected String getFileName(String artifact, String version) throws IOException {
        return (String)loadProperties(artifact, version).get(FILE_NAME_PROPERTY);
    }

    /**
     * @return the path to the artifact
     * @throws com.codenvy.im.update.PropertiesNotFoundException
     *         if property file is absent in the repository
     * @throws java.io.IOException
     *         if an I/O error occurs
     */
    protected Path getArtifact(String artifact, String version) throws IOException {
        String fileName = getFileName(artifact, version);
        return getArtifact(artifact, version, fileName);
    }

    protected Path getArtifact(String artifact, String version, String fileName) {
        return getArtifactDir(artifact, version).resolve(fileName);
    }

    protected Path getRepositoryDir() {
        return Paths.get(repositoryDir);
    }

    protected Path getArtifactDir(String artifact) {
        return getRepositoryDir().resolve(artifact);
    }

    protected Path getArtifactDir(String artifact, String version) {
        return Paths.get(repositoryDir, artifact, version);
    }

    protected Path getPropertiesFile(String artifact, String version) {
        return getArtifactDir(artifact, version).resolve(PROPERTIES_FILE);
    }
}
