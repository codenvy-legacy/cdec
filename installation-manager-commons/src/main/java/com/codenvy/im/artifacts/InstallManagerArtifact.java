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

import com.codenvy.im.command.Command;
import com.codenvy.im.config.Config;
import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.install.InstallOptions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.IOUtils.toByteArray;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class InstallManagerArtifact extends AbstractArtifact {
    private static final Logger LOG = LoggerFactory.getLogger(InstallManagerArtifact.class);

    // That is figure out for issue https://jira.codenvycorp.com/browse/CDEC-20
    /*private static final String updateScriptCatchErrors = "trap catch_errors ERR ;\n\n" +
                                                          "error_count=0 ;\n\n" +
                                                          "function catch_errors() {\n" +
                                                          "   error_count=$(expr $error_count + 1) ; \n" +
                                                          "}\n\n";

    private static final String updateScriptCheckErrors = "if [[ $error_count -eq 0 ]]; then\n" +
                                                          "rm -f ./im-update.log\n" +
                                                          "fi";*/


    public static final String NAME = "installation-manager";

    @Inject
    public InstallManagerArtifact() {
        super(NAME);
    }

    @Override
    public void install(Path pathToBinaries, CdecInstallOptions options) throws IOException {
        Path dirForUpdate = pathToBinaries.getParent().resolve("unpack");
        try {
            if (exists(dirForUpdate)) {
                cleanDirectory(dirForUpdate.toFile());
            } else {
                createDirectories(dirForUpdate);
            }

            unpack(pathToBinaries, dirForUpdate);

            File im = null;
            File imCli = null;

            for (File file : listFiles(dirForUpdate.toFile(), null, false)) {
                if (file.getName().startsWith(NAME + "-cli")) {
                    imCli = file;
                } else {
                    im = file;
                }
            }

            if (im == null) {
                throw new IOException("Installation Manager binaries not found");
            } else if (imCli == null) {
                throw new IOException("Installation Manager CLI binaries not found");
            }

            Path dirImUpdateUnpack = dirForUpdate.resolve("im");
            unpack(im.toPath(), dirImUpdateUnpack);

            Path dirImCliUpdate = getImCliUpdateScriptDir().resolve("codenvy-cli");
            if (exists(dirImCliUpdate)) {
                cleanDirectory(dirImCliUpdate.toFile());
            }
            createDirectories(dirImCliUpdate);
            Path fileImCliUpdate = dirImCliUpdate.resolve(imCli.getName());

            Files.createFile(fileImCliUpdate);
            Files.copy(imCli.toPath(), new FileOutputStream(fileImCliUpdate.toFile()));

            createImCliUpdateScript(fileImCliUpdate);

            restart(dirImUpdateUnpack);
        } catch (InterruptedException | URISyntaxException e) {
            if (exists(dirForUpdate)) {
                try {
                    cleanDirectory(dirForUpdate.toFile());
                } catch (IOException ioe) {
                    LOG.error("Can't remove temporary unpacked files in : " + dirForUpdate);
                }
            }

            throw new IOException(e.getMessage(), e);
        }
    }

    private void createImCliUpdateScript(Path fileImCliUpdateTarGz) throws IOException, URISyntaxException {
        String[] imCliInstalledParams = getImCliInstalledProperties();

        String imCliInstalledPath = imCliInstalledParams[0];
        String imCliUpdateScriptDir = imCliInstalledParams[1];
        String codenvyShareGroup = imCliInstalledParams[2];
        String userName = imCliInstalledParams[3];
        String userGroup = imCliInstalledParams[4];

        List<String> commands = new ArrayList<>();

        commands.add("#!/bin/bash");
        commands.add("cd ~ ;");
        commands.add("rm -rf " + imCliInstalledPath + "/* ;");
        commands.add("newgrp " + codenvyShareGroup + " << END1");
        commands.add("tar -xzf " + fileImCliUpdateTarGz + " -C " + imCliInstalledPath + " ;");
        commands.add("rm -rf " + fileImCliUpdateTarGz.getParent() + " ;");
        commands.add("chown -R " + userName + ":" + userGroup + " " + imCliInstalledPath + " ;");

        Path updateScript = (new File(imCliUpdateScriptDir)).toPath().resolve("codenvy-cli-update-script.sh");
        commands.add("rm -f " + updateScript + " ;");
        commands.add("END1");

        commands.add("newgrp " + userGroup + " << END2");
        commands.add("chmod +x " + imCliInstalledPath + "/bin/* ;");
        commands.add("END2");

        Files.deleteIfExists(updateScript);
        try (FileOutputStream out = new FileOutputStream(updateScript.toFile())) {
            IOUtils.writeLines(commands, "\n", out);
        }

        setPermissionOwnerGroupRWXOtherR(updateScript);
    }

    private void setPermissionOwnerGroupRWXOtherR(Path updateScript) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        //add owners permission
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        //add group permissions
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);

        perms.add(PosixFilePermission.OTHERS_READ);

        Files.setPosixFilePermissions(updateScript, perms);
    }

    private Path getImCliUpdateScriptDir() throws IOException, URISyntaxException {
        return new File(getImCliInstalledProperties()[1]).toPath();
    }

    private String[] getImCliInstalledProperties() throws IOException, URISyntaxException {
        Path fileWithImCliInstalled = getInstalledPath().getParent().resolve(".codenvy/codenvy-cli-installed");

        if (!exists(fileWithImCliInstalled)) {
            throw new IOException("File " + fileWithImCliInstalled.toFile().getAbsolutePath() + " doesn't exist.");
        }

        byte[] bytes = toByteArray(fileWithImCliInstalled.toUri());
        return new String(bytes, 0, bytes.length, "UTF-8").split("\n");

    }

    private void restart(Path unpackedUpdates) throws IOException, InterruptedException, URISyntaxException {
        String installedPath = getInstalledPath().toFile().getAbsolutePath();
        StringBuilder stringBuilder = new StringBuilder(200);

        stringBuilder.append("sleep 5 ; ") // a little bit time to answer to CLI
                .append(installedPath).append("/installation-manager stop ; ")
                .append("cp -r ")
                .append(unpackedUpdates.toFile().getAbsolutePath())
                .append("/* ")
                .append(installedPath).append(" ; ")
                .append("chmod +x ").append(installedPath).append("/installation-manager ; ")
                .append("rm -rf ").append(unpackedUpdates.getParent().toFile().getAbsolutePath()).append(" ; ")
                .append(installedPath).append("/installation-manager start ");

        runCommand(stringBuilder.toString());
    }

    private void runCommand(String command) throws IOException, InterruptedException, URISyntaxException {
        LOG.info("Executing  command: " + command);

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.start();

        LOG.info("Executed command: " + command);
    }

    @Override
    public String getInstalledVersion(String authToken) throws IOException {
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
    public List<String> getInstallInfo(Config config, InstallOptions installOptions) throws IOException {
        return Collections.emptyList();
        // TODO
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Config config, InstallOptions installOptions) {
        return null;
        // TODO
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(location.toURI()).getParent();
    }
}
