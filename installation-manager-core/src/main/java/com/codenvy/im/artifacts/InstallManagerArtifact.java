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
package com.codenvy.im.artifacts;

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
    public static final String NAME = "installation-manager-cli";

    private static final String CODENVY_CLI_DIR_NAME   = "codenvy-cli";
    private static final String UPDATE_CLI_SCRIPT_NAME = "codenvy-cli-update-script.sh";
    private static final String IM_ROOT_DIRECTORY_NAME = "codenvy-im";
    private static final String PATH_TO_JAVA           = IM_ROOT_DIRECTORY_NAME + "/jre";

    @Inject
    public InstallManagerArtifact() {
        super(NAME);
    }

    /** {@inheritDoc} */
    @Override
    public Version getInstalledVersion() throws IOException {
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
    public List<String> getInstallInfo(InstallOptions installOptions) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUpdateInfo(InstallOptions installOptions) throws IOException {
        return new ArrayList<String>() {{
            add("Initialize updating installation manager");
        }};
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Version versionToInstall, final Path pathToBinaries, InstallOptions installOptions) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        int step = installOptions.getStep();

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
                                                               "chmod +x %1$s/bin/* \n" +    // set permissions to execute CLI client scripts
                                                               "sed -i \"2iJAVA_HOME=${HOME}/%3$s\" %1$s/bin/codenvy \n" +
                                                               // setup java home path
                                                               "sed -i \"2iJAVA_HOME=${HOME}/%3$s\" %1$s/bin/interactive-mode \n" +
                                                               // setup java home path
                                                               "rm -f %4$s \n" +             // remove update script
                                                               "",
                                                               cliClientDir.toAbsolutePath(),
                                                               pathToBinaries.toAbsolutePath(),
                                                               PATH_TO_JAVA,
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
