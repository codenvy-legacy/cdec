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
package com.codenvy.cdec.im.cli.command;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.user.UserCredentials;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.preferences.PreferencesAPI;
import com.codenvy.client.CodenvyClient;
import com.codenvy.client.dummy.DummyCodenvyClient;

/** @author Dmytro Nochevnov */
public class AbstractIMCommandTest {
    private TestAbstractIMCommand spyCommand;
    private Preferences globalPreferences;

    @Mock
    private InstallationManagerService mockInstallationManagerProxy;

    @Mock
    private CommandSession session;
    
    private final static String UPDATE_SERVER_REMOTE_NAME = "CodenvyUpdateServer";
    private final static String UPDATE_SERVER_URL = "https://test.com";
    
    private String DEFAULT_PREFERENCES_FILE = "default-preferences.json"; 
    private String PREFERENCES_WITH_UPDATE_SERVER_FILE = "preferences-with-update-server-remote.json";
    private String PREFERENCES_WITH_UPDATE_SERVER_WITHOUT_LOGIN_FILE = "preferences-with-update-server-remote-without-login.json";
    
    private Remote updateServerRemote;
    
    @BeforeMethod
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new TestAbstractIMCommand());        
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;
        
        updateServerRemote = new Remote();
        updateServerRemote.setUrl(UPDATE_SERVER_URL);
        
        doReturn(UPDATE_SERVER_URL).when(mockInstallationManagerProxy).getUpdateServerUrl();
    }

    @Test
    public void testGetUpdateServerUrl() {
        assertEquals(spyCommand.getUpdateServerUrl(), UPDATE_SERVER_URL);
    }
    
    @Test
    public void testGetRemoteNameForUpdateServer() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);        
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        
        assertEquals(spyCommand.getRemoteNameForUpdateServer(), UPDATE_SERVER_REMOTE_NAME);
    }
    
    @Test
    public void testValidateIfUserLoggedIn() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);        
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        
        spyCommand.validateIfUserLoggedIn();
    }
        
    @Test
    public void testInit() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);        
        prepareTestAbstractIMCommand(spyCommand);        
        spyCommand.init();
        
        assertNotNull(spyCommand.preferencesStorage);
        assertEquals(spyCommand.preferencesStorage.getAuthToken(), "authToken");
    }
    
    @Test(expectedExceptions=IllegalStateException.class,expectedExceptionsMessageRegExp="Please login using im:login command.")
    public void testInitWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);       
        prepareTestAbstractIMCommand(spyCommand);        
        spyCommand.init();
    }
    
    @Test(expectedExceptions=IllegalStateException.class,expectedExceptionsMessageRegExp="Please login using im:login command.")
    public void testInitWhenUserDidnotLogin() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_WITHOUT_LOGIN_FILE);        
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
    }
    
    /**
     * Modifies 'globalPreferences' and PREFERENCES_WITH_UPDATE_SERVER_FILE file content.
     * So this test should perform at the last when DEFAULT_PREFERENCES_FILE is being used! 
     */
    @Test(priority=1)
    public void testGetCredentialsRep() throws IOException {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        prepareTestAbstractIMCommand(spyCommand);
        spyCommand.init();
        
        spyCommand.preferencesStorage.setAccountId("testAccountId");
        
        JacksonRepresentation<UserCredentials> testCredentialsRep = spyCommand.getCredentialsRep();
        UserCredentials testCreadentials = testCredentialsRep.getObject();
        
        assertEquals(testCreadentials.getToken(), "authToken");
        assertEquals(testCreadentials.getAccountId(), "testAccountId");
    }
    
    /**
     * Modifies 'globalPreferences' and DEFAULT_PREFERENCES_FILE file content.
     * So this test should perform at the last when DEFAULT_PREFERENCES_FILE is being used! 
     */
    @Test(priority=1)
    public void testCreateUpdateServerRemote() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);        
        prepareTestAbstractIMCommand(spyCommand);
        doNothing().when(spyCommand).validateIfUserLoggedIn();
        spyCommand.init();
        
        assertNull(spyCommand.getRemoteNameForUpdateServer());
        
        spyCommand.createUpdateServerRemote(UPDATE_SERVER_URL);
        assertNotNull(spyCommand.getRemoteNameForUpdateServer());
    }
    
    private void prepareTestAbstractIMCommand(TestAbstractIMCommand command) {
        doReturn(globalPreferences).when(session).get(Preferences.class.getName());
        
        DummyCodenvyClient codenvyClient = new DummyCodenvyClient();
        doReturn(codenvyClient).when(session).get(DummyCodenvyClient.class.getName());                
        command.setCodenvyClient(codenvyClient);
        command.setSession(session);
    }
    
    private Preferences loadPreferences(String preferencesFilePath) {
        File preferencesFile = new File(getClass().getClassLoader().getResource(preferencesFilePath).getPath()); 
        return PreferencesAPI.getPreferences(preferencesFile.toURI());
    }
    
    class TestAbstractIMCommand extends AbstractIMCommand {        
        @Override
        protected Object doExecute() throws Exception {
            return null;
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
    }
}