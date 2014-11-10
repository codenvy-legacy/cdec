/*
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
package com.codenvy.im.artifacts;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.installer.InstallInProgressException;
import com.codenvy.im.installer.InstallStartedException;
import com.codenvy.im.installer.Installer;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 * */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String        updateEndpoint;

    private Installer installer;

    @Inject
    public CDECArtifact(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                        HttpTransport transport) {
        super(NAME);
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
    }

    @Override
    public void install(Path pathToBinaries) throws CommandException, AgentException, ConfigException, InstallStartedException,
                                                    InstallInProgressException {
        install(pathToBinaries, Installer.InstallType.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER);
    }

    public void install(Path pathToBinaries, Installer.InstallType installType) throws
                                                                      CommandException,
                                                                      AgentException,
                                                                      ConfigException,
                                                                      InstallStartedException,
                                                                      InstallInProgressException {
        if (installer == null) {
            installer = new Installer(pathToBinaries, installType);
            throw new InstallStartedException(installer.getCommandsInfo());
        }

        installer.executeNextCommand();

        if (installer.isFinished()) {
            installer = null;
            return;
        }

        throw new InstallInProgressException();
    }

    @Override
    public String getInstalledVersion(String accessToken) throws IOException {
        return null;  // TODO issue CDEC-62
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean isInstallable(Version versionToInstall, String accessToken) {
        return true;
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }

}
