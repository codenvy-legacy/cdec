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

import java.util.Map;
import java.util.Map.Entry;
import java.net.ConnectException;

import com.codenvy.cdec.im.cli.preferences.PreferencesStorage;
import com.codenvy.cdec.response.Response;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.restlet.RestletClientFactory;
import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.client.Codenvy;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected final InstallationManagerService installationManagerProxy;
    protected PreferencesStorage preferencesStorage;
    
    private static final String DEFAULT_UPDATE_SERVER_REMOTE_NAME = "updateServer";

    public AbstractIMCommand() {
        try {
            installationManagerProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
        } catch (MissingAnnotationException | IllegalPathException e) {
            throw new IllegalStateException("Can't initialize proxy service", e);
        }
    }

    @Override
    public void init() {
        super.init();
        
        String remote = getUpdateServerRemote();
        Map<String, Codenvy> readyRemotes = getMultiRemoteCodenvy().getReadyRemotes();
        if (!readyRemotes.containsKey(remote) && 
            !(this instanceof LoginCommand)) {
            throw new IllegalStateException("Please login using im:login command.");
        }
        
        preferencesStorage = new PreferencesStorage(getGlobalPreferences(),
                                                    getUpdateServerRemote());
    }
    
    protected void printError(Exception ex) {
        try {
            if (isConnectionException(ex)) {
                printError("It is impossible to connect to installation manager server.");
            } else {
                printError(getPrettyPrintingJson(Response.valueOf(ex).toJson()));                
            }
        } catch (JSONException e) {
            Ansi ansi = ansi().fg(RED)
                              .a("Unexpected error: " + e.getMessage())
                              .newline()
                              .a("Suppressed error: " + ex.getMessage())
                              .reset();
            System.out.println(ansi);
        }
    }

    private boolean isConnectionException(Exception e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getClass().getCanonicalName().equals(ConnectException.class.getCanonicalName());
    }
    
    protected void printError(String message) {
        System.out.println(ansi().fg(RED).a(message).reset());
    }

    protected void printResult(@Nullable String response) {
        if (response == null) {
            printError(new IllegalArgumentException("Unexpected error occurred. Response is empty"));
        } else {
            try {
                String message = getPrettyPrintingJson(response);
                System.out.println(ansi().a(message));
            } catch (JSONException e) {
                printError("Unexpected error: " + e.getMessage());
            }
        }
    }
    
    /** Find out or add remote for update server */
    @Nonnull
    protected String getUpdateServerRemote() {
        String updateServerUrl = getUpdateServerUrl();
        String remote = getRemote(updateServerUrl);

        // create remote for update sever if it is absent
        if (remote == null) {
            if (!getMultiRemoteCodenvy().addRemote(DEFAULT_UPDATE_SERVER_REMOTE_NAME, updateServerUrl)) {
                throw new IllegalStateException(String.format("It was impossible to add remote. Please add remote with url '%s' manually.", 
                                                              updateServerUrl));
            }
            
            return DEFAULT_UPDATE_SERVER_REMOTE_NAME;
        }        

        return remote;
    }

    protected String getUpdateServerUrl() {
        return installationManagerProxy.getUpdateServerUrl();
    }
    
    @Nonnull
    private Preferences getGlobalPreferences() {
        return (Preferences)session.get(Preferences.class.getName());
    }
    
    @Nullable
    private String getRemote(String url) throws IllegalStateException {
        Map<String, Remote> availableRemotes = getMultiRemoteCodenvy().getAvailableRemotes();

        for (Entry<String, Remote> remoteEntry : availableRemotes.entrySet()) {
            Remote remote = remoteEntry.getValue();
            if (remote.url.equalsIgnoreCase(url)) {
                return remoteEntry.getKey();
            }
        }
        
        return null;
    }
}