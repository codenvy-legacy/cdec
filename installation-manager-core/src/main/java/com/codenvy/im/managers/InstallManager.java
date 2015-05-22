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
import static com.codenvy.im.utils.Commons.isInstall;

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
    public void install(Artifact artifact, Version version, Path pathToBinaries, InstallOptions options) throws IOException {
        if (isInstall(artifact, version)) {
            doInstall(artifact, version, pathToBinaries, options);
        } else {
            doUpdate(artifact, version, pathToBinaries, options);
        }
    }

    /** Install specific artifact. */
    protected void doInstall(Artifact artifact, Version version, Path pathToBinaries, InstallOptions options) throws IOException {
        Command command = artifact.getInstallCommand(version, pathToBinaries, options);
        executeCommand(command);
    }

    /** Updates specific artifact. */
    protected void doUpdate(Artifact artifact, Version version, Path pathToBinaries, InstallOptions options) throws IOException {
        Command command = artifact.getUpdateCommand(version, pathToBinaries, options);
        executeCommand(command);
    }

    /** @return the list with descriptions of installation steps */
    public List<String> getInstallInfo(Artifact artifact, Version version, InstallOptions options) throws IOException {
        if (isInstall(artifact, version)) {
            return doGetInstallInfo(artifact, options);
        } else {
            return doGetUpdateInfo(artifact, options);
        }
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

    protected List<String> doGetUpdateInfo(Artifact artifact, InstallOptions options) throws IOException {
        return artifact.getUpdateInfo(options);
    }

    protected List<String> doGetInstallInfo(Artifact artifact, InstallOptions options) throws IOException {
        return artifact.getInstallInfo(options);
    }

    protected String executeCommand(Command command) throws CommandException {
        return command.execute();
    }


}
