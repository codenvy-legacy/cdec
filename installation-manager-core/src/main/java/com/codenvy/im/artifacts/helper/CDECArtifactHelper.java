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
package com.codenvy.im.artifacts.helper;

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.utils.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Dmytro Nochevnov
 */
public abstract class CDECArtifactHelper {
    CDECArtifact original;

    public CDECArtifactHelper(CDECArtifact original) {
        this.original = original;
    }

    /**
     * @return list of install command description
     */
    public abstract List<String> getInstallInfo(InstallOptions installOptions) throws IOException;

    /**
     * @return list of commands to install Codenvy due to given version, install options and path to binaries
     */
    public abstract Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException;

    /**
     * @return list of commands to update Codenvy due to given version to update, install options and path to binaries
     */
    public abstract Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException;

    /**
     * @return list of commands to backup codenvy due to given backup config and codenvy config
     */
    public abstract Command getBackupCommand(BackupConfig backupConfig, ConfigManager codenvyConfigManager) throws IOException;

    /**
     * @return list of commands to restore codenvy due to given backup config and codenvy config
     */
    public abstract Command getRestoreCommand(BackupConfig backupConfig, ConfigManager codenvyConfigManager) throws IOException;
}
