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
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class Installer {

    private final ConfigFactory configFactory;

    @Inject
    public Installer(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

    /** Installs specific artifact. */
    public void install(Artifact artifact, Path pathToBinaries, InstallOptions options) throws IOException {
        Config config = configFactory.loadOrCreateConfig(options);

        Command command = artifact.getInstallCommand(pathToBinaries, config, options);
        executeCommand(command);
    }

    /** @retrun installation information. */
    public List<String> getInstallInfo(Artifact artifact, InstallOptions options) throws IOException {
        Config config = configFactory.loadOrCreateConfig(options);
        return artifact.getInstallInfo(config, options);
    }

    /** Updates specific artifact. */
    public void update(Artifact artifact, Version version, InstallOptions options) throws IOException {
    }

    protected void executeCommand(Command command) throws CommandException {
        command.execute();
    }
}
