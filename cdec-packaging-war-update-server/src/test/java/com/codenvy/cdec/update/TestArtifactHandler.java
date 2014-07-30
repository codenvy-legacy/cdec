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

import com.google.common.io.InputSupplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static com.google.common.io.Files.copy;
import static org.testng.Assert.*;

/**
 * @author Anatoliy Bazko
 */
public class TestArtifactHandler extends BaseTest {

    private ArtifactHandler artifactHandler;

    @BeforeMethod
    public void prepare() throws Exception {
        artifactHandler = new ArtifactHandler(DOWNLOAD_DIRECTORY.toString());
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testShouldThrowExceptionIfDownloadDirectoryDoesNotExist() throws Exception {
        Files.delete(DOWNLOAD_DIRECTORY);

        artifactHandler.getLastVersion("installation-manager");
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void testShouldThrowExceptionIfArtifactDirectoryDoesNotExist() throws Exception {
        artifactHandler.getLastVersion("installation-manager");
    }

    @Test
    public void testGetLastVersion() throws Exception {
        Files.createDirectories(Paths.get("target", "download", "installation-manager", "1.0.1"));
        Files.createDirectories(Paths.get("target", "download", "installation-manager", "1.0.2"));
        Files.createDirectories(Paths.get("target", "download", "installation-manager", "3.1.1"));
        Files.createDirectories(Paths.get("target", "download", "installation-manager", "4.4.1"));

        assertEquals(artifactHandler.getLastVersion("installation-manager"), "4.4.1");
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldGetLastVersionShouldThrowException() throws Exception {
        artifactHandler.getLastVersion("installation-manager");
    }

    @Test
    public void testGetDownloadDirectoryForArtifact() throws Exception {
        assertEquals(artifactHandler.getDirectory("installation-manager").toString(),
                     "target/download/installation-manager");
    }

    @Test
    public void testGetDownloadDirectoryForArtifactWithVersion() throws Exception {
        assertEquals(artifactHandler.getDirectory("installation-manager", "1.0.1").toString(),
                     "target/download/installation-manager/1.0.1");
    }

    @Test
    public void testGetPropertiesFile() throws Exception {
        assertEquals(artifactHandler.getPropertiesFile("installation-manager", "1.0.1").toString(),
                     "target/download/installation-manager/1.0.1/.properties");
    }

    @Test
    public void testLoadStoreProperties() throws Exception {
        Properties properties = new Properties();
        properties.put(ArtifactHandler.VERSION_PROPERTY, "1.0.1");
        properties.put(ArtifactHandler.REVISION_PROPERTY, "abcdef");
        properties.put(ArtifactHandler.BUILD_TIME_PROPERTY, "20140930");
        properties.put(ArtifactHandler.FILE_NAME_PROPERTY, "installation-manager-1.0.1.zip");
        artifactHandler.storeProperties("installation-manager", "1.0.1", properties);

        Path propertiesFile = Paths.get("target", "download", "installation-manager", "1.0.1", ArtifactHandler.ARTIFACT_PROPERTIES);
        assertTrue(Files.exists(propertiesFile));

        properties = artifactHandler.loadProperties("installation-manager", "1.0.1");
        assertNotNull(properties);
        assertEquals(properties.size(), 4);
        assertEquals(properties.getProperty(ArtifactHandler.VERSION_PROPERTY), "1.0.1");
        assertEquals(properties.getProperty(ArtifactHandler.REVISION_PROPERTY), "abcdef");
        assertEquals(properties.getProperty(ArtifactHandler.BUILD_TIME_PROPERTY), "20140930");
        assertEquals(properties.getProperty(ArtifactHandler.FILE_NAME_PROPERTY), "installation-manager-1.0.1.zip");

        assertEquals(artifactHandler.getFileName("installation-manager", "1.0.1"), "installation-manager-1.0.1.zip");
    }

    @Test
    public void testUpload() throws Exception {
        Path tmp = Paths.get("target", "download", "tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, Charset.defaultCharset())) {
            writer.write("hello");
        }

        try (InputStream in = Files.newInputStream(tmp)) {
            artifactHandler.upload(in, "installation-manager", "1.0.1", "installation-manager-1.0.1.jar", new Properties());
        }

        Path artifact = Paths.get("target", "download", "installation-manager", "1.0.1", "installation-manager-1.0.1.jar");
        assertTrue(Files.exists(artifact));
        assertEquals(Files.size(artifact), 5);

        try (BufferedReader reader = Files.newBufferedReader(artifact, Charset.defaultCharset())) {
            assertEquals(reader.readLine(), "hello");
        }
    }

    @Test
    public void testDownload() throws Exception {
        Properties props = new Properties();
        props.put(ArtifactHandler.FILE_NAME_PROPERTY, "installation-manager-1.0.1.jar");
        props.put(ArtifactHandler.VERSION_PROPERTY, "1.0.1");
        artifactHandler.storeProperties("installation-manager", "1.0.1", props);

        Path artifact = Paths.get("target", "download", "installation-manager", "1.0.1", "installation-manager-1.0.1.jar");
        try (BufferedWriter writer = Files.newBufferedWriter(artifact, Charset.defaultCharset())) {
            writer.write("hello");
        }

        Path tmp = Paths.get("target", "download", "tmp");
        try (InputStream in = artifactHandler.download("installation-manager", "1.0.1")) {
            copy(new InputSupplier<InputStream>() {
                @Override
                public InputStream getInput() throws IOException {
                    return in;
                }
            }, tmp.toFile());
        }

        try (BufferedReader reader = Files.newBufferedReader(artifact, Charset.defaultCharset())) {
            assertEquals(reader.readLine(), "hello");
        }
    }

    @Test(expectedExceptions = ArtifactNotFoundException.class)
    public void shouldThrowExceptionIfArtifactDoesNotExist() throws Exception {
        artifactHandler.download("installation-manager", "1.0.1");
    }
}
