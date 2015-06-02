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
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.im.utils.Commons.getProperException;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManager {
    private final Set<Artifact> artifacts;

    @Inject
    public InstallManager(Set<Artifact> artifacts) {
        this.artifacts = new Commons.ArtifactsSet(artifacts); // keep order
    }

    /**
     * Scans all installed artifacts and returns their versions.
     *
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    public Map<Artifact, Version> getInstalledArtifacts() throws IOException {
        Map<Artifact, Version> installed = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                Version installedVersion = artifact.getInstalledVersion();
                if (installedVersion != null) {
                    installed.put(artifact, installedVersion);
                }
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** Installs specific artifact. */
    public void performInstallStep(Artifact artifact, Version version, Path pathToBinaries, InstallOptions options) throws IOException {
        Command command = artifact.getInstallCommand(version, pathToBinaries, options);
        executeCommand(command);
    }

    /** Updates specific artifact. */
    public void performUpdateStep(Artifact artifact, Version version, Path pathToBinaries, InstallOptions options) throws IOException {
        Command command = artifact.getUpdateCommand(version, pathToBinaries, options);
        executeCommand(command);
    }

    /** @return the list with descriptions of installation steps */
    public List<String> getInstallInfo(Artifact artifact, InstallType installType) throws IOException {
        return artifact.getUpdateInfo(installType);
    }

    /** @return the list with descriptions of installation steps */
    public List<String> getUpdateInfo(Artifact artifact, InstallType installType) throws IOException {
        return artifact.getUpdateInfo(installType);
    }

    /**
     * @return the latest version of the artifact
     * @throws java.io.IOException
     *         if an I/O error occurred
     */
    public Version getLatestInstallableVersion(Artifact artifact) throws IOException {
        return artifact.getLatestInstallableVersion();
    }

    public boolean isInstallable(Artifact artifact, Version version) throws IOException {
        return artifact.isInstallable(version);
    }

    protected String executeCommand(Command command) throws CommandException {
        return command.execute();
    }
}
