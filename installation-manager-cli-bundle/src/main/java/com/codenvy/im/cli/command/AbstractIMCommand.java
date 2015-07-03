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
package com.codenvy.im.cli.command;

import com.codenvy.cli.command.builtin.AbsCommand;
import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.client.Codenvy;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.console.Console;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.saas.SaasUserCredentials;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected IMArtifactLabeledFacade facade;
    protected PreferencesStorage      preferencesStorage;
    protected Console                 console;

    private static final String DEFAULT_SAAS_SERVER_REMOTE_NAME = "saas-server";

    public AbstractIMCommand() {
        facade = INJECTOR.getInstance(IMArtifactLabeledFacade.class);
    }

    @Override
    protected Void execute() throws Exception {
        try {
            init();
            doExecuteCommand();

        } catch (Exception e) {
            console.printErrorAndExit(e);
        } finally {
            console.reset();
        }

        return null;
    }

    protected abstract void doExecuteCommand() throws Exception;

    @Override
    public void init() {
        super.init();

        initConsole();
        initDtoFactory();
        initPreferencesStorage();
    }

    protected void initPreferencesStorage() {
        preferencesStorage = new PreferencesStorage(getGlobalPreferences(), getOrCreateRemoteNameForSaasServer());
    }

    protected void initConsole() {
        try {
            console = Console.create(isInteractive());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
        String remoteName = getOrCreateRemoteNameForSaasServer();

        Map<String, Codenvy> readyRemotes = getMultiRemoteCodenvy().getReadyRemotes();
        if (!readyRemotes.containsKey(remoteName)) {
            throw new IllegalStateException("Please log in into '" + remoteName + "' remote.");
        }

        if (preferencesStorage == null
            || preferencesStorage.getAuthToken() == null
            || preferencesStorage.getAccountId() == null
            || preferencesStorage.getAuthToken().isEmpty()
            || preferencesStorage.getAccountId().isEmpty()) {
            throw new IllegalStateException("Please log in into '" + remoteName + "' remote.");
        }
    }

    /**
     * Find out remote for saas server.
     * Creates new one with default name if there is no such remote stored in preferences.
     */
    @Nonnull
    protected String getOrCreateRemoteNameForSaasServer() {
        URL url;
        try {
            url = new URL(getSaasServerEndpoint());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        String saasServerUrl = url.getProtocol() + "://" + url.getHost();

        String remoteName = getRemoteNameByUrl(saasServerUrl);

        if (remoteName == null) {
            createRemote(DEFAULT_SAAS_SERVER_REMOTE_NAME, saasServerUrl);
            return DEFAULT_SAAS_SERVER_REMOTE_NAME;
        }

        return remoteName;
    }

    protected String getSaasServerEndpoint() {
        return facade.getSaasServerEndpoint();
    }

    @Nullable
    protected AccountReference getAccountReferenceWhereUserIsOwner(@Nullable String accountName) throws IOException {
        SaasUserCredentials credentials = getCredentials();
        return facade.getAccountWhereUserIsOwner(accountName, credentials.getToken());
    }

    protected SaasUserCredentials getCredentials() {
        validateIfUserLoggedIn();
        return new SaasUserCredentials(preferencesStorage.getAuthToken(), preferencesStorage.getAccountId());
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

    /** Returns true if only remoteName = name of remote which has url = {saas server url} */
    protected boolean isRemoteForSaasServer(@Nonnull String remoteName) {
        return remoteName.equals(getOrCreateRemoteNameForSaasServer());
    }

    @Nullable
    protected String getRemoteUrlByName(String remoteName) {
        Remote remote = getMultiRemoteCodenvy().getRemote(remoteName);
        if (remote == null) {
            return null;
        }

        return remote.getUrl();
    }

    protected MultiRemoteCodenvy getMultiRemoteCodenvy() {
        return super.getMultiRemoteCodenvy();
    }

    protected boolean isInteractive() {
        return super.isInteractive();
    }

}
