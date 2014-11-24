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
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class TestCDECArtifact {
    private CDECArtifact spyCdecArtifact;

    @Mock
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        spyCdecArtifact = spy(new CDECArtifact("", mockTransport));
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        CdecConfig config = new CdecConfig(Collections.<String, String>emptyMap());
        CdecInstallOptions options = new CdecInstallOptions();
        options.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
        options.setStep(1);

        List<String> info = spyCdecArtifact.getInstallInfo(config, options);
        assertNotNull(info);
        assertTrue(info.size() > 1);
    }

    @Test
    public void testGetInstallCommand() throws Exception {
        CdecConfig config = new CdecConfig(Collections.<String, String>emptyMap());
        CdecInstallOptions options = new CdecInstallOptions();
        options.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
        options.setStep(1);

        Command command = spyCdecArtifact.getInstallCommand(Paths.get("some path"), config, options);
        assertNotNull(command);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetInstallCommandError() throws Exception {
        CdecConfig config = new CdecConfig(Collections.<String, String>emptyMap());
        CdecInstallOptions options = new CdecInstallOptions();
        options.setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
        options.setStep(Integer.MAX_VALUE);

        spyCdecArtifact.getInstallCommand(Paths.get("some path"), config, options);
    }

    @Test
    public void testInstalledVersion() throws Exception {
        when(mockTransport.doOption(endsWith("api/"), eq("authToken"))).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");

        Version version = spyCdecArtifact.getInstalledVersion("authToken");
        assertEquals(version, Version.valueOf("3.2.0-SNAPSHOT"));
    }
}
