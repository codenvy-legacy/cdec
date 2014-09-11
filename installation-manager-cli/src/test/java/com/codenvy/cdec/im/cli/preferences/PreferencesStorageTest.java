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
    
    /**
     * Modifies 'globalPreferences' and PREFERENCES_WITH_UPDATE_SERVER_FILE file content.
     * So this test should perform at the last! 
     */
    @Test(priority=1)
    private void testSetAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);

        preferencesStorage.setAccountId("testAccountId");        
        assertEquals(preferencesStorage.getAccountId(), "testAccountId");
    }  
    
//    @Test(expectedExceptions = IllegalStateException.class,
//          expectedExceptionsMessageRegExp = "ID of Codenvy account which is used for subscription is needed.")
    @Test
    private void testGetAbsentAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_UPDATE_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        
        String accountId = null;
        try {
            accountId = preferencesStorage.getAccountId();
        } catch(IllegalStateException e) {
            assertEquals(e.getMessage(), "ID of Codenvy account which is used for subscription is needed.");
            return;
        }
        
        fail(accountId);
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
