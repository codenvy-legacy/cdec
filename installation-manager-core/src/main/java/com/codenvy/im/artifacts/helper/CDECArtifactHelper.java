/*
 *  2012-2016 Codenvy, S.A.
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
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import org.eclipse.che.commons.annotation.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author Dmytro Nochevnov
 */
public abstract class CDECArtifactHelper {
    protected final CDECArtifact  original;
    protected final ConfigManager configManager;

    public CDECArtifactHelper(CDECArtifact original, ConfigManager configManager) {
        this.original = original;
        this.configManager = configManager;
    }

    public List<String> getInstallInfo() throws IOException {
        return ImmutableList.of("Disable SELinux",
                                "Install puppet binaries",
                                "Unzip Codenvy binaries",
                                "Configure puppet master",
                                "Configure puppet agent",
                                "Launch puppet master",
                                "Launch puppet agent",
                                "Install Codenvy (~25 min)",
                                "Boot Codenvy");
    }

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
    public abstract Command getBackupCommand(BackupConfig backupConfig) throws IOException;

    /**
     * @return list of commands to restore codenvy due to given backup config and codenvy config
     */
    public abstract Command getRestoreCommand(BackupConfig backupConfig) throws IOException;

    /** @return commands to add change puppet master config and wait until changes is propagated */
    public abstract Command getUpdateConfigCommand(Config config, Map<String, String> properties) throws IOException;

    /** @return list of commands to re-install Codenvy */
    public abstract Command getReinstallCommand(Config config, @Nullable Version installedVersion) throws IOException;

    /** for testing propose */
    public String getTmpCodenvyDir() {
        return "/tmp/codenvy";
    }

    /** for testing propose */
    public String getPuppetDir() {
        return "/etc/puppet";
    }
}
