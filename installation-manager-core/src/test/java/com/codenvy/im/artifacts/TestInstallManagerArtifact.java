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

import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
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

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testGetInstallInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1);

        imArtifact.getInstallInfo(options);
    }

    @Test
    public void testGetUpdateInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1);

        List<String> info = imArtifact.getUpdateInfo(options);
        assertNotNull(info);
        assertEquals(info.toString(), "[Initialize updating installation manager]");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testGeInstallCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setCliUserHomeDir("/home/dummy-user");

        options.setStep(0);
        imArtifact.getInstallCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
    }

    @Test
    public void testGetUpdateCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setCliUserHomeDir("/home/dummy-user");

        options.setStep(0);
        Command command = imArtifact.getUpdateCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
        assertEquals(command.toString(), "["
                                         + "{'command'='sudo sh -c \" echo '#!/bin/bash \n"
                                         + "rm -rf /home/dummy-user/codenvy-im/codenvy-cli/* \n"
                                         + "tar -xzf /home/dummy-user/codenvy-im/child -C /home/dummy-user/codenvy-im/codenvy-cli \n"
                                         + "chmod +x /home/dummy-user/codenvy-im/codenvy-cli/bin/* \n"
                                         + "sed -i \"2iJAVA_HOME=/home/dummy-user/codenvy-im/jre\" /home/dummy-user/codenvy-im/codenvy-cli/bin/codenvy \n"
                                         + "rm -f /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh \n"
                                         + "rm -f /home/dummy-user/codenvy-im/child \n"
                                         + "' > /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh\"', 'agent'='LocalAgent'}, "
                                         + "{'command'='sudo chmod 775 /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh', 'agent'='LocalAgent'}, "
                                         + "{'command'='sudo cp /parent/child /home/dummy-user/codenvy-im/child', 'agent'='LocalAgent'}"
                                         + "]");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Step number 1000 is out of range")
    public void testGetUpdateCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1000);

        imArtifact.getUpdateCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        assertNotNull(imArtifact.getInstalledVersion());
    }

    @Test
    public void testGetInstalledPath() throws Exception {
        assertEquals(imArtifact.getInstalledPath(), testExecutionPath);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testBackupCommand() throws IOException {
        imArtifact.getBackupCommand(null, null);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testRestoreCommand() throws IOException {
        imArtifact.getRestoreCommand(null, null);
    }
}
