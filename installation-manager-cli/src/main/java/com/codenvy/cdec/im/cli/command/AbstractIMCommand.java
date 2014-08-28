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

import com.codenvy.cdec.response.Response;
import com.codenvy.cdec.restlet.InstallationManagerService;
import com.codenvy.cdec.restlet.RestletClientFactory;
import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.security.RemoteCredentials;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.client.Codenvy;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    private String tokenOfUpdateServerRemote;
    
    protected final InstallationManagerService installationManagerProxy;

    public AbstractIMCommand() {
        try {
            installationManagerProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
        } catch (MissingAnnotationException | IllegalPathException | IOException e) {
            throw new IllegalStateException("Can't initialize proxy service");
        }
    }

    @Override
    public void init() {
        super.init();

        if (tokenOfUpdateServerRemote == null) {
            String updateServerRemoteUrl = getUpdateServerUrl();        
            tokenOfUpdateServerRemote = getToken(updateServerRemoteUrl);
        }
    }
    
    protected void printError(Exception ex) {
        try {
            String message = getPrettyPrintingJson(Response.valueOf(ex).toJson());
            System.out.println(ansi().fg(RED).a(message).reset());
        } catch (JSONException e) {
            Ansi ansi = ansi().fg(RED).a("Unexpected error: " + e.getMessage())
                              .newline().a("Suppressed error: " + ex.getMessage())
                              .reset();

            System.out.println(ansi);
        }
    }

    protected void printResult(@Nullable String response) {
        if (response == null) {
            printError(new IllegalArgumentException("Unexpected error occurred. Response is empty"));
        } else {
            try {
                String message = getPrettyPrintingJson(response);
                System.out.println(ansi().a(message));
            } catch (JSONException e) {
                System.out.println(ansi().fg(RED).a("Unexpected error: " + e.getMessage()).reset());
            }
        }
    }

    protected String getToken() {
        return tokenOfUpdateServerRemote;
    }
    
    private String getToken(String remoteUrl) {
        MultiRemoteCodenvy multiRemoteCodenvy = getMultiRemoteCodenvy();

        String productionRemoteName = getRemoteNameByUrl(remoteUrl);
        if (productionRemoteName == null) {
            System.out.println(ansi().fg(RED)
                                     .a(String.format("Please add remote with url '%s' and login to it.", 
                                                      remoteUrl))
                                     .reset());
            return null;
        }
        
        Map<String, Codenvy> readyRemotes = getMultiRemoteCodenvy().getReadyRemotes();
        if (! readyRemotes.containsKey(productionRemoteName)) {
            System.out.println(ansi().fg(RED)
                                     .a(String.format("Please login to remote '%s'.", 
                                                      productionRemoteName))
                                     .reset());
            return null;
        }

        RemoteCredentials credentials = getRemoteCredentials(productionRemoteName);
        return credentials.getToken();
    }
    
    private RemoteCredentials getRemoteCredentials(String remoteName) {
        Preferences globalPreferences = getGlobalPreferences();
        
        if (globalPreferences == null) {
            System.out.println(ansi().fg(RED)
                               .a(String.format("Please login to remote '%s'.", 
                                                remoteName))
                               .reset());
            return null;
        }
        
        Preferences remotesPreferences = globalPreferences.path("remotes");        
        return remotesPreferences.get(remoteName, RemoteCredentials.class);
    }
    
    private String getRemoteNameByUrl(String url) {
        MultiRemoteCodenvy multiRemoteCodenvy = getMultiRemoteCodenvy();
        Map<String, Remote> availableRemotes = multiRemoteCodenvy.getAvailableRemotes();
        
        for(Entry<String, Remote> remoteEntry: availableRemotes.entrySet()) {
            Remote remote = remoteEntry.getValue();
            if(remote.url.equals(url)) {
                return remoteEntry.getKey();
            }
        }
        
        return null;
    }
    
    private Preferences getGlobalPreferences() {
        return (Preferences)session.get(Preferences.class.getName());
    }
    
    private String getUpdateServerUrl() {
        return installationManagerProxy.getUpdateServerUrl();
    }
}