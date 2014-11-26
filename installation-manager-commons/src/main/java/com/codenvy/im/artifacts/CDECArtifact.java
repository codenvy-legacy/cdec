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
import com.codenvy.im.command.Command;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.Config;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String apiNodeUrl;

    @Inject
    public CDECArtifact(@Named("cdec.api-node.url") String apiNodeUrl, HttpTransport transport) {
        super(NAME);
        this.transport = transport;
        this.apiNodeUrl = apiNodeUrl;
    }

    /** {@inheritDoc} */
    @Override
    public Version getInstalledVersion(String authToken) throws IOException {
        String response;
        try {
            response = transport.doOption(combinePaths(apiNodeUrl, "api/"), authToken);
        } catch (IOException e) {
            return null;
        }

        try {
            ApiInfo apiInfo = Commons.fromJson(response, ApiInfo.class);
            return Version.valueOf(apiInfo.getIdeVersion());
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(Config config, InstallOptions installOptions) throws IOException {
        return new ArrayList<String>() {{
            add("Disable SELinux");
            add("Install puppet client");
            add("Unzip the CDEC binaries");
        }};
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Version version, Path pathToBinaries, Config config, InstallOptions installOptions) throws IOException {
        if (!(config instanceof CdecConfig)) {
            throw new IllegalArgumentException("Unexpected config class " + config.getClass().getName());
        }

        final CdecConfig cdecConfig = (CdecConfig)config;

        StringBuilder command;
        int step = installOptions.getStep();

        switch (step) {
            case 0:
                command = new StringBuilder();  // TODO use MacroCommand
                command.append("sudo setenforce 0");
                command.append(" && ");
                command.append("sudo cp /etc/selinux/config /etc/selinux/config.bak");
                command.append(" && ");
                command.append("sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config");
                return new SimpleCommand(command.toString(), initLocalAgent(), "Disable SELinux");

            case 1:
                command = new StringBuilder();   // TODO use MacroCommand
                command.append(format("sudo rpm -ivh %s", cdecConfig.getPuppetResourceUrl()));
                command.append(" && ");
                command.append(format("sudo yum install %s -y", cdecConfig.getPuppetVersion()));
                return new SimpleCommand(command.toString(), initLocalAgent(), "Install puppet client");

            case 2:
                command = new StringBuilder();   // TODO use UnpackCommand
                command.append(format("sudo unzip %s -d /etc/puppet", pathToBinaries.toString()));
                return new SimpleCommand(command.toString(), initLocalAgent(), "Unzip the CDEC binaries");


            default:
                throw new IllegalArgumentException(String.format("Step number %d is out of range", step));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }
}
