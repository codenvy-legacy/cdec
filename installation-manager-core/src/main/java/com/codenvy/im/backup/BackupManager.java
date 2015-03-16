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
package com.codenvy.im.backup;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.command.Command;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.utils.TarUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/** @author Dmytro Nochevnov */
@Singleton
public class BackupManager {

    private ConfigUtil configUtil;

    @Inject
    public BackupManager(ConfigUtil configUtil) throws IOException {
        this.configUtil = configUtil;
    }

    /**
     * Backup due to config
     * @param initialConfig initial backup config
     * @return updated backup config
     */
    public BackupConfig backup(BackupConfig initialConfig) throws IOException {
        BackupConfig backupConfig = BackupConfig.clone(initialConfig);
        Artifact artifact = createArtifact(backupConfig.getArtifactName());
        Files.createDirectories(backupConfig.getBackupDirectory());

        Path tempBackupDirectory = backupConfig.obtainBaseTempDirectory();
        try {
            FileUtils.deleteDirectory(tempBackupDirectory.toFile());
            Files.createDirectories(tempBackupDirectory);

            Command backupCommand = artifact.getBackupCommand(backupConfig, configUtil);
            backupCommand.execute();

            TarUtils.packFile(backupConfig.obtainBackupTarPack(), backupConfig.obtainBackupTarGzippedPack());
            Files.deleteIfExists(backupConfig.obtainBackupTarPack());
            FileUtils.deleteDirectory(tempBackupDirectory.toFile());
        } catch(Exception e) {
            throw new BackupException(e.getMessage(), e);
        }

        return backupConfig;
    }
}
