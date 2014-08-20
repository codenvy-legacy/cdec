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
     * Install the artifact if the existed version lower than a downloaded one.
     *
     * @return the version of newly installed artifact or the current one if installation wasn't necessary
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    String install(Artifact artifact) throws IOException;

    /**
     * Scans all installed artifacts and returns their versions.
     */
    Map<Artifact, String> getInstalledArtifacts() throws IOException;

    /**
     * @return the list of the artifacts to update.
     * @throws java.io.IOException
     *         if I/O error occurred
     */
    Map<Artifact, String> getUpdates() throws IOException;

    /**
     * Download the specific version of the artifact.
     */
    void download(Artifact artifact, String version) throws IOException;


    /**
     * Downloads updates.
     */
    void download() throws IOException;
}
