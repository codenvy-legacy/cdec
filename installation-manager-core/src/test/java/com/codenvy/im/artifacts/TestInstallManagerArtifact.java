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
        assertEquals(info.toString(), "[Initialize updating installation manager]");
    }

    @Test
    public void testGetInstallCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setCliUserHomeDir("/home/dummy-user");

        options.setStep(0);
        Command command = imArtifact.getInstallCommand(Version.valueOf("1.0.0"), PATH_TO_BINARIES, options);
        assertEquals(command.toString(), "[echo '"
                                         + "#!/bin/bash \n"
                                         + "rm -rf /home/dummy-user/codenvy-im/codenvy-cli/* \n"
                                         + "tar -xzf /parent/child -C /home/dummy-user/codenvy-im/codenvy-cli \n"
                                         + "chmod +x /home/dummy-user/codenvy-im/codenvy-cli/bin/* \n"
                                         + "rm -f /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh ; \n"
                                         + "' > /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh ; , "
                                         + "chmod 775 /home/dummy-user/codenvy-im/codenvy-cli-update-script.sh ; "
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
