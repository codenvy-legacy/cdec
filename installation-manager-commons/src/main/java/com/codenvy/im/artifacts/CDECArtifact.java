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

import com.codenvy.api.core.rest.shared.dto.ApiInfo;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.installer.InstallInProgressException;
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.installer.InstallStartedException;
import com.codenvy.im.installer.Installer;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.codenvy.im.utils.Commons.combinePaths;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
// TODO
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String apiNodeUrl;

    protected Installer installer; // TODO

    @Inject
    public CDECArtifact(@Named("cdec.api-node.url") String apiNodeUrl, HttpTransport transport) {
        super(NAME);
        this.transport = transport;
        this.apiNodeUrl = apiNodeUrl;
    }

    public CDECArtifact(String apiNodeUrl, HttpTransport transport, String name) {
        super(name);
        this.transport = transport;
        this.apiNodeUrl = apiNodeUrl;
    }

    /** {@inheritDoc} */
    @Override
    public void install(Path pathToBinaries, InstallOptions options) throws CommandException,
                                                                            AgentException,
                                                                            ConfigException,
                                                                            InstallStartedException,
                                                                            InstallInProgressException,
                                                                            IllegalArgumentException {
        if (installer == null
            || !installer.getOptions().getId().equals(options.getId())) {

            if (options == null
                || options.getType() == null) {
                throw new IllegalArgumentException("Install type is unknown.");
            }

            installer = createInstaller(pathToBinaries, options);
            throw new InstallStartedException(installer.getOptions());
        }

        installer.executeNextCommand();

        if (installer.isFinished()) {
            installer = null;
            return;
        }

        throw new InstallInProgressException();
    }

    /** {@inheritDoc} */
    protected Installer createInstaller(Path pathToBinaries, InstallOptions options) {
        return new Installer(pathToBinaries, options.getType());
    }

    // TODO
    /** {@inheritDoc} */
    @Override
    public String getInstalledVersion(String authToken) throws IOException {
        String response = transport.doOption(combinePaths(apiNodeUrl, "api/"), authToken);
        ApiInfo apiInfo = null;
        try {
            apiInfo = Commons.fromJson(response, ApiInfo.class);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
        return apiInfo.getIdeVersion();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }

}
