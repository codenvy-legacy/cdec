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
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallOptions;
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

    public abstract List<String> getInstallInfo(InstallOptions installOptions) throws IOException;

    public abstract Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException;

    public abstract Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException;

    public abstract Command getBackupCommand(BackupConfig backupConfig, ConfigUtil codenvyConfigUtil) throws IOException;

    public abstract Command getRestoreCommand(BackupConfig backupConfig, ConfigUtil codenvyConfigUtil) throws IOException;
}
