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
package com.codenvy.im.cli.command;

import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestLoginCommand extends AbstractTestCommand {
    private static final String TEST_USER_ACCOUNT_ID        = "testUserAccountId";
    private static final String TEST_USER_ACCOUNT_NAME      = "testUserAccountName";
    private static final String TEST_USER_ACCOUNT_REFERENCE =
            "{\"name\":\"" + TEST_USER_ACCOUNT_NAME + "\",\"id\":\"" + TEST_USER_ACCOUNT_ID + "\",\"links\":[]}";
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

        doNothing().when(mockPreferencesStorage).setAccountId(TEST_USER_ACCOUNT_ID);

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

        doNothing().when(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(Commons.createDtoFromJson(TEST_USER_ACCOUNT_REFERENCE, AccountReference.class))
            .when(spyCommand).getAccountReferenceWhereUserIsOwner(null);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Your Codenvy account '%s' will be used to verify on-premises subscription.\n" +
                                           "Login success.\n",
                                           TEST_USER_ACCOUNT_NAME));
        assertTrue(output.contains(TEST_USER_ACCOUNT_NAME));
        verify(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
    }

    @Test
    public void testLoginSuccessIfAddTrialSubscriptionFailed() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doThrow(IOException.class).when(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(Commons.createDtoFromJson(TEST_USER_ACCOUNT_REFERENCE, AccountReference.class))
                .when(spyCommand).getAccountReferenceWhereUserIsOwner(null);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Your Codenvy account 'testUserAccountName' will be used to verify on-premises subscription.\n" +
                             "Login success.\n" +
                             "{\n" +
                             "  \"status\" : \"ERROR\"\n" +
                             "}\n");
        assertTrue(output.contains(TEST_USER_ACCOUNT_NAME));
        verify(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
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
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
    }

    @Test
    public void testLoginFailedNoAppropriateAccountWasFound() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        doReturn(null).when(spyCommand).getAccountReferenceWhereUserIsOwner(null);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, SaasAccountServiceProxy.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG + "\n");
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
    }

    @Test
    public void testLoginWithAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        commandInvoker.argument("accountName", TEST_USER_ACCOUNT_NAME);

        doNothing().when(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(Commons.createDtoFromJson(TEST_USER_ACCOUNT_REFERENCE, AccountReference.class))
                .when(spyCommand).getAccountReferenceWhereUserIsOwner(TEST_USER_ACCOUNT_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Login success.\n");
        assertFalse(output.contains(TEST_USER_ACCOUNT_NAME));

        verify(service).addTrialSaasSubscription(any(SaasUserCredentials.class));
    }

    @Test
    public void testLoginFailedWithAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        commandInvoker.argument("accountName", TEST_USER_ACCOUNT_NAME);

        doReturn(true).when(mockMultiRemoteCodenvy).login(SAAS_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(null).when(spyCommand).getAccountReferenceWhereUserIsOwner(TEST_USER_ACCOUNT_NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, String.format("Account '%s' is not yours or may be you aren't owner of this account.\n", TEST_USER_ACCOUNT_NAME));
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
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
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
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
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
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
        verify(service, never()).addTrialSaasSubscription(any(SaasUserCredentials.class));
    }

    class TestedLoginCommand extends LoginCommand {
        protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
            return mockMultiRemoteCodenvy;
        }
    }
}
