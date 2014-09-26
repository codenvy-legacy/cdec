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
package com.codenvy.im.cli.command;

import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.client.Codenvy;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.response.Response;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.restlet.RestletClientFactory;
import com.codenvy.im.user.UserCredentials;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.ConnectException;
import java.util.Map;
import java.util.Map.Entry;

import static com.codenvy.im.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected InstallationManagerService installationManagerProxy;
    protected PreferencesStorage preferencesStorage;

    private static final String DEFAULT_UPDATE_SERVER_REMOTE_NAME = "Codenvy Update Server";

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
        validateIfUserLoggedIn();

        preferencesStorage = new PreferencesStorage(getGlobalPreferences(), getRemoteNameForUpdateServer());
    }


    /**
     * @throws IllegalStateException
     *         if user isn't logged in
     */
    protected void validateIfUserLoggedIn() throws IllegalStateException {
        String remoteName = getRemoteNameForUpdateServer();

        Map<String, Codenvy> readyRemotes = getMultiRemoteCodenvy().getReadyRemotes();
        if (!readyRemotes.containsKey(remoteName)) {
            throw new IllegalStateException("Please login using im:login command.");
        }
    }

    protected void printLineSeparator() {
        System.out.println(System.lineSeparator());
    }

    protected void printError(Exception ex) {
        try {
            if (isConnectionException(ex)) {
                printError("It is impossible to connect to Installation Manager Service. It might be stopped or it is starting up right now, " +
                           "please retry a bit later.");
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

    protected void printInfo(String message) {
        System.out.print(message);
        System.out.flush();
    }

    protected void printSuccess(String message) {
        System.out.println(ansi().fg(GREEN).a(message).reset());
    }

    protected void printResponse(@Nullable String response) {
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

    /** Find out remote for update server */
    protected String getRemoteNameForUpdateServer() {
        String updateServerUrl = getUpdateServerUrl();
        return getRemoteNameByUrl(updateServerUrl);
    }

    protected String getUpdateServerUrl() {
        return installationManagerProxy.getUpdateServerUrl();
    }

    protected JacksonRepresentation<UserCredentials> getCredentialsRep() {
        UserCredentials userCredentials = new UserCredentials(preferencesStorage.getAuthToken(), preferencesStorage.getAccountId());
        return new JacksonRepresentation<>(userCredentials);
    }
    
    @Nonnull
    private Preferences getGlobalPreferences() {
        return (Preferences)session.get(Preferences.class.getName());
    }

    private String getRemoteNameByUrl(String url) throws IllegalStateException {
        Map<String, Remote> availableRemotes = getMultiRemoteCodenvy().getAvailableRemotes();

        for (Entry<String, Remote> remoteEntry : availableRemotes.entrySet()) {
            Remote remote = remoteEntry.getValue();
            if (remote.url.equalsIgnoreCase(url)) {
                return remoteEntry.getKey();
            }
        }

        createDefaultRemote(url);
        return DEFAULT_UPDATE_SERVER_REMOTE_NAME;
    }

    /** Add into preferences remote with default name and url = url */
    protected void createDefaultRemote(String url) {
        if (!getMultiRemoteCodenvy().addRemote(DEFAULT_UPDATE_SERVER_REMOTE_NAME, url)) {
            throw new IllegalStateException(String.format("It was impossible to add remote. Please add remote with url '%s' manually.", url));
        }
    }
}