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

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.command.UnpackCommand;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManagerArtifact extends AbstractArtifact {
    private static final Logger LOG = LoggerFactory.getLogger(InstallManagerArtifact.class);
    public static final String NAME = "installation-manager";

    private static final String CODENVY_CLI_DIR_NAME = "codenvy-cli";
    private static final String UPDATE_CLI_SCRIPT_NAME = "codenvy-cli-update-script.sh";
    private static final String IM_ROOT_DIRECTORY_NAME = "codenvy-im";

    @Inject
    public InstallManagerArtifact() {
        super(NAME);
    }

    /** {@inheritDoc} */
    @Override
    public Version getInstalledVersion(String authToken) throws IOException {
        try (InputStream in = Artifact.class.getClassLoader().getResourceAsStream("codenvy/BuildInfo.properties")) {
            Properties props = new Properties();
            props.load(in);

            if (props.containsKey("version")) {
                return Version.valueOf((String)props.get("version"));
            } else {
                throw new IOException(format("Can't get the version of '%s' artifact", NAME));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return new ArrayList<String>() {{
            add("Initialize updating installation manager");
        }};
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Version version, final Path pathToBinaries, InstallOptions installOptions) {
        int step = installOptions.getStep();

        final Path dirToUnpack = pathToBinaries.getParent().resolve("unpack");
        final Agent syncAgent = new LocalAgent();

        switch (step) {
            case 0:
                final Path cliClientDir = Paths.get(installOptions.getCliUserHomeDir())
                                                .resolve(IM_ROOT_DIRECTORY_NAME)
                                                .resolve(CODENVY_CLI_DIR_NAME);

                final Path updateCliScript = Paths.get(installOptions.getCliUserHomeDir())
                                                              .resolve(IM_ROOT_DIRECTORY_NAME)
                                                              .resolve(UPDATE_CLI_SCRIPT_NAME);

                final String contentOfUpdateCliScript = format("#!/bin/bash \n" +
                                                               "rm -rf %1$s/* \n" +          // remove content of cli client dir
                                                               "tar -xzf %2$s -C %1$s \n" +  // unpack update into the cli client dir
                                                               "chmod +x %3$s \n" +          // set permissions to execute CLI client scripts
                                                               "rm -f %4$s ; \n" +           // remove update script
                                                               "",
                                                               cliClientDir.toAbsolutePath(),
                                                               pathToBinaries.toAbsolutePath(),
                                                               cliClientDir.resolve("bin/*").toAbsolutePath(),
                                                               updateCliScript.toAbsolutePath());

                return new MacroCommand(ImmutableList.<Command>of(
                    new SimpleCommand(format("echo '%s' > %s ; ", contentOfUpdateCliScript, updateCliScript.toAbsolutePath()),
                                          syncAgent, "Create script to update cli client"),

                    new SimpleCommand(format("chmod 775 %s ; ", updateCliScript.toAbsolutePath()),
                                          syncAgent, "Set permissions to execute update script")),
                "Update installation manager CLI client");
            default:
                throw new IllegalArgumentException(format("Step number %d is out of range", step));
        }
    }

    /** @return path where artifact located */
    protected Path getInstalledPath() throws URISyntaxException {
        URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI()).getParent();
    }
}
