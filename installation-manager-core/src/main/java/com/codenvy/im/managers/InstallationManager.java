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
package com.codenvy.im.managers;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.utils.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Anatoliy Bazko
 */
public interface InstallationManager {

    /** @return installation information. */
    List<String> getInstallInfo(Artifact artifact, Version version, InstallOptions options) throws IOException;

    /**
     * Install the specific version of the artifact.
     *
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    void install(String authToken, Artifact artifact, Version version, InstallOptions options) throws IOException;

    /**
     * Scans all installed artifacts and returns their versions.
     *
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    Map<Artifact, Version> getInstalledArtifacts() throws IOException;

    /**
     * @return downloaded artifacts from the local repository
     * @throws IOException
     *         if an I/O error occurs
     */
    Map<Artifact, SortedMap<Version, Path>> getDownloadedArtifacts() throws IOException;

    /**
     * @return set of downloaded into local repository versions of artifact
     * @throws IOException
     *         if an I/O error occurs
     */
    SortedMap<Version, Path> getDownloadedVersions(Artifact artifact) throws IOException;

    /**
     * @return the list of the artifacts to update.
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    Map<Artifact, Version> getUpdates() throws IOException;

    /**
     * @param authToken
     *         the authentication token
     * @return the latest version of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    Version getLatestInstallableVersion(String authToken, Artifact artifact) throws IOException;


    /**
     * Download the specific version of the artifact.
     *
     * @return path to downloaded artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     * @throws java.lang.IllegalStateException
     *         if the subscription is invalid or expired
     */
    Path download(Artifact artifact, Version version) throws IOException, IllegalStateException;

    /** Checks if FS has enough free space, for instance to download artifacts */
    void checkEnoughDiskSpace(long size) throws IOException;

    /** Checks connection to server is available */
    void checkIfConnectionIsAvailable() throws IOException;

    /** @return the configuration */
    LinkedHashMap<String, String> getConfig();

    /**
     * @return path to artifact into the local repository
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    Path getPathToBinaries(Artifact artifact, Version version) throws IOException;

    /**
     * @return size in bytes of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    Long getBinariesSize(Artifact artifact, Version version) throws IOException;

    /** Filters what need to download, either all updates or a specific one. */
    Map<Artifact, Version> getUpdatesToDownload(Artifact artifact, Version version) throws IOException;

    boolean isInstallable(Artifact artifact, Version version) throws IOException;

    /** Add node to multi-server Codenvy */
    NodeConfig addNode(String dns) throws IOException;

    /** Remove node from multi-server Codenvy */
    NodeConfig removeNode(String dns) throws IOException;

    /**
     * Perform backup according to certain backup config.
     *
     * @return updated backup config
     */
    BackupConfig backup(BackupConfig config) throws IOException;

    /** Perform backup according to certain backup config. */
    void restore(BackupConfig config) throws IOException;

    /**
     * Stores installation related properties.
     *
     * @param newProperties
     *         properties to store
     * @throws IOException
     *         if any I/O error occurred
     */
    void storeProperties(Map<String, String> newProperties) throws IOException;

    /**
     * Loads installation related properties.
     * If there is no stored specific property then no value will be returned.
     *
     * @param names
     *         properties names to load
     * @throws IOException
     *         if any I/O error occurred
     */
    Map<String, String> readProperties(Collection<String> names) throws IOException;

    /**
     * Change codenvy config property.
     * @param property property name
     * @param value property value
     * @throws IOException if any I/O error occurred
     */
    void changeCodenvyConfig(String property, String value) throws IOException;
}
