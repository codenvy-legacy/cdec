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
package com.codenvy.im.installer;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.ConfigException;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class InstallerTest {
    SshServer spySshd;
    Installer installer;

    static final String TEST_USER             = "vagrant";
    static final String TEST_HOST             = "127.0.0.1";
    static final int    TEST_PORT             = 2224;
    static final String TEST_PASSWORD         = "vagrant";
    static final String TEST_AUTH_PRIVATE_KEY = "~/.ssh/id_rsa";

    @Mock
    CdecConfig mockConfig;

    @BeforeTest
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        spySshd = getSpySshd();
        spySshd.start();

        installer = new Installer();
    }

    @Test
    public void testGetSingleNodeWithoutPuppetMasterInstallCommandsUsing() {
        // connect to agent using password
        doReturn(TEST_HOST).when(mockConfig).getHost();
        doReturn(String.valueOf(TEST_PORT)).when(mockConfig).getSSHPort();
        doReturn(TEST_USER).when(mockConfig).getUser();
        doReturn(TEST_PASSWORD).when(mockConfig).getPassword();

        List<Command> commands = installer.getInstallCdecOnSingleNodeWithPuppetMasterCommands(Paths.get(""), mockConfig);
        assertNotNull(commands);
        assertEquals(commands.size(), 5);

        // use connect to agent using auth key
        doReturn("").when(mockConfig).getPassword();
        doReturn(TEST_AUTH_PRIVATE_KEY).when(mockConfig).getPrivateKeyFileAbsolutePath();

        commands = installer.getInstallCdecOnSingleNodeWithPuppetMasterCommands(Paths.get(""), mockConfig);
        assertNotNull(commands);
        assertEquals(commands.size(), 5);
    }

    @Test(expectedExceptions = ConfigException.class,
          expectedExceptionsMessageRegExp = "Installation config file 'config file' is incomplete.")
    public void testGetSingleNodeWithoutPuppetMasterInstallCommandsWithIncompleteConfig() {
        doReturn("").when(mockConfig).getHost();
        doReturn("").when(mockConfig).getSSHPort();
        doReturn("config file").when(mockConfig).getConfigSource();

        installer.getInstallCdecOnSingleNodeWithPuppetMasterCommands(Paths.get(""), mockConfig);
    }

    @Test(expectedExceptions = AgentException.class,
          expectedExceptionsMessageRegExp = "Can't connect to host 'wrong user@wrong host:0'. " +
                                            "Error: java.net.UnknownHostException: wrong host")
    public void testGetSingleNodeWithoutPuppetMasterInstallCommandsWhenConnectionToServerError() {
        doReturn("wrong host").when(mockConfig).getHost();
        doReturn("0").when(mockConfig).getSSHPort();
        doReturn("wrong user").when(mockConfig).getUser();
        doReturn("wrong password").when(mockConfig).getPassword();

        installer.getInstallCdecOnSingleNodeWithPuppetMasterCommands(Paths.get(""), mockConfig);
    }

    @AfterTest
    public void tearDown() throws InterruptedException {
        spySshd.stop();
    }

    private SshServer getSpySshd() {
        SshServer sshdSpy = spy(SshServer.setUpDefaultServer());
        sshdSpy.setPort(TEST_PORT);

        sshdSpy.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        sshdSpy.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                return true;
            }
        });

        sshdSpy.setCommandFactory(new CommandFactory() {
            public org.apache.sshd.server.Command createCommand(String command) {
                return new ProcessShellFactory(command.split(" ")).create();
            }
        });

        return sshdSpy;
    }
}
