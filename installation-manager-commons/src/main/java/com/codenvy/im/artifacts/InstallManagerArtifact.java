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
import com.codenvy.im.config.Config;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
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

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManagerArtifact extends AbstractArtifact {
    private static final Logger LOG = LoggerFactory.getLogger(InstallManagerArtifact.class);
    public static final String NAME = "installation-manager";

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
                throw new IOException(this.getName());
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
    public List<String> getInstallInfo(Config config, InstallOptions installOptions) throws IOException {
        return new ArrayList<String>() {{
            add("Unpack downloaded installation manager");
            add("Update installation manager");
        }};
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Version version, Path pathToBinaries, Config config, InstallOptions installOptions) {
        try {
            int step = installOptions.getStep();

            final Agent agent = new LocalAgent();

            final Path dirToUnpack = pathToBinaries.getParent().resolve("unpack");

            switch (step) {
                case 0:
                    return new UnpackCommand(pathToBinaries, dirToUnpack, "Unpack downloaded installation manager");

                case 1:
                    final String cliClientPackName = Commons.getBinaryFileName(this, version, "cli");
                    final Path cliClientDir = Paths.get("");  // TODO
                    final Command updateDaemonCommand = new MacroCommand(new ArrayList<Command>() {{
                        add(new UnpackCommand(dirToUnpack.resolve(cliClientPackName), cliClientDir,
                                              "Unpack installation manager cli client"));
                    }}, "Update installation manager cli client");

                    final String imDaemonPackName = Commons.getBinaryFileName(this, version, null);
                    final Path installedPath = getInstalledPath();
                    final Command updateCliClientCommand = new MacroCommand(new ArrayList<Command>() {{
                        add(new SimpleCommand("service codenvy-installation-manager stop", agent, "Stop daemon"));
                        add(new UnpackCommand(dirToUnpack.resolve(imDaemonPackName), installedPath,
                                              "Unpack installation manager daemon"));
                        add(new SimpleCommand(String.format("chmod +x %s/installation-manager", installedPath.toFile().getAbsolutePath()),
                                              agent, "Set daemon file as executable"));
                        add(new SimpleCommand("service installation-manager start", agent, "Start daemon"));
                    }}, "Update installation manager daemon");

                    return new MacroCommand(new ArrayList<Command>() {{
                        add(updateDaemonCommand);
                        add(updateCliClientCommand );
                    }}, "Update installation manager");

                default:
                    throw new IllegalArgumentException(String.format("Step number %d is out of range", step));
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI()).getParent();
    }
}
