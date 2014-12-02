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

import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallManagerArtifact {
    private InstallManagerArtifact spyImArtifact;

    @Mock
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        spyImArtifact = spy(new InstallManagerArtifact());
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(1);

        List<String> info = spyImArtifact.getInstallInfo(options);
        assertNotNull(info);
        assertEquals(info.toString(), "[Unpack downloaded installation manager, Update installation manager]");
    }

    @Test
    public void testGetInstallCommand() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(0);

//  TODO      Command command = spyImArtifact.getInstallCommand(Version.valueOf("1.0.0"), Paths.get("some path"), null, options);
//        assertNotNull(command);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetInstallCommandError() throws Exception {
        InstallOptions options = new InstallOptions();
        options.setStep(Integer.MAX_VALUE);

        Path executionDir = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        spyImArtifact.getInstallCommand(Version.valueOf("1.0.0"), executionDir, options);
    }

    @Test
    public void testGetInstalledVersion() throws Exception {
        Version version = spyImArtifact.getInstalledVersion("authToken");
//  TODO      assertEquals(version, Version.valueOf("3.2.0-SNAPSHOT"));
    }

    @Test
    public void testGetInstalledPath() throws Exception {
        // TODO
    }

    @Test
    public void testGetInstalledVersionReturnNullIfCDECNotInstalled() throws Exception {
//  TODO      doThrow(new IOException()).when(mockTransport).doOption(endsWith("api/"), eq("authToken"));
//        Version version = spyImArtifact.getInstalledVersion("authToken");
//        assertNull(version);
    }
}
