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

import jline.console.ConsoleReader;

import com.codenvy.api.account.shared.dto.AccountReference;
import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.client.Codenvy;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.service.InstallationManagerService;
import com.codenvy.im.service.UserCredentials;

import org.fusesource.jansi.Ansi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import static com.codenvy.im.utils.Commons.createDtoFromJson;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected InstallationManagerService service;
    protected PreferencesStorage         preferencesStorage;

    private static final String DEFAULT_UPDATE_SERVER_REMOTE_NAME = "update-server";

    public AbstractIMCommand() {
        service = INJECTOR.getInstance(InstallationManagerService.class);
    }

    @Override
    public void init() {
        super.init();
        initDtoFactory();
        preferencesStorage = new PreferencesStorage(getGlobalPreferences(), getOrCreateRemoteNameForUpdateServer());
        validateIfUserLoggedIn();
    }

    private void initDtoFactory() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            DtoFactory.getInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * @throws IllegalStateException
     *         if user isn't logged in
     */
    protected void validateIfUserLoggedIn() throws IllegalStateException {
        String remoteName = getOrCreateRemoteNameForUpdateServer();

        Map<String, Codenvy> readyRemotes = getMultiRemoteCodenvy().getReadyRemotes();
        if (!readyRemotes.containsKey(remoteName)) {
            throw new IllegalStateException("To use installation manager commands you have to login into '" + remoteName + "' remote.");
        }

        if (preferencesStorage == null
            || preferencesStorage.getAuthToken() == null
            || preferencesStorage.getAccountId() == null
            || preferencesStorage.getAuthToken().isEmpty()
            || preferencesStorage.getAccountId().isEmpty()) {
            throw new IllegalStateException("To use installation manager commands you have to login into '" + remoteName + "' remote.");
        }
    }

    protected void printError(Exception ex) {
        if (isConnectionException(ex)) {
            printError("It is impossible to connect to Installation Manager Service. It might be stopped or it is starting up right now, " +
                       "please retry a bit later.");
        } else {
            printError(Response.valueOf(ex).toJson());
        }
    }

    protected boolean isConnectionException(Exception e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getClass().getCanonicalName().equals(ConnectException.class.getCanonicalName());
    }

    protected void printError(String message) {
        print(ansi().fg(RED).a(message).newline().reset(), false);
    }

    protected void printError(String message, boolean suppressCodenvyPrompt) {
        print(ansi().fg(RED).a(message).newline().reset(), suppressCodenvyPrompt);
    }

    protected void printProgress(int percents) {
        printProgress(createProgressBar(percents));
    }

    protected void printProgress(String message) {
        System.out.print(ansi().saveCursorPosition().a(message).restorCursorPosition());
        System.out.flush();
    }

    private String createProgressBar(int percent) {
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                bar.append("=");
            } else if (i == (percent / 2)) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }

        bar.append("]   ").append(percent).append("%     ");
        return bar.toString();
    }

    protected void cleanCurrentLine() {
        System.out.print(ansi().eraseLine(Ansi.Erase.ALL));
        System.out.flush();
    }

    protected void cleanLineAbove() {
        System.out.print(ansi().a(ansi().cursorUp(1).eraseLine(Ansi.Erase.ALL)));
        System.out.flush();
    }

    protected void printLn(String message) {
        print(message);
        System.out.println();
    }

    protected void print(String message, boolean suppressCodenvyPrompt) {
        if (!isInteractive() && !suppressCodenvyPrompt) {
            printCodenvyPrompt();
        }
        System.out.print(message);
        System.out.flush();
    }

    protected void print(String message) {
        if (!isInteractive()) {
            printCodenvyPrompt();
        }
        System.out.print(message);
        System.out.flush();
    }

    private void print(Ansi ansi, boolean suppressCodenvyPrompt) {
        if (!isInteractive() && !suppressCodenvyPrompt) {
            printCodenvyPrompt();
        }
        System.out.print(ansi);
        System.out.flush();
    }

    protected void printCodenvyPrompt() {
        final String lightBlue = '\u001b' + "[94m";
        System.out.print(ansi().a(lightBlue + "[CODENVY] ").reset()); // light blue
    }

    protected void printResponse(Object response) throws JsonParseException {
        if (response instanceof Exception) {
            printError((Exception)response);
            return;
        }

        Response responseObj = Response.fromJson(response.toString());
        if (responseObj.getStatus() != ResponseCode.OK) {
            printErrorAndExitIfNotInteractive(response);
        } else {
            printLn(response.toString());
        }
    }

    protected void printSuccess(String message, boolean suppressCodenvyPrompt) {
        print(ansi().fg(GREEN).a(message).newline().reset(), suppressCodenvyPrompt);
    }

    protected void printSuccess(String message) {
        print(ansi().fg(GREEN).a(message).newline().reset(), false);
    }

    /** @return "true" only if only user typed line equals "y". */
    protected boolean askUser(String prompt) throws IOException {
        print(prompt + " [y/N] ");
        String userAnswer = readLine();
        return userAnswer != null && userAnswer.equalsIgnoreCase("y");
    }

    /** @return line typed by user */
    protected String readLine() throws IOException {
        return doReadLine(null);
    }

    protected String readPassword() throws IOException {
        return doReadLine('*');
    }

    private String doReadLine(@Nullable Character mask) throws IOException {
        if (isInteractive()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                return reader.readLine();
            }
        } else {
            return new ConsoleReader().readLine(mask);
        }
    }

    protected void pressAnyKey(String prompt) throws IOException {
        print(prompt);
        session.getKeyboard().read();
    }

//    protected String extractUrl(String )

    /**
     * Find out remote for update server.
     * Creates new one with default name if there is no such remote stored in preferences.
     */
    @Nonnull
    protected String getOrCreateRemoteNameForUpdateServer() {
        URL url;
        try {
            url = new URL(getUpdateServerEndpoint());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        String updateServerUrl = url.getProtocol() + "://" + url.getHost();

        String remoteName = getRemoteNameByUrl(updateServerUrl);

        if (remoteName == null) {
            createRemote(DEFAULT_UPDATE_SERVER_REMOTE_NAME, updateServerUrl);
            return DEFAULT_UPDATE_SERVER_REMOTE_NAME;
        }

        return remoteName;
    }

    protected String getUpdateServerEndpoint() {
        return service.getUpdateServerEndpoint();
    }

    @Nullable
    protected AccountReference getAccountReferenceWhereUserIsOwner(@Nullable String accountName) throws IOException {
        Request request = new Request().setUserCredentials(getCredentials());
        String json = service.getAccountReferenceWhereUserIsOwner(accountName, request);
        return json == null ? null : createDtoFromJson(json, AccountReference.class);
    }


    protected UserCredentials getCredentials() {
        return new UserCredentials(preferencesStorage.getAuthToken(), preferencesStorage.getAccountId());
    }

    @Nonnull
    private Preferences getGlobalPreferences() {
        return (Preferences)session.get(Preferences.class.getName());
    }

    /** Searches and returns name of remote with certain url. */
    @Nullable
    protected String getRemoteNameByUrl(String url) throws IllegalStateException {
        Map<String, Remote> availableRemotes = getMultiRemoteCodenvy().getAvailableRemotes();

        for (Entry<String, Remote> remoteEntry : availableRemotes.entrySet()) {
            Remote remote = remoteEntry.getValue();
            if (remote.url.equalsIgnoreCase(url)) {
                return remoteEntry.getKey();
            }
        }

        return null;
    }

    /** Adds into preferences remote with certain name and url */
    protected void createRemote(String name, String url) {
        if (!getMultiRemoteCodenvy().addRemote(name, url)) {
            throw new IllegalStateException(String.format("It was impossible to add remote. Please add remote with url '%s' manually.", url));
        }
    }

    /** Returns true if only remoteName = name of remote which has url = {update server url} */
    protected boolean isRemoteForUpdateServer(@Nonnull String remoteName) {
        return remoteName.equals(getOrCreateRemoteNameForUpdateServer());
    }

    @Nullable
    protected String getRemoteUrlByName(String remoteName) {
        Remote remote = getMultiRemoteCodenvy().getRemote(remoteName);
        if (remote == null) {
            return null;
        }

        return remote.getUrl();
    }

    protected void printErrorAndExitIfNotInteractive(Object error) {
        if (error instanceof Exception) {
            printError((Exception)error);
        } else {
            printError(error.toString());
        }

        if (!isInteractive()) {
            System.exit(1);
        }
    }

    protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
        return super.getMultiRemoteCodenvy();
    }

    protected boolean isInteractive() {
        return super.isInteractive();
    }

    protected Request initRequest(String artifactName, String version) {
        return new Request()
                .setArtifactName(artifactName)
                .setVersion(version)
                .setUserCredentials(getCredentials());
    }

    protected Request initRequest() {
        return new Request().setUserCredentials(getCredentials());
    }
}
