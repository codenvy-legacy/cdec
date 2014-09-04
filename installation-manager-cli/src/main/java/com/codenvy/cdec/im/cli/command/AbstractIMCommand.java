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
import com.codenvy.cdec.im.cli.preferences.SubscriptionPreferences;
import com.codenvy.cdec.im.cli.preferences.PreferencesStorage;
import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.security.RemoteCredentials;
import com.codenvy.client.Codenvy;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected final InstallationManagerService installationManagerProxy;
    private PreferencesStorage utils;

    public AbstractIMCommand() {
        try {
            installationManagerProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
        } catch (MissingAnnotationException | IllegalPathException e) {
            throw new IllegalStateException("Can't initialize proxy service", e);
        }
    }

    private PreferencesStorage getUtils() {
        if (utils == null) {
            MultiRemoteCodenvy multiRemoteCodenvy = getMultiRemoteCodenvy();
            String updateServerUrl = getUpdateServerUrl();
            Preferences globalPreferences = getGlobalPreferences();
            utils = new PreferencesStorage(multiRemoteCodenvy, globalPreferences, updateServerUrl);
        }
        
        return utils;
    }
    
    @Nonnull
    private Preferences getGlobalPreferences() {
        return (Preferences)session.get(Preferences.class.getName());
    }
    
    protected void printError(Exception ex) {
        try {
            printError(getPrettyPrintingJson(Response.valueOf(ex).toJson()));
        } catch (JSONException e) {
            Ansi ansi = ansi().fg(RED)
                              .a("Unexpected error: " + e.getMessage())
                              .newline()
                              .a("Suppressed error: " + ex.getMessage())
                              .reset();
            System.out.println(ansi);
        }
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
    
    private String getUpdateServerUrl() {
        return installationManagerProxy.getUpdateServerUrl();
    }
    
    @Nonnull
    /** @return authentication token to update server */
    protected String getAuthToken() throws IllegalStateException {
        return getUtils().getAuthToken();
    }
    
    protected String getAccountId() {
        return getUtils().getAccountId(); 
    }
    
    protected void setAccountId(String accountId) {
        getUtils().setAccountId(accountId);
    }
}