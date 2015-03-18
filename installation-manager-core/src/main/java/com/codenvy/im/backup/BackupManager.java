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
import static com.codenvy.im.backup.BackupConfig.removeGzipExtension;
import static java.lang.String.format;

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

        Path baseTempDir = backupConfig.obtainTempDirectory();
        try {
            Path backupFile = backupConfig.getBackupFile();
            Command backupCommand = artifact.getBackupCommand(backupConfig, configUtil);
            backupCommand.execute();

            Path compressedBackupFile = backupConfig.addGzipExtension(backupFile);
            TarUtils.packFile(backupFile, compressedBackupFile);

            Files.deleteIfExists(backupFile);
            backupConfig.setBackupFile(compressedBackupFile);
        } catch(Exception e) {
            throw new BackupException(e.getMessage(), e);
        }

        return backupConfig;
    }

    /** Restore due to config */
    public void restore(BackupConfig initialConfig) throws IOException {
        BackupConfig backupConfig = BackupConfig.clone(initialConfig);
        Path compressedBackupFile = backupConfig.getBackupFile();
        if (!Files.exists(compressedBackupFile)) {
            throw new IllegalArgumentException(format("Backup file %s doesn't exist.", compressedBackupFile));
        }

        Artifact artifact = createArtifact(backupConfig.getArtifactName());
        Path tempDir = backupConfig.obtainArtifactTempDirectory();
        try {
            TarUtils.unpack(compressedBackupFile, tempDir);

            String backupFileName = removeGzipExtension(compressedBackupFile).getFileName().toString();
            Path backupFile = tempDir.resolve(backupFileName);
            backupConfig.setBackupFile(backupFile);

            backupConfig.setBackupDirectory(tempDir);

            Command restoreCommand = artifact.getRestoreCommand(backupConfig, configUtil);
            restoreCommand.execute();
        } catch(Exception e) {
            throw new BackupException(e.getMessage(), e);
        }
    }
}
