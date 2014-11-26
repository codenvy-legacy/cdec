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
package com.codenvy.im.cli.command;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.utils.Commons;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestLoginCommand {
    private static final String TEST_USER_ACCOUNT_ID        = "testUserAccountId";
    private static final String TEST_USER_ACCOUNT_NAME      = "testUserAccountName";
    private static final String TEST_USER_ACCOUNT_REFERENCE =
            "{\"name\":\"" + TEST_USER_ACCOUNT_NAME + "\",\"id\":\"" + TEST_USER_ACCOUNT_ID + "\",\"links\":[]}";
    private static final String TEST_USER_PASSWORD          = "testUserPassword";
    private static final String TEST_USER                   = "testUser";
    private final static String UPDATE_SERVER_URL           = "http://codenvy-stg.com/update";
    private final static String UPDATE_SERVER_REMOTE_NAME   = "update-server";

    private static final String ANOTHER_REMOTE_NAME = "another remote";
    private static final String ANOTHER_REMOTE_URL  = "another remote url";


    private TestedLoginCommand spyCommand;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;
    @Mock
    private PreferencesStorage         mockPreferencesStorage;
    @Mock
    private CommandSession             commandSession;
    @Mock
    private MultiRemoteCodenvy         mockMultiRemoteCodenvy;

    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        doReturn(UPDATE_SERVER_URL).when(mockInstallationManagerProxy).getUpdateServerEndpoint();

        doNothing().when(mockPreferencesStorage).setAccountId(TEST_USER_ACCOUNT_ID);

        spyCommand = spy(new TestedLoginCommand());
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;
        spyCommand.preferencesStorage = mockPreferencesStorage;

        doNothing().when(spyCommand).init();
        doReturn(UPDATE_SERVER_REMOTE_NAME).when(spyCommand).getRemoteNameByUrl(UPDATE_SERVER_URL);
        doReturn(true).when(spyCommand).isRemoteForUpdateServer(UPDATE_SERVER_REMOTE_NAME);
        doReturn(false).when(spyCommand).isRemoteForUpdateServer(ANOTHER_REMOTE_NAME);

        doReturn(UPDATE_SERVER_URL).when(spyCommand).getRemoteUrlByName(UPDATE_SERVER_REMOTE_NAME);
        doReturn(ANOTHER_REMOTE_URL).when(spyCommand).getRemoteUrlByName(ANOTHER_REMOTE_NAME);
    }

    @Test
    public void testLogin() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(Commons.createDtoFromJson(TEST_USER_ACCOUNT_REFERENCE, AccountReference.class))
                .when(spyCommand).getAccountReferenceWhereUserIsOwner(null);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Your Codenvy account '%s' has been obtained and will be used to verify subscription.\n" +
                                           "Login success on remote '%s' [%s] which is used by installation manager commands.\n",
                                           TEST_USER_ACCOUNT_NAME,
                                           UPDATE_SERVER_REMOTE_NAME,
                                           UPDATE_SERVER_URL));
        assertTrue(output.contains(TEST_USER_ACCOUNT_NAME));
    }

    @Test
    public void testLoginFailed() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        // simulate fail login
        doReturn(false).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Login failed on remote '%s'.\n", UPDATE_SERVER_REMOTE_NAME));
    }

    @Test
    public void testLoginFailedNoAppropriateAccountWasFound() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        doReturn(null).when(spyCommand).getAccountReferenceWhereUserIsOwner(null);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, LoginCommand.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG + "\n");
    }

    @Test
    public void testLoginWithAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        commandInvoker.argument("accountName", TEST_USER_ACCOUNT_NAME);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(Commons.createDtoFromJson(TEST_USER_ACCOUNT_REFERENCE, AccountReference.class))
                .when(spyCommand).getAccountReferenceWhereUserIsOwner(TEST_USER_ACCOUNT_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Login success on remote '%s' [%s] which is used by installation manager commands.\n",
                                           UPDATE_SERVER_REMOTE_NAME,
                                           UPDATE_SERVER_URL));
        assertFalse(output.contains(TEST_USER_ACCOUNT_NAME));
    }

    @Test
    public void testLoginFailedWithAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        commandInvoker.argument("accountName", TEST_USER_ACCOUNT_NAME);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(null).when(spyCommand).getAccountReferenceWhereUserIsOwner(TEST_USER_ACCOUNT_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Account '%s' is not yours or may be you aren't owner of this account.\n", TEST_USER_ACCOUNT_NAME));
    }

    @Test
    public void testLoginWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
            .when(mockInstallationManagerProxy).getUpdateServerEndpoint();

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
