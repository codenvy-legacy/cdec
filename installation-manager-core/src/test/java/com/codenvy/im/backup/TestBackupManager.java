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
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.utils.TarUtils;
import com.codenvy.im.utils.Version;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** @author Dmytro Nochevnov */
public class TestBackupManager {

    @Mock
    private ConfigUtil   mockConfigUtil;
    @Mock
    private CDECArtifact mockCdecArtifact;
    @Mock
    private Command      mockCommand;

    private BackupManager spyManager;

    private static final Path   ORIGIN_BASE_TMP_DIRECTORY      = BackupConfig.BASE_TMP_DIRECTORY;
    private static final String ORIGIN_BACKUP_NAME_TIME_FORMAT = BackupConfig.BACKUP_NAME_TIME_FORMAT;

    private static final Path   TEST_DEFAULT_BACKUP_DIRECTORY = Paths.get("target/backups");
    private static final Path   TEST_BASE_TMP_DIRECTORY       = Paths.get("target/tmp_backup");
    private static final String TEST_BACKUP_NAME_TIME_FORMAT  = "dd-MMM-yyyy";

    private static final String TEST_VERSION = "1.0.0";

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        FileUtils.deleteDirectory(TEST_DEFAULT_BACKUP_DIRECTORY.toFile());
        FileUtils.deleteDirectory(TEST_BASE_TMP_DIRECTORY.toFile());

        BackupConfig.BASE_TMP_DIRECTORY = TEST_BASE_TMP_DIRECTORY;
        BackupConfig.BACKUP_NAME_TIME_FORMAT = TEST_BACKUP_NAME_TIME_FORMAT;

        spyManager = spy(new BackupManager(TEST_DEFAULT_BACKUP_DIRECTORY.toString(), mockConfigUtil));
        doReturn(mockCdecArtifact).when(spyManager).getArtifact(CDECArtifact.NAME);

        doReturn(CDECArtifact.NAME).when(mockCdecArtifact).getName();
        doReturn(Version.valueOf("1.0.0")).when(mockCdecArtifact).getInstalledVersion();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        BackupConfig.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
        BackupConfig.BACKUP_NAME_TIME_FORMAT = ORIGIN_BACKUP_NAME_TIME_FORMAT;
    }

    @Test
    public void testBackupCodenvy() throws IOException {
        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupDirectory(TEST_DEFAULT_BACKUP_DIRECTORY);

        BackupConfig expectedBackupConfig = initialBackupConfig.clone()
                                                               .setBackupFile(initialBackupConfig.generateBackupFilePath())
                                                               .setArtifactVersion(TEST_VERSION);

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                BackupConfig backupConfig = (BackupConfig)invocationOnMock.getArguments()[0];
                return SimpleCommand.createCommand(format("echo '' > %s", backupConfig.getBackupFile().toString()));  // create empty backup file for testing propose
            }
        }).when(mockCdecArtifact).getBackupCommand(expectedBackupConfig, mockConfigUtil);

        BackupConfig result = spyManager.backup(initialBackupConfig);

        expectedBackupConfig.setBackupFile(BackupConfig.addGzipExtension(expectedBackupConfig.getBackupFile()));  // expect backup file name with added gzip extension
        assertEquals(result, expectedBackupConfig);
        assertTrue(Files.exists(result.getBackupFile()));
    }

    @Test(expectedExceptions = BackupException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testBackupException() throws IOException {
        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupDirectory(TEST_DEFAULT_BACKUP_DIRECTORY);

        BackupConfig expectedBackupConfig = initialBackupConfig.setBackupFile(initialBackupConfig.generateBackupFilePath())
                                                               .setArtifactVersion(TEST_VERSION);

        doThrow(new IOException("error")).when(mockCdecArtifact).getBackupCommand(expectedBackupConfig, mockConfigUtil);
        spyManager.backup(initialBackupConfig);
    }


    @Test(expectedExceptions = BackupException.class,
          expectedExceptionsMessageRegExp = "Artifact version is unavailable")
    public void testBackupNullArtifactVersionException() throws IOException {
        BackupConfig backupConfig = new BackupConfig().setArtifactName("codenvy");
        doReturn(null).when(mockCdecArtifact).getInstalledVersion();
        spyManager.backup(backupConfig);
    }

    @Test
    public void testRestoreCodenvy() throws IOException {
        Path backupFile = TEST_DEFAULT_BACKUP_DIRECTORY.resolve("testBackup.tar");
        Path compressedBackupFile = prepareCompressedBackup(backupFile);

        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(compressedBackupFile);

        BackupConfig expectedBackupConfig = new BackupConfig().setArtifactName("codenvy");

        Path expectedBackupDirectory = expectedBackupConfig.obtainArtifactTempDirectory();
        expectedBackupConfig.setBackupDirectory(expectedBackupDirectory);

        Path tempBackupFile = expectedBackupDirectory.resolve(backupFile.getFileName().toString());
        expectedBackupConfig.setBackupFile(tempBackupFile);

        doReturn(mockCommand).when(mockCdecArtifact).getRestoreCommand(expectedBackupConfig,
                                                                       mockConfigUtil);

        spyManager.restore(initialBackupConfig);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Backup file is unknown.")
    public void testRestoreUnknownBackup() throws IOException {
        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy");

        spyManager.restore(initialBackupConfig);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Backup file 'non-exists' doesn't exist.")
    public void testRestoreNonExistsBackup() throws IOException {
        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(Paths.get("non-exists"));

        spyManager.restore(initialBackupConfig);
    }

    @Test(expectedExceptions = BackupException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testRestoreIOException() throws IOException {
        Path compressedBackupFile = prepareCompressedBackup(TEST_DEFAULT_BACKUP_DIRECTORY.resolve("testBackup.tar"));
        BackupConfig initialBackupConfig = new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(compressedBackupFile);

        doThrow(new IOException("error")).when(mockCdecArtifact).getRestoreCommand(any(BackupConfig.class), any(ConfigUtil.class));
        spyManager.restore(initialBackupConfig);
    }


    @Test(expectedExceptions = BackupException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testRestoreBackupException() throws IOException {
        Path compressedBackupFile = prepareCompressedBackup(TEST_DEFAULT_BACKUP_DIRECTORY.resolve("testBackup.tar"));

        BackupConfig spyBackupConfig = spy(new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(compressedBackupFile));

        doReturn(spyBackupConfig).when(spyBackupConfig).clone();
        doThrow(new BackupException("error")).when(spyBackupConfig).extractConfigFromBackup();

        spyManager.restore(spyBackupConfig);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testRestoreIllegalArgumentException() throws IOException {
        Path compressedBackupFile = prepareCompressedBackup(TEST_DEFAULT_BACKUP_DIRECTORY.resolve("testBackup.tar"));

        BackupConfig spyBackupConfig = spy(new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(compressedBackupFile));

        doReturn(spyBackupConfig).when(spyBackupConfig).clone();
        doReturn(spyBackupConfig).when(spyBackupConfig).extractConfigFromBackup();
        doThrow(new IllegalArgumentException("error")).when(spyManager).checkBackup(mockCdecArtifact, spyBackupConfig);

        spyManager.restore(spyBackupConfig);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testRestoreIllegalStateException() throws IOException {
        Path compressedBackupFile = prepareCompressedBackup(TEST_DEFAULT_BACKUP_DIRECTORY.resolve("testBackup.tar"));

        BackupConfig spyBackupConfig = spy(new BackupConfig().setArtifactName("codenvy")
                                                             .setBackupFile(compressedBackupFile));

        doReturn(spyBackupConfig).when(spyBackupConfig).clone();
        doReturn(spyBackupConfig).when(spyBackupConfig).extractConfigFromBackup();
        doThrow(new IllegalStateException("error")).when(spyManager).checkBackup(mockCdecArtifact, spyBackupConfig);

        spyManager.restore(spyBackupConfig);
    }

    @Test
    public void testGetArtifact() throws IOException {
        BackupManager manager = new BackupManager(TEST_DEFAULT_BACKUP_DIRECTORY.toString(), mockConfigUtil);
        Artifact result = manager.getArtifact("codenvy");
        assertEquals(result.getName(), "codenvy");
    }

    @Test
    public void testCheckBackupSuccessful() {
        BackupConfig checkingConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                              .setArtifactVersion(TEST_VERSION);

        spyManager.checkBackup(mockCdecArtifact, checkingConfig);
    }

    @Test
    public void testCheckBackupIllegalArgumentException(BackupConfig checkingConfig, String expectedExceptionMessage) {
        try {
            spyManager.checkBackup(mockCdecArtifact, checkingConfig);
        } catch(IllegalArgumentException e) {
            assertEquals(e.getMessage(), expectedExceptionMessage);
            return;
        }

        fail(format("Here should be IllegalArgumentException with message '%s'", expectedExceptionMessage));
    }

    @DataProvider()
    public Object[][] GetCheckBackupIllegalArgumentExceptionData() {
        return new Object[][] {
            {new BackupConfig().setArtifactName("anotherArtifact"),
             "Backed up artifact 'anotherArtifact' doesn't equal restoring artifact 'codenvy'"},

            {new BackupConfig().setArtifactName("codenvy").setArtifactVersion("0.0.1"),
             "Backed up artifact version '0.0.1' doesn't equal to restoring artifact version '1.0.0'"},
        };
    }

    @Test
    public void testCheckBackupIllegalStateException() throws IOException {
        BackupConfig checkingConfig = new BackupConfig().setArtifactName("codenvy");

        String expectedExceptionMessage = "It is impossible to get version of restoring artifact 'codenvy'";

        doReturn(null).when(mockCdecArtifact).getInstalledVersion();
        try {
            spyManager.checkBackup(mockCdecArtifact, checkingConfig);
            fail("Here should be IllegalStateException.");
        } catch(IllegalStateException e) {
            assertEquals(e.getMessage(), expectedExceptionMessage);
        }

        doThrow(new IOException()).when(mockCdecArtifact).getInstalledVersion();
        try {
            spyManager.checkBackup(mockCdecArtifact, checkingConfig);
            fail("Here should be IllegalStateException.");
        } catch(IllegalStateException e) {
            assertEquals(e.getMessage(), expectedExceptionMessage);
        }
    }

    private Path prepareCompressedBackup(Path backupFile) throws IOException {
        String backupConfigJson = "{\n"
                                  + "  \"artifactName\" : \"codenvy\",\n"
                                  + "  \"artifactVersion\" : \"1.0.0\"\n"
                                  + "}";
        Path backupConfigFile = backupFile.getParent().resolve(BackupConfig.BACKUP_CONFIG_FILE);
        FileUtils.writeStringToFile(backupConfigFile.toFile(), backupConfigJson);
        TarUtils.packFile(backupConfigFile, backupFile);

        Path compressedBackupFile = BackupConfig.addGzipExtension(backupFile);
        TarUtils.compressFile(backupFile, compressedBackupFile);
        return compressedBackupFile;
    }
}
