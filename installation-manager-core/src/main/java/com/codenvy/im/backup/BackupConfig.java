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

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class BackupConfig {

    public enum Component {
        LDAP("ldap.ldif"),
        MONGO,
        FS;

        private String filename;

        private Component() {
        }

        private Component(String filename) {
            this.filename = filename;
        }

        public Path getRelativeBackupPath() {
            Path backupPath = Paths.get(name().toLowerCase());
            if (filename != null) {
                backupPath = backupPath.resolve(filename);
            }

            return backupPath;
        }
    }

    public static final Path DEFAULT_BACKUP_DIRECTORY = Paths.get(System.getenv("HOME")).resolve(".codenvy").resolve("backups");
    public static final Path BASE_TMP_DIRECTORY       = Paths.get(System.getProperty("java.io.tmpdir")).resolve("codenvy");

    private String artifactName;
    private Path   backupDirectory;
    private Path   backupFile;

    public String getArtifactName() {
        return artifactName;
    }

    public BackupConfig setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    @Nonnull
    public Path getBackupDirectory() {
        if (backupDirectory == null) {
            backupDirectory = DEFAULT_BACKUP_DIRECTORY;
        }
        return backupDirectory;
    }

    public BackupConfig setBackupDirectory(Path backupDirectory) {
        this.backupDirectory = backupDirectory;
        return this;
    }

    /**
     * @return path to backup file without real file extension
     */
    @Nonnull
    public Path getBackupFile() {
        if (backupFile == null) {
            String fileName = obtainBackupFileName();
            backupFile = getBackupDirectory().resolve(fileName);
        }

        return backupFile;
    }

    public BackupConfig setBackupFile(Path backupFile) {
        this.backupFile = backupFile;
        return this;
    }

    public static BackupConfig clone(BackupConfig config) {
        return new BackupConfig().setArtifactName(config.getArtifactName())
                                 .setBackupDirectory(config.getBackupDirectory())
                                 .setBackupFile(config.getBackupFile());
    }

    public Path obtainArtifactTempDirectory() {
        return obtainTempDirectory().resolve(artifactName);
    }

    public static Path obtainTempDirectory() {
        return BASE_TMP_DIRECTORY;
    }

    public static Path obtainBaseTempBackupPath(Path parentDirectory, Component component) {
        return parentDirectory.resolve(component.getRelativeBackupPath());
    }

    public static Path addGzipExtension(Path file) {
        return Paths.get(format("%s.gz", file.toString()));
    }

    public static Path removeGzipExtension(Path file) {
        String fixedFilePath = file.toString().replaceAll("[.]gz$", "");  // remove '.gz' extension
        return Paths.get(fixedFilePath);
    }

    /**
     * @return name of backup file without extension
     */
    private String obtainBackupFileName() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy_HH-mm-ss");
        String currentTime = dateFormat.format(new Date());
        return format("%s_backup_%s.tar", artifactName, currentTime);
    }

}
