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
package com.codenvy.im.install;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.ConfigFactory;

import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstaller {

    private static final String PRIVATE_KEY = TestInstaller.class.getClassLoader().getResource("../test-classes/test_rsa").getFile();

    @Mock
    private ConfigFactory configFactory;
    private Installer     installer;

    @BeforeTest
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        installer = spy(new Installer(configFactory));
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        Artifact artifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
        InstallOptions options = new InstallOptions().setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

        doReturn(new CdecConfig(Collections.<String, String>emptyMap())).when(configFactory).loadOrCreateDefaultConfig(options);

        List<String> info = installer.getInstallInfo(artifact, options);
        assertTrue(info.size() > 0);
    }

    // @Test TODO
    public void testInstall() throws Exception {
        FileUtils.write(new File("target/key"), "key");

        Artifact artifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);

        InstallOptions options = new InstallOptions();
        options.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);
        options.setStep(1);

        doReturn(new CdecConfig(new HashMap<String, String>())).when(configFactory).loadOrCreateDefaultConfig(options);  // TODO set CdecConfig properties
        doNothing().when(installer).executeCommand(any(Command.class));

        installer.install(artifact, Paths.get("some path"), options);
        verify(installer).executeCommand(any(Command.class));
    }

    @Test
    public void testUpdate() throws Exception {
        installer.update(null, null, null);
    }
}
