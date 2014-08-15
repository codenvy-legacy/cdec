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
package com.codenvy.cdec;

import com.codenvy.cdec.artifacts.Artifact;

import java.io.IOException;
import java.util.Map;

/**
 * @author Anatoliy Bazko
 */
public interface InstallationManager {


    /**
     * Install artifact if existed version lower than downloaded one.
     *
     * @return the installed version of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    public String installArtifact(Artifact artifact) throws IOException;

    /**
     * Scans all available artifacts and returns their current versions.
     */
    public Map<Artifact, String> getInstalledArtifacts() throws IOException;

    /**
     * Scans all available artifacts and returns their last versions from Update Server.
     */
    Map<Artifact, String> getAvailable2DownloadArtifacts() throws IOException;

    /**
     * Downloads updates.
     */
    void downloadUpdates() throws IOException;

    /**
     * @return the list of artifacts with newer versions than currently installed
     */
    Map<Artifact, String> getNewVersions();

    /**
     * Checks if new versions are available. The retrieved list can be obtained by invoking {@link #getNewVersions()} method.
     *
     * @throws java.io.IOException
     *         if I/O error occurred
     */
    void checkNewVersions() throws IOException, IllegalArgumentException;
}
