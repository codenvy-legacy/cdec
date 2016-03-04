/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.im.cli.command;

import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestLoginCommand extends AbstractTestCommand {
    private static final String TEST_USER_PASSWORD          = "testUserPassword";
    private static final String TEST_USER                   = "testUser";
    private final static String SAAS_SERVER_URL             = "http://codenvy-stg.com";
    private final static String SAAS_SERVER_REMOTE_NAME     = "saas-server";

    private static final String ANOTHER_REMOTE_NAME = "another remote";
    private static final String ANOTHER_REMOTE_URL  = "another remote url";


    private TestedLoginCommand spyCommand;

    @Mock
    private IMArtifactLabeledFacade service;
    @Mock
    private PreferencesStorage      mockPreferencesStorage;
    @Mock
    private CommandSession          commandSession;
    @Mock
    private MultiRemoteCodenvy      mockMultiRemoteCodenvy;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        doReturn(SAAS_SERVER_URL).when(service).getSaasServerEndpoint();

        spyCommand = spy(new TestedLoginCommand());
        spyCommand.facade = service;
        spyCommand.preferencesStorage = mockPreferencesStorage;

        performBaseMocks(spyCommand, true);

        doReturn(SAAS_SERVER_REMOTE_NAME).when(spyCommand).getRemoteNameByUrl(SAAS_SERVER_URL);
        doReturn(true).when(spyCommand).isRemoteForSaasServer(SAAS_SERVER_REMOTE_NAME);
        doReturn(false).when(spyCommand).isRemoteForSaasServer(ANOTHER_REMOTE_NAME);

        doReturn(SAAS_SERVER_URL).when(spyCommand).getRemoteUrlByName(SAAS_SERVER_REMOTE_NAME);
        doReturn(ANOTHER_REMOTE_URL).when(spyCommand).getRemoteUrlByName(ANOTHER_REMOTE_NAME);
    }

    @Test
    public void testLogin() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Login success.\n");
    }

    @Test
    public void testLoginFailed() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        // simulate fail login
        doReturn(false).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Login failed on remote '%s'.\n", SAAS_SERVER_REMOTE_NAME));
    }

    @Test
    public void testLoginWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception"))
            .when(service).getSaasServerEndpoint();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testLoginToSpecificRemote() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--remote", ANOTHER_REMOTE_NAME);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(ANOTHER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Login success on remote '%s' [%s].\n",
                                           ANOTHER_REMOTE_NAME,
                                           ANOTHER_REMOTE_URL));
    }

    @Test
    public void testLoginFailedToSpecificRemote() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--remote", ANOTHER_REMOTE_NAME);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        // simulate fail login
        doReturn(false).when(mockMultiRemoteCodenvy).login(ANOTHER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Login failed on remote '%s'.\n", ANOTHER_REMOTE_NAME));
    }

    class TestedLoginCommand extends LoginCommand {
        protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
            return mockMultiRemoteCodenvy;
        }
    }
}
