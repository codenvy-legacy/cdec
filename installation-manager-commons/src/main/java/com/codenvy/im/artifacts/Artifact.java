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
package com.codenvy.im.artifacts;

import com.codenvy.im.command.Command;
import com.codenvy.im.config.Config;
import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Anatoliy Bazko
 */
// TODO java doc
public interface Artifact extends Comparable<Artifact> {

    /** @return the artifact name */
    String getName();

    /** Installs artifact */
    // TODO remove
    void install(Path pathToBinaries, @Nullable CdecInstallOptions options) throws IOException;

    /** @return current installed version of the artifact */
    @Nullable
    Version getInstalledVersion(String authToken) throws IOException;

    /** @return the priority of the artifact to install, update etc. */
    int getPriority();

    List<String> getInstallInfo(Config config, InstallOptions installOptions) throws IOException;

    /** @return list of commands to perform installation. */
    Command getInstallCommand(Config config, InstallOptions installOptions) throws IOException;

    /**
     * @return true if given version of the artifact can be installed, in general case versionToInstall should be greater than current installed
     * version of the artifact
     */
    boolean isInstallable(Version versionToInstall, String accessToken) throws IOException;

    /** @return properties stored at update server */
    Map getProperties(Version version, String updateEndpoint, HttpTransport transport) throws IOException;

    /** @return version at the update server which is could be used to update already installed version */
    @Nullable
    Version getLatestInstallableVersionToDownload(String authToken, String updateEndpoint, HttpTransport transport) throws IOException;

    public void validateProperties(Map properties) throws IOException;

    SortedMap<Version, Path> getDownloadedVersions(Path downloadDir, String updateEndpoint, HttpTransport transport) throws IOException;
}
