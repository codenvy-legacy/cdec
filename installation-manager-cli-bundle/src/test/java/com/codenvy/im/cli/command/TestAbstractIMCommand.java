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
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.preferences.PreferencesAPI;
import com.codenvy.client.CodenvyClient;
import com.codenvy.client.dummy.DummyCodenvyClient;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.google.common.io.Files;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public class TestAbstractIMCommand {
    private TestedAbstractIMCommand spyCommand;
    private Preferences             globalPreferences;

    @Mock
    private IMArtifactLabeledFacade service;
    @Mock
    private CommandSession          session;
    @Mock
    private MultiRemoteCodenvy      mockMultiRemoteCodenvy;
    @Mock
    private Remote mockSaasServerRemote;
    @Mock
    private Remote mockAnotherRemote;

    private final static String SAAS_SERVER_REMOTE_NAME = "saas-server";
    private final static String SAAS_SERVER_URL         = "https://test.com";
    private final static String TEST_ACCOUNT_ID         = "test-account-id";
    private static final String TEST_ACCOUNT_NAME       = "test-account-name";
    private static final String TEST_ACCOUNT_REFERENCE  =
        "{\"name\":\"" + TEST_ACCOUNT_NAME + "\",\"id\":\"" + TEST_ACCOUNT_ID + "\",\"links\":[]}";
    private final static String TEST_TOKEN              = "authToken";

    private static final String ANOTHER_REMOTE_NAME = "another remote";
    private static final String ANOTHER_REMOTE_URL  = "another remote url";

    private String DEFAULT_PREFERENCES_FILE                          = "default-preferences.json";
    private String PREFERENCES_WITH_SAAS_SERVER_FILE                 = "preferences-with-saas-server-remote.json";
    private String PREFERENCES_WITH_SAAS_SERVER_WITHOUT_LOGIN_FILE = "preferences-with-saas-server-remote-without-login.json";
    private String PREFERENCES_SAAS_SERVER_WITHOUT_ACCOUNT_ID_FILE = "preferences-with-saas-server-remote-without-accountid.json";

    private Remote saasServerRemote;

    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new TestedAbstractIMCommand());
        spyCommand.facade = service;
        doReturn(true).when(spyCommand).isInteractive();
        doNothing().when(spyCommand).initConsole();

        saasServerRemote = new Remote();
        saasServerRemote.setUrl(SAAS_SERVER_URL);

        doReturn(SAAS_SERVER_URL).when(service).getSaasServerEndpoint();
    }

    @Test
    public void testGetSaasServerUrl() {
        assertEquals(spyCommand.getSaasServerEndpoint(), SAAS_SERVER_URL);
    }

    @Test
    public void testGetAccountId() throws Exception {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();

        doReturn(Commons.createDtoFromJson(TEST_ACCOUNT_REFERENCE, AccountReference.class))
            .when(service).getAccountWhereUserIsOwner("accountName", TEST_TOKEN);

        AccountReference accountReference = spyCommand.getAccountReferenceWhereUserIsOwner("accountName");
        assertNotNull(accountReference);
        assertEquals(accountReference.getId(), TEST_ACCOUNT_ID);
        assertEquals(accountReference.getName(), TEST_ACCOUNT_NAME);
    }

    @Test
    public void testGetRemoteNameForSaasServer() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();

        assertEquals(spyCommand.getOrCreateRemoteNameForSaasServer(), SAAS_SERVER_REMOTE_NAME);
    }

    @Test
    public void testValidateIfUserLoggedIn() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();

        spyCommand.validateIfUserLoggedIn();
    }

    @Test
    public void testCreateSaasServerRemote() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        doNothing().when(spyCommand).validateIfUserLoggedIn();
        spyCommand.init();

        assertNotNull(spyCommand.getOrCreateRemoteNameForSaasServer());
    }

    @Test
    public void testInit() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();

        assertNotNull(spyCommand.preferencesStorage);
        assertEquals(spyCommand.preferencesStorage.getAuthToken(), TEST_TOKEN);
        assertEquals(spyCommand.preferencesStorage.getAccountId(), TEST_ACCOUNT_ID);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Please log in into 'saas-server' remote.")
    public void testInitWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        spyCommand.validateIfUserLoggedIn();
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Please log in into 'saas-server' remote.")
    public void testInitWhenUserDidNotLogin() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_WITHOUT_LOGIN_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        spyCommand.validateIfUserLoggedIn();
    }

    @Test(expectedExceptions = IllegalStateException.class,
            expectedExceptionsMessageRegExp = "Please log in into 'saas-server' remote.")
    public void testInitWhenUserDidNotObtainAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_SAAS_SERVER_WITHOUT_ACCOUNT_ID_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        spyCommand.validateIfUserLoggedIn();
    }

    @Test
    public void testGetCredentialsRep() throws IOException {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();

        spyCommand.preferencesStorage.setAccountId("testAccountId");

        SaasUserCredentials credentials = spyCommand.getCredentials();

        assertEquals(credentials.getToken(), TEST_TOKEN);
        assertEquals(credentials.getAccountId(), "testAccountId");
    }

    @Test
    public void testIsRemoteForUpdateServer() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        doNothing().when(spyCommand).validateIfUserLoggedIn();
        spyCommand.init();

        String remoteNameForUpdateServer = spyCommand.getOrCreateRemoteNameForSaasServer();
        assertTrue(spyCommand.isRemoteForSaasServer(remoteNameForUpdateServer));
        assertFalse(spyCommand.isRemoteForSaasServer("another remote"));
    }

    @Test
    public void testGetRemoteUrlByName() {
        doReturn(mockMultiRemoteCodenvy).when(spyCommand).getMultiRemoteCodenvy();

        doReturn(SAAS_SERVER_URL).when(mockSaasServerRemote).getUrl();
        doReturn(mockSaasServerRemote).when(mockMultiRemoteCodenvy).getRemote(SAAS_SERVER_REMOTE_NAME);
        assertEquals(spyCommand.getRemoteUrlByName(SAAS_SERVER_REMOTE_NAME), SAAS_SERVER_URL);

        doReturn(ANOTHER_REMOTE_URL).when(mockAnotherRemote).getUrl();
        doReturn(mockAnotherRemote).when(mockMultiRemoteCodenvy).getRemote(ANOTHER_REMOTE_NAME);
        assertEquals(spyCommand.getRemoteUrlByName(ANOTHER_REMOTE_NAME), ANOTHER_REMOTE_URL);
    }

    private void prepareTestAbstractIMCommand(TestedAbstractIMCommand command) {
        doReturn(globalPreferences).when(session).get(Preferences.class.getName());

        DummyCodenvyClient codenvyClient = new DummyCodenvyClient();
        doReturn(codenvyClient).when(session).get(DummyCodenvyClient.class.getName());
        command.setCodenvyClient(codenvyClient);
        command.setSession(session);
    }

    private Preferences loadPreferences(String preferencesFileRelativePath) {
        String preferencesFileFullPath = getClass().getClassLoader().getResource(preferencesFileRelativePath).getPath();
        String tempPreferencesFileFullPath = preferencesFileFullPath + ".temp";
        File preferencesFile = new File(preferencesFileFullPath);
        File tempPreferencesFile = new File(tempPreferencesFileFullPath);

        try {
            Files.copy(preferencesFile, tempPreferencesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return PreferencesAPI.getPreferences(tempPreferencesFile.toURI());
    }

    class TestedAbstractIMCommand extends AbstractIMCommand {
        @Override
        protected Void execute() throws Exception {
            return null;
        }

        @Override
        protected void doExecuteCommand() throws Exception {
        }

        /** is needed for prepareTestAbstractIMCommand() method */
        @Override
        protected void setCodenvyClient(CodenvyClient codenvyClient) {
            super.setCodenvyClient(codenvyClient);
        }

        /** is needed for prepareTestAbstractIMCommand() method */
        protected void setSession(CommandSession session) {
            this.session = session;
        }

        protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
            return super.getMultiRemoteCodenvy();
        }
    }
}
