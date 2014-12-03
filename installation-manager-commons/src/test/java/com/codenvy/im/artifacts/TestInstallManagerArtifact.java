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
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallManagerArtifact {
    public static final Path PATH_TO_BINARIES = Paths.get("/parent/child");
    private InstallManagerArtifact imArtifact;
    private Path testExecutionPath;

    @Mock
    private HttpTransport mockTransport;

    @BeforeClass
    public void setUp() throws Exception {
        testExecutionPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        imArtifact = (InstallManagerArtifact)ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1);

        List<String> info = imArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertEquals(info.toString(), "[Unpack update of installation manager, Update installation manager]");
    }

    @Test
    public void testGetInstallCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setCliUserHomeDir("/home/dummy-user");

        options.setStep(0);
        Command command = imArtifact.getInstallCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
        assertEquals(command.toString(), "[rm -rf /parent/unpack, " +
                                         "Unpack '/parent/child' into '/parent/unpack', " +
                                         "Unpack '/parent/unpack/installation-manager-1.0.0-binary.tar.gz' into '/parent/unpack/daemon', " +
                                         "Unpack '/parent/unpack/installation-manager-cli-1.0.0-binary.tar.gz' into '/parent/unpack/cli']");

        options.setStep(1);
        command = imArtifact.getInstallCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
        assertEquals(command.toString(), "[[echo '#!/bin/bash \n"
                                         + "rm -rf /home/dummy-user/codenvy-cli/* \n"
                                         + "cp -r /parent/unpack/cli/* /home/dummy-user/codenvy-cli \n"
                                         + "chmod +x /home/dummy-user/codenvy-cli/bin/* \n"
                                         + "newgrp codenvyshare << END\n"
                                         + "  rm -f /home/codenvy-shared/codenvy-cli-update-script.sh ; \n"
                                         + "END\n"
                                         + "' > /home/codenvy-shared/codenvy-cli-update-script.sh ; , "
                                         + "chmod 775 /home/codenvy-shared/codenvy-cli-update-script.sh ; "
                                         + "], "
                                         + "sleep 6 ; "
                                         + testExecutionPath + "/installation-manager stop ; "
                                         + "rm -rf " + testExecutionPath + " ; "
                                         + "mkdir " + testExecutionPath + " ; "
                                         + "cp -r /parent/unpack/daemon/* " + testExecutionPath + " ; "
                                         + "chmod +x " + testExecutionPath + "/installation-manager ; "
                                         + testExecutionPath + "/installation-manager start ; "
                                         + "]");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Step number 1000 is out of range")
    public void testGetInstallCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1000);

        imArtifact.getInstallCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        assertNotNull(imArtifact.getInstalledVersion("authToken"));
    }

    @Test
    public void testGetInstalledPath() throws Exception {
        assertEquals(imArtifact.getInstalledPath(), testExecutionPath);
    }
}
