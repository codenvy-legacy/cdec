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

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.installer.InstallInProgressException;
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.installer.InstallStartedException;
import com.codenvy.im.installer.Installer;
import com.codenvy.im.utils.HttpTransport;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/** @author Anatoliy Bazko
 *  @author Dmytro Nochevnov
 * */
public class TestCDECArtifact {
    private CDECArtifact spyCdecArtifact;

    @Mock
    private HttpTransport mockTransport;

    @Mock
    static Command mockTestCommand1;

    @Mock
    static Command mockTestCommand2;

    static final Path testPath = Paths.get("test path");

    static final String TEST_COMMAND_1 = "test command 1";
    static final String TEST_COMMAND_2 = "test command 2";

    static final Installer.Type testType    = Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER;
    static final InstallOptions testOptions = new InstallOptions().setType(testType);

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_COMMAND_1).when(mockTestCommand1).toString();
        doReturn(TEST_COMMAND_2).when(mockTestCommand2).toString();
        Installer testInstaller = new TestInstaller(testPath, testType);

        spyCdecArtifact = spy(new CDECArtifact("", mockTransport));
        doReturn(testInstaller).when(spyCdecArtifact).createInstaller(testPath, testOptions);
    }

    @Test
    public void testInstallSequence() {
        try {
            spyCdecArtifact.install(testPath, testOptions);
        } catch (InstallStartedException ise) {
            InstallOptions installOptions = ise.getInstallOptions();
            assertEquals(installOptions.getType(), testOptions.getType());

            assertEquals(installOptions.getCommandsInfo().size(), 2);
            assertEquals(installOptions.getCommandsInfo().get(0), TEST_COMMAND_1);
            assertEquals(installOptions.getCommandsInfo().get(1), TEST_COMMAND_2);

            String installId = installOptions.getId();
            assertNotNull(installId);

            try {
                spyCdecArtifact.install(testPath, installOptions);
            } catch (InstallInProgressException ipe) {
                spyCdecArtifact.install(testPath, installOptions);
                return;
            }

            fail("InstallInProgressException wasn't thrown.");
        }

        fail("InstallStartedException wasn't thrown");
    }

    public static class TestInstaller extends Installer {
        public TestInstaller(Path pathToBinaries, Installer.Type installType) throws ConfigException, AgentException {
            super(pathToBinaries, installType);
        }

        @Override protected LinkedList<Command> getInstallCommands(Path pathToBinaries) throws AgentException, ConfigException {
            return new LinkedList<Command>() {{
                add(mockTestCommand1);
                add(mockTestCommand2);
            }};
        }
    }

    @Test
    public void testInstalledVersion() throws Exception {
        when(mockTransport.doOption(endsWith("api/"), eq("authToken"))).thenReturn("{\"ideVersion\":\"3.2.0-SNAPSHOT\"}");

        String version = spyCdecArtifact.getInstalledVersion("authToken");
        assertEquals(version, "3.2.0-SNAPSHOT");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testGetInstalledPath() throws URISyntaxException {
        spyCdecArtifact.getInstalledPath();
    }
}
