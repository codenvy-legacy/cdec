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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Override
    public void install(Path pathToBinaries) throws IOException {
        try {
            Path dirForUpdate = pathToBinaries.getParent().resolve("unpack");
            if (Files.exists(dirForUpdate)) {
                FileUtils.cleanDirectory(dirForUpdate.toFile());
            } else {
                Files.createDirectories(dirForUpdate);
            }

            unpack(pathToBinaries, dirForUpdate);

            File im = null;

            for (File file : FileUtils.listFiles(dirForUpdate.toFile(), null, false)) {
                if (!file.getName().startsWith(NAME + "-cli")) {
                    im = file;
                }
            }

            Path dirImUpdateUnpack = dirForUpdate.resolve("im");
            unpack(im.toPath(), dirImUpdateUnpack);

            restart(dirImUpdateUnpack);
        } catch (InterruptedException | URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void restart(Path unpackedUpdates) throws IOException, InterruptedException, URISyntaxException {
        String installedPath = getInstalledPath().toFile().getAbsolutePath();
        StringBuilder stringBuilder = new StringBuilder(200);

        stringBuilder.append("sleep 5 ; ") // a little bit time to answer to CLI
                .append(installedPath).append("/installation-manager stop ; ")
                .append("cp -r ")
                .append(unpackedUpdates.toFile().getAbsolutePath())
                .append("/* ")
                .append(installedPath)
                .append(" ; ")
                .append("chmod +x " + installedPath + "/installation-manager ; ")
                .append("rm -rf ")
                .append(unpackedUpdates.getParent().toFile().getAbsolutePath())
                .append(" ; ")
                .append(installedPath).append("/installation-manager start ");

        runCommand(stringBuilder.toString());
    }

    private void runCommand(String command) throws IOException, InterruptedException, URISyntaxException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.start();
        LOG.info("Executed command: " + command);
    }

    @Override
    public String getInstalledVersion(String accessToken) throws IOException {
        try (InputStream in = Artifact.class.getClassLoader().getResourceAsStream("codenvy/BuildInfo.properties")) {
            Properties props = new Properties();
            props.load(in);

            if (props.containsKey("version")) {
                return (String)props.get("version");
            } else {
                throw new IOException(this.getName());
            }
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI()).getParent();
    }
}
