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
public class LoginCommandTest {    
    private static final String TEST_USER_ACCOUNT_ID      = "testUserAccountId";
    private static final String TEST_USER_PASSWORD        = "testUserPassword";
    private static final String TEST_USER                 = "testUser";    
    private final static String UPDATE_SERVER_URL         = "http://codenvy-stg.com/update";
    private final static String UPDATE_SERVER_REMOTE_NAME = "Codenvy Update Server"; 
    
    private TestLoginCommand spyCommand;
    
    @Mock private InstallationManagerService mockInstallationManagerProxy;
    @Mock private PreferencesStorage mockPreferencesStorage;
    @Mock private CommandSession commandSession;
    @Mock private MultiRemoteCodenvy mockMultiRemoteCodenvy;
        
    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        doReturn(UPDATE_SERVER_URL).when(mockInstallationManagerProxy).getUpdateServerEndpoint();
        
        doNothing().when(mockPreferencesStorage).setAccountId(TEST_USER_ACCOUNT_ID);
        
        spyCommand = spy(new TestLoginCommand());        
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;
        spyCommand.preferencesStorage = mockPreferencesStorage;
                
        doNothing().when(spyCommand).init();
        doReturn(mockMultiRemoteCodenvy).when(spyCommand).getMultiRemoteCodenvy();
        doReturn(UPDATE_SERVER_REMOTE_NAME).when(spyCommand).getOrCreateRemoteNameForUpdateServer();
        doReturn(UPDATE_SERVER_REMOTE_NAME).when(spyCommand).getRemoteNameByUrl(UPDATE_SERVER_URL);
    }
    
    @Test
    public void testLogin() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        
        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(TEST_USER_ACCOUNT_ID).when(spyCommand).getAccountIdWhereUserIsOwner();

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains("Login succeeded."));
        assertTrue(output.contains(TEST_USER_ACCOUNT_ID));
    }

    @Test
    public void testLoginWithAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);
        commandInvoker.argument("accountId", TEST_USER_ACCOUNT_ID);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);
        doReturn(true).when(spyCommand).isValidAccount();

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains("Login succeeded."));
        assertFalse(output.contains(TEST_USER_ACCOUNT_ID));
    }

    @Test
    public void testFailLogin() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", "user");
        commandInvoker.argument("password", "passwd");
        
        doReturn(false).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, "user", "passwd");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains("Login failed."));
    }

    @Test
    public void testFailGetAccountId() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("username", TEST_USER);
        commandInvoker.argument("password", TEST_USER_PASSWORD);

        doReturn(true).when(mockMultiRemoteCodenvy).login(UPDATE_SERVER_REMOTE_NAME, TEST_USER, TEST_USER_PASSWORD);

        doReturn("").when(spyCommand).getAccountIdWhereUserIsOwner();

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertTrue(output.contains(LoginCommand.CANNOT_RECOGNISE_ACCOUNT_ID_MSG));
    }

    @Test
    public void testLoginWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{"
                                  + "message: \"Server Error Exception\","
                                  + "status: \"ERROR\""
                                  + "}";
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
            .when(mockInstallationManagerProxy).getUpdateServerEndpoint();
        
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, Commons.getPrettyPrintingJson(expectedOutput) + "\n");
    }

    @Test
    public void testLoginToSpecificRemote() {
        // TODO
    }

    @Test
    public void testFailLoginToSpecificRemote() {
        // TODO
    }

    static class TestLoginCommand extends LoginCommand {
        protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
            return super.getMultiRemoteCodenvy();
        }
    }
}
