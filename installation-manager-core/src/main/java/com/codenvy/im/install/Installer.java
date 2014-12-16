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
package com.codenvy.im.install;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.codenvy.im.utils.Commons.isInstall;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class Installer {

    @Inject
    public Installer() {
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
