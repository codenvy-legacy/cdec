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

import static com.codenvy.cdec.artifacts.ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY;
import static com.codenvy.cdec.artifacts.ArtifactProperties.SUBSCRIPTION_PROPERTY;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.user.UserCredentials;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
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
    
    @Mock
    private MultiRemoteCodenvy mockMultiRemoteCodenvy;
    
    private final static String UPDATE_SERVER_REMOTE_NAME = "CodenvyUpdateServer";
    private final static String UPDATE_SERVER_URL = "https://test.com";
    
    private String DEFAULT_PREFERENCES_FILE = "default-preferences.json"; 
    private String PREFERENCES_WITH_UPDATE_SERVER_FILE = "preferences-with-update-server-remote.json";
    
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
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        assertEquals(spyCommand.getRemoteNameForUpdateServer(), UPDATE_SERVER_REMOTE_NAME);
    }
    
    @Test
    public void testValidateIfUserLoggedIn() {
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
     
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, null);
        }}).when(mockMultiRemoteCodenvy).getReadyRemotes();
        
        spyCommand.validateIfUserLoggedIn();
    }

    @Test(expectedExceptions=IllegalStateException.class,expectedExceptionsMessageRegExp="Please login using im:login command.")
    public void testValidateIfUserLoggedInWhenUserDidnotLogin() {
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        doReturn(new HashMap()).when(mockMultiRemoteCodenvy).getReadyRemotes();
        
        spyCommand.validateIfUserLoggedIn();
    }
    
    @Test
    public void testInit() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        
        prepareTestAbstractIMCommand(spyCommand);
        
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, null);
        }}).when(mockMultiRemoteCodenvy).getReadyRemotes();
        
        spyCommand.init();
        
        assertNotNull(spyCommand.preferencesStorage);
        assertEquals(spyCommand.preferencesStorage.getAuthToken(), "authToken");
    }
    
    @Test(expectedExceptions=IllegalStateException.class,expectedExceptionsMessageRegExp="Please login using im:login command.")
    public void testInitWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        
        prepareTestAbstractIMCommand(spyCommand);
        
        doReturn(new HashMap()).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        spyCommand.init();
    }
    
    @Test(expectedExceptions=IllegalStateException.class,expectedExceptionsMessageRegExp="Please login using im:login command.")
    public void testInitWhenUserDidnotLogin() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        
        prepareTestAbstractIMCommand(spyCommand);
        
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        doReturn(new HashMap()).when(mockMultiRemoteCodenvy).getReadyRemotes();
        
        spyCommand.init();
    }
    
    /**
     * Modifies 'globalPreferences' and PREFERENCES_WITH_UPDATE_SERVER_FILE file content.
     * So this test should perform at the last! 
     */
    @Test(priority=1)
    public void testGetCredentialsRep() throws IOException {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        
        prepareTestAbstractIMCommand(spyCommand);
        
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, updateServerRemote);
        }}).when(mockMultiRemoteCodenvy).getAvailableRemotes();
        
        doReturn(new HashMap() {{
            put(UPDATE_SERVER_REMOTE_NAME, null);
        }}).when(mockMultiRemoteCodenvy).getReadyRemotes();
        
        spyCommand.init();
        
        spyCommand.preferencesStorage.setAccountId("testAccountId");
        
        JacksonRepresentation<UserCredentials> testCredentialsRep = spyCommand.getCredentialsRep();
        UserCredentials testCreadentials = testCredentialsRep.getObject();
        
        assertEquals(testCreadentials.getToken(), "authToken");
        assertEquals(testCreadentials.getAccountId(), "testAccountId");
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
        
        @Override
        protected void setCodenvyClient(CodenvyClient codenvyClient) {
            super.setCodenvyClient(codenvyClient);
        }

        protected void setSession(CommandSession session) {
            this.session = session;
        }
        
        @Override
        protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
            return mockMultiRemoteCodenvy;
        }
    }
}
