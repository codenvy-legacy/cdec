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
    private final static String UPDATE_SERVER_REMOTE_NAME = "Codenvy Update Server";    
        
    private Preferences globalPreferences;
    private String DEFAULT_PREFERENCES_FILE = getClass().getSimpleName()  + File.separator + "default-preferences.json"; 
    private String PREFERENCES_WITH_UPDATE_SERVER_FILE = getClass().getSimpleName() + "/preferences-with-update-server-remote.json";
    
    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);        
    }

    @Test
    private void testGetAuthToken() {
        globalPreferences = loadPreferencesWithUpdateServerRemote();
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);

        assertEquals(preferencesStorage.getAuthToken(), "authToken");
    }
    
    /**
     * Modifies 'globalPreferences' and PREFERENCES_WITH_UPDATE_SERVER_FILE file content.
     * So this test should perform at the last! 
     */
    @Test(priority=1)
    private void testSetAccountId() {
        globalPreferences = loadPreferencesWithUpdateServerRemote();
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);

        preferencesStorage.setAccountId("testAccountId");        
        assertEquals(preferencesStorage.getAccountId(), "testAccountId");
    }  
    
    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "ID of Codenvy account which is used for subscription is needed.")
    private void testGetAbsentAccountId() {
        globalPreferences = loadPreferencesWithUpdateServerRemote();
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        preferencesStorage.getAccountId();
    }  
    
    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "User didn't login.")
    private void testGetAuthTokenWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferencesWithoutUpdateServerRemote();
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        
        preferencesStorage.getAuthToken();
    }
    
    @Test(expectedExceptions = IllegalStateException.class,
        expectedExceptionsMessageRegExp = "ID of Codenvy account which is used for subscription is needed.")
    private void testGetAccountIdWhenUpdateServerRemoteAbsent() {
        globalPreferences = loadPreferencesWithoutUpdateServerRemote();
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, UPDATE_SERVER_REMOTE_NAME);
        preferencesStorage.getAccountId();
    }
    
    private Preferences loadPreferencesWithoutUpdateServerRemote() {
        File preferencesFile = new File(getClass().getClassLoader().getResource(DEFAULT_PREFERENCES_FILE).getPath()); 
        return PreferencesAPI.getPreferences(preferencesFile.toURI());
    }
    
    private Preferences loadPreferencesWithUpdateServerRemote() {
        File preferencesFile = new File(getClass().getClassLoader().getResource(PREFERENCES_WITH_UPDATE_SERVER_FILE).getPath()); 
        return PreferencesAPI.getPreferences(preferencesFile.toURI());
    }
}
