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
package com.codenvy.im.service;

import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.request.Request;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Dmytro Nochevnov
 */
public interface InstallationManagerService {

    /** Starts downloading */
    String startDownload(Request request);

    /** Interrupts downloading */
    String stopDownload();

    /** @return the current status of downloading process */
    String getDownloadStatus();

    /** @return update list from the server */
    String getUpdates(Request request);

    /** @retrun the list of downloaded artifacts */
    String getDownloads(Request request);

    /** @return the list of installed artifacts ant theirs versions */
    String getInstalledVersions(Request request) throws IOException;

    /** Installs artifact */
    String install(InstallOptions installOptions, Request request) throws IOException;

    /** @return installation info */
    String getInstallInfo(InstallOptions installOptions, Request request) throws IOException;

    /** @return update server url */
    String getUpdateServerEndpoint();

    /** Adds trial subscription for user being logged in */
    String addTrialSubscription(Request request) throws IOException;

    /** Check user's subscription. */
    String checkSubscription(String subscription, Request request) throws IOException;

    /** @return the version of the artifact that can be installed */
    String getVersionToInstall(Request request, int installStep) throws IOException;

    /** @return account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @Nullable
    String getAccountReferenceWhereUserIsOwner(@Nullable String accountName, Request request) throws IOException;

    /** @return the configuration of the Installation Manager */
    String getConfig();

    /** Sets new configuration for installation manager */
    String setConfig(InstallationManagerConfig config);

    /** Add node to multi-server Codenvy */
    String addNode(String dns);

    /** Remove node from multi-server Codenvy */
    String removeNode(String dns);

    /** Perform backup according to certain backup config */
    String backup(BackupConfig config) throws IOException;

    /** Perform restore according to certain backup config */
    String restore(BackupConfig config) throws IOException;
}
