/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
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
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.cli.preferences.PreferencesStorage;
import com.codenvy.im.console.Console;
import com.codenvy.im.event.Event;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.response.DownloadArtifactStatus;
import com.codenvy.im.response.DownloadProgressResponse;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.InstallArtifactStepInfo;
import com.codenvy.im.response.UpdatesArtifactInfo;
import com.codenvy.im.response.UpdatesArtifactStatus;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Version;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.dto.server.DtoFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {
    protected IMArtifactLabeledFacade facade;
    protected ConfigManager           configManager;
    protected PreferencesStorage      preferencesStorage;
    protected Console                 console;

    private static final String DEFAULT_SAAS_SERVER_REMOTE_NAME = "saas-server";

    private static final Logger LOG = Logger.getLogger(AbstractIMCommand.class.getSimpleName());  // use java.util.logging instead of slf4j

    protected static boolean updateImClientDone;

    public AbstractIMCommand() {
        facade = INJECTOR.getInstance(IMArtifactLabeledFacade.class);
        configManager = INJECTOR.getInstance(ConfigManager.class);
    }

    @Override
    protected Void execute() throws Exception {
        try {
            init();
            if (!updateImClientDone) {
                updateImCliClientIfNeeded();
                updateImClientDone = true;
            }
            doExecuteCommand();

        } catch (Exception e) {
            console.printErrorAndExit(e);
        } finally {
            console.reset();
        }

        return null;
    }

    protected void updateImCliClientIfNeeded() {
        final String updateFailMessage = "WARNING: automatic update of IM CLI client has been failed. See logs for details.\n";

        try {
            // get latest update of IM CLI client
            Artifact imArtifact = createArtifact(InstallManagerArtifact.NAME);
            List<UpdatesArtifactInfo> updates = facade.getAllUpdates(imArtifact);
            if (updates.isEmpty()) {
                return;
            }
            UpdatesArtifactInfo update = updates.get(updates.size() - 1);
            Version versionToUpdate = Version.valueOf(update.getVersion());

            // download update of IM CLI client
            if (update.getStatus().equals(UpdatesArtifactStatus.AVAILABLE_TO_DOWNLOAD)) {
                facade.startDownload(imArtifact, versionToUpdate);

                while (true) {
                    DownloadProgressResponse downloadProgressResponse = facade.getDownloadProgress();

                    if (downloadProgressResponse.getStatus().equals(DownloadArtifactStatus.FAILED)) {
                        // log error and continue working
                        LOG.log(Level.SEVERE, format("Fail of automatic download of update of IM CLI client. Error: %s", downloadProgressResponse.getMessage()));
                        console.printError(updateFailMessage, isInteractive());
                        return;
                    }

                    if (downloadProgressResponse.getStatus().equals(DownloadArtifactStatus.DOWNLOADED)) {
                        break;
                    }
                }
            }

            // update IM CLI client
            InstallOptions installOptions = new InstallOptions();
            installOptions.setCliUserHomeDir(System.getProperty("user.home"));
            installOptions.setConfigProperties(Collections.emptyMap());
            installOptions.setInstallType(InstallType.SINGLE_SERVER);
            installOptions.setStep(0);

            String stepId = facade.update(imArtifact, versionToUpdate, installOptions);
            facade.waitForInstallStepCompleted(stepId);
            InstallArtifactStepInfo updateStepInfo = facade.getUpdateStepInfo(stepId);
            if (updateStepInfo.getStatus() == InstallArtifactStatus.FAILURE) {
                // log error and continue working
                LOG.log(Level.SEVERE, format("Fail of automatic install of update of IM CLI client. Error: %s", updateStepInfo.getMessage()));
                console.printError(updateFailMessage, isInteractive());
                return;
            }

            console.println("This CLI client was out-dated so automatic update has being started. It will be finished at the next launch.\n");
            console.exit(0);

        } catch (Exception e) {
            // log error and continue working
            LOG.log(Level.SEVERE, format("Fail of automatic update of IM CLI client. Error: %s", e.getMessage()));
            console.printError(updateFailMessage, isInteractive());
        }
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
    @NotNull
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

    protected void logEventToSaasCodenvy(Event event) {
        try {
            facade.logSaasAnalyticsEvent(event, preferencesStorage.getAuthToken());
        } catch(Exception e) {
            LOG.log(Level.WARNING, "Error of logging event to SaaS Codenvy: " + e.getMessage(), e);   // do not interrupt main process
        }
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

    @NotNull
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

    /**
     * Create remote with certain name and url.
     * Existed one will be rewritten with new url. Token and username linked to remote will be removed.
     */
    protected void createRemote(String name, String url) {
        Remote remote = getMultiRemoteCodenvy().getRemote(name);
        if (remote != null) {
            if (preferencesStorage != null) {
                preferencesStorage.invalidate();
            }

            remote.setUrl(url);
            getGlobalPreferences().path("remotes").put(name, remote);
            return;
        }

        getMultiRemoteCodenvy().addRemote(name, url);
    }

    /** Returns true if only remoteName = name of remote which has url = {saas server url} */
    protected boolean isRemoteForSaasServer(@NotNull String remoteName) {
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
