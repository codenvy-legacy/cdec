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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.security.RemoteCredentials;
import com.codenvy.client.Codenvy;

/** @author Dmytro Nochevnov */
public class PreferencesStorage {
    private MultiRemoteCodenvy multiRemoteCodenvy;
    private Preferences globalPreferences;
    private String updateServerUrl;
    private final AtomicReference<String> authToken;    
    
    public PreferencesStorage(MultiRemoteCodenvy multiRemoteCodenvy, Preferences globalPreferences, String updateServerUrl) {
        this.multiRemoteCodenvy = multiRemoteCodenvy;
        this.globalPreferences = globalPreferences;
        this.updateServerUrl = updateServerUrl;
        
        authToken = new AtomicReference<>();
    }

    @Nonnull
    /** @return authentication token to update server */
    public String getAuthToken() throws IllegalStateException {
        if (authToken.get() == null) {
            synchronized (authToken) {
                if (authToken.get() == null) {
                    authToken.set(doGetToken());
                }
            }
        }

        return authToken.get();
    }
    
    @Nullable
    public String getAccountId() {        
        SubscriptionPreferences accountDescription = readPreference(SubscriptionPreferences.class);
        if (accountDescription == null || accountDescription.getAccountId() == null) {
            throw new IllegalStateException("ID of Codenvy account which is used for subscription is needed.");
        }

        return accountDescription.getAccountId(); 
    }
    
    public void setAccountId(String accountId) {
        SubscriptionPreferences accountDescription = new SubscriptionPreferences();
        accountDescription.setAccountId(accountId);
        
        writePreference(accountDescription);
    }
    
    @Nonnull
    private String doGetToken() throws IllegalStateException {
        RemoteCredentials credentials = readPreference(RemoteCredentials.class);
        return credentials.getToken(); // TODO outdated after 3h ?
    }
    
    @Nonnull
    private String getUpdateServerRemote() {
        String remote = getRemote(updateServerUrl);

        Map<String, Codenvy> readyRemotes = multiRemoteCodenvy.getReadyRemotes();
        if (!readyRemotes.containsKey(remote)) {
            throw new IllegalStateException(String.format("Please login to remote '%s'.", remote));
        }
        return remote;
    }

    @Nonnull
    private String getRemote(String url) throws IllegalStateException {
        Map<String, Remote> availableRemotes = multiRemoteCodenvy.getAvailableRemotes();

        for (Entry<String, Remote> remoteEntry : availableRemotes.entrySet()) {
            Remote remote = remoteEntry.getValue();
            if (remote.url.equalsIgnoreCase(url)) {
                return remoteEntry.getKey();
            }
        }

        throw new IllegalStateException(String.format("Please add remote with url '%s' and login to it.", url));
    }

    private <T> T readPreference(Class<T> clazz) {
        String remote = getUpdateServerRemote(); 
        return globalPreferences.path("remotes").get(remote, clazz);
    }
    
    private <T> void writePreference(T preference) {
        String remote = getUpdateServerRemote();
        globalPreferences.path("remotes").merge(remote, preference);
    }
}