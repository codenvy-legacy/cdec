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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.TarUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class BackupConfig {

    protected static final String BACKUP_CONFIG_FILE = "backup.config.json";

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

    protected static String BACKUP_NAME_TIME_FORMAT  = "dd-MMM-yyyy_HH-mm-ss";
    protected static Path   DEFAULT_BACKUP_DIRECTORY = Paths.get(System.getenv("HOME")).resolve(".codenvy").resolve("backups");
    protected static Path   BASE_TMP_DIRECTORY       = Paths.get(System.getProperty("java.io.tmpdir")).resolve("codenvy");

    private String artifactName;
    private String artifactVersion;
    private Path backupDirectory = DEFAULT_BACKUP_DIRECTORY;
    private Path backupFile;

    public String getArtifactName() {
        return artifactName;
    }

    public BackupConfig setArtifactName(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public BackupConfig setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
        return this;
    }

    @Nonnull
    public Path getBackupDirectory() {
        return backupDirectory;
    }

    public BackupConfig setBackupDirectory(Path backupDirectory) {
        this.backupDirectory = backupDirectory;
        return this;
    }

    /**
     * @return path to backup file
     */
    public Path getBackupFile() {
        return backupFile;
    }

    public BackupConfig setBackupFile(Path backupFile) {
        this.backupFile = backupFile;
        return this;
    }

    public BackupConfig clone() {
        return new BackupConfig().setArtifactName(artifactName)
                                 .setArtifactVersion(artifactVersion)
                                 .setBackupDirectory(backupDirectory)
                                 .setBackupFile(backupFile);
    }

    public Path obtainArtifactTempDirectory() {
        return getTempDirectory().resolve(artifactName);
    }

    public static Path getTempDirectory() {
        return BASE_TMP_DIRECTORY;
    }

    public static Path getComponentTempPath(Path parentDirectory, Component component) {
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
     * @return default  backup file path
     */
    public Path generateBackupFilePath() {
        DateFormat dateFormat = new SimpleDateFormat(BACKUP_NAME_TIME_FORMAT);
        String currentTime = dateFormat.format(getCurrentDate());

        String fileName;
        if (artifactName != null) {
            fileName = format("%s_backup_%s.tar", artifactName, currentTime);
        } else {
            fileName = format("backup_%s.tar", currentTime);
        }

        return getBackupDirectory().resolve(fileName);
    }

    protected Date getCurrentDate() {
        return new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BackupConfig that = (BackupConfig)o;

        if (artifactName != null ? !artifactName.equals(that.artifactName) : that.artifactName != null) {
            return false;
        }
        if (artifactVersion != null ? !artifactVersion.equals(that.artifactVersion) : that.artifactVersion != null) {
            return false;
        }
        if (backupDirectory != null ? !backupDirectory.equals(that.backupDirectory) : that.backupDirectory != null) {
            return false;
        }
        if (backupFile != null ? !backupFile.equals(that.backupFile) : that.backupFile != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = artifactName != null ? artifactName.hashCode() : 0;
        result = 31 * result + (artifactVersion != null ? artifactVersion.hashCode() : 0);
        result = 31 * result + (backupDirectory != null ? backupDirectory.hashCode() : 0);
        result = 31 * result + (backupFile != null ? backupFile.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return format("{'artifactName':'%s', 'artifactVersion':'%s', 'backupDirectory':'%s', 'backupFile':'%s'}",
                      artifactName,
                      artifactVersion,
                      backupDirectory,
                      backupFile);
    }

    public String toJson() throws JsonProcessingException {
        BackupConfig config = this.clone();
        config.setBackupFile(null);
        config.setBackupDirectory(null);
        return Commons.toJson(config);
    }

    public static BackupConfig fromJson(String json) throws JsonParseException {
        return Commons.fromJson(json, BackupConfig.class);
    }

    /**
     * Store backup config in this.backupFile
     */
    public void storeConfigIntoBackup() throws IOException {
        String configJson = toJson();

        Path tempDir = BASE_TMP_DIRECTORY;
        Files.createDirectories(tempDir);

        Path configFile = tempDir.resolve(BACKUP_CONFIG_FILE);
        FileUtils.write(configFile.toFile(), configJson);

        TarUtils.packFile(configFile, backupFile);

        // cleanup
        Files.deleteIfExists(configFile);
    }

    /**
     * @return config of backup, stored in this.backupFile
     */
    @Nonnull
    public BackupConfig extractConfigFromBackup() throws IOException {
        Path tempDir = BASE_TMP_DIRECTORY;
        Files.createDirectories(tempDir);

        TarUtils.unpackFile(backupFile, tempDir, Paths.get(BACKUP_CONFIG_FILE));
        Path storedConfigFile = tempDir.resolve(BACKUP_CONFIG_FILE);

        BackupConfig storedConfig = null;
        try {
            String storedConfigJson = FileUtils.readFileToString(storedConfigFile.toFile());
            storedConfig = fromJson(storedConfigJson);
            if (storedConfig == null) {
                throw new IOException();
            }
        } catch (JsonParseException | IOException e) {
            throw new BackupException(format("There was a problem with config of backup which should be placed in file '%s/%s'", backupFile.getFileName(), BACKUP_CONFIG_FILE));
        }

        // cleanup
        Files.deleteIfExists(storedConfigFile);

        return storedConfig;
    }
}
