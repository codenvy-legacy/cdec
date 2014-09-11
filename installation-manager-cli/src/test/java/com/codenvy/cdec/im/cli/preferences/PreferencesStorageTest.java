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
package com.codenvy.cdec.im.cli.preferences;

import java.io.File;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.preferences.PreferencesAPI;

import static org.testng.Assert.*;

/** @author Dmytro Nochevnov */
public class PreferencesStorageTest {
    private final static String UPDATE_SERVER_REMOTE_NAME = "CodenvyUpdateServer";    
        
    private Preferences globalPreferences;
    private String DEFAULT_PREFERENCES_FILE = "default-preferences.json"; 
    private String PREFERENCES_WITH_UPDATE_SERVER_FILE = "preferences-with-update-server-remote.json";
    
    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);        
    }

    @Test
    private void testGetAuthToken() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);

        assertEquals(preferencesStorage.getAuthToken(), "authToken");
    }

    @Test
    private void testGetAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        
        // test getting absent accountId
        try {
            preferencesStorage.getAccountId();
            fail("There should be IllegalStateException because accountId is absent.");
        } catch(IllegalStateException e) {
            assertEquals(e.getMessage(), "ID of Codenvy account which is used for subscription is needed.");
        }
        
        // test setting accotunId. Modifies 'globalPreferences' and PREFERENCES_WITH_UPDATE_SERVER_FILE file content.
        preferencesStorage.setAccountId("testAccountId");        
        assertEquals(preferencesStorage.getAccountId(), "testAccountId");
    }  
    
    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "User didn't login.")
    private void testGetAuthTokenWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        
        preferencesStorage.getAuthToken();
    }
    
    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "ID of Codenvy account which is used for subscription is needed.")
    private void testGetAccountIdWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        preferencesStorage.getAccountId();
    }
    
    private Preferences loadPreferences(String preferencesFilePath) {
        File preferencesFile = new File(getClass().getClassLoader().getResource(preferencesFilePath).getPath()); 
        return PreferencesAPI.getPreferences(preferencesFile.toURI());
    }
}
