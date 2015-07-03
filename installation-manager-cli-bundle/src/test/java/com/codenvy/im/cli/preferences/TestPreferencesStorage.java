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
package com.codenvy.im.cli.preferences;

import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.preferences.PreferencesAPI;
import com.google.common.io.Files;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/** @author Dmytro Nochevnov */
public class TestPreferencesStorage {
    private final static String SAAS_SERVER_REMOTE_NAME = "saas-server";
    private final static String TEST_TOKEN              = "authToken";
    private final static String TEST_ACCOUNT_ID         = "test-account-id";

    private Preferences globalPreferences;
    private final static String DEFAULT_PREFERENCES_FILE          = "default-preferences.json";
    private final static String PREFERENCES_WITH_SAAS_SERVER_FILE = "preferences-with-saas-server-remote.json";


    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    private void testGetAuthToken() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, SAAS_SERVER_REMOTE_NAME);

        assertEquals(preferencesStorage.getAuthToken(), TEST_TOKEN);
    }

    @Test
    private void testGetPreferencesWhenSaasServerRemoteAbsent() {
        globalPreferences = loadPreferences(DEFAULT_PREFERENCES_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, SAAS_SERVER_REMOTE_NAME);
        assertNull(preferencesStorage.getAccountId());
        assertNull(preferencesStorage.getAuthToken());
    }

    @Test
    private void testGetAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, SAAS_SERVER_REMOTE_NAME);

        assertEquals(preferencesStorage.getAccountId(), TEST_ACCOUNT_ID);
    }

    @Test
    private void testSetAccountId() {
        globalPreferences = loadPreferences(PREFERENCES_WITH_SAAS_SERVER_FILE);
        PreferencesStorage preferencesStorage = new PreferencesStorage(globalPreferences, SAAS_SERVER_REMOTE_NAME);

        preferencesStorage.setAccountId("testAccountId");
        assertEquals(preferencesStorage.getAccountId(), "testAccountId");
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
}
