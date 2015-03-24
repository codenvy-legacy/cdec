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

import com.codenvy.im.artifacts.CDECArtifact;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov  */
public class TestBackupConfig {
    private static final Path ORIGIN_DEFAULT_BACKUP_DIRECTORY = BackupConfig.DEFAULT_BACKUP_DIRECTORY;
    private static final Path ORIGIN_BASE_TMP_DIRECTORY       = BackupConfig.BASE_TMP_DIRECTORY;

    private static final Path TEST_DEFAULT_BACKUP_DIRECTORY = Paths.get("target/backup/codenvy");
    private static final Path TEST_BASE_TMP_DIRECTORY       = Paths.get("target/tmp_backup/codenvy");

    private static final String TEST_VERSION = "1.0.0";

    @BeforeMethod
    public void setup() {
        BackupConfig.DEFAULT_BACKUP_DIRECTORY = TEST_DEFAULT_BACKUP_DIRECTORY;
        BackupConfig.BASE_TMP_DIRECTORY = TEST_BASE_TMP_DIRECTORY;
    }

    @AfterMethod
    public void tearDown() {
        BackupConfig.DEFAULT_BACKUP_DIRECTORY = ORIGIN_DEFAULT_BACKUP_DIRECTORY;
        BackupConfig.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
    }

    @Test
    public void testEmptyInstance() {
        BackupConfig testConfig = new BackupConfig();
        assertEquals(testConfig.toString(), "{'artifactName':'null', " +
                                            "'artifactVersion':'null', " +
                                            "'backupDirectory':'target/backup/codenvy', " +
                                            "'backupFile':'null'}");

        assertNull(testConfig.getArtifactName());
        assertEquals(testConfig.getBackupDirectory(), Paths.get("target/backup/codenvy"));
        assertNull(testConfig.getBackupFile());
    }

    @Test
    public void testEquals() throws InterruptedException {
        BackupConfig backupConfig = new BackupConfig();
        BackupConfig anotherBackupConfig = new BackupConfig();

        assertTrue(backupConfig.equals(anotherBackupConfig));

        Path backupFile = backupConfig.generateBackupFilePath();
        backupConfig.setBackupFile(backupFile);
        assertFalse(backupConfig.equals(anotherBackupConfig));

        anotherBackupConfig.setBackupFile(backupFile);
        assertTrue(backupConfig.equals(anotherBackupConfig));
    }

    @Test
    public void testInstanceWithArtifact() {
        BackupConfig testConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME);
        assertEquals(testConfig.toString(), "{'artifactName':'codenvy', " +
                                            "'artifactVersion':'null', " +
                                            "'backupDirectory':'target/backup/codenvy', " +
                                            "'backupFile':'null'}");

        assertTrue(testConfig.hashCode() != 0);
    }

    @Test
    public void testInstanceWithDirectory() {
        BackupConfig testConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                    .setBackupDirectory(Paths.get("testDirectory"));
        assertEquals(testConfig.toString(), "{'artifactName':'codenvy', " +
                                            "'artifactVersion':'null', " +
                                            "'backupDirectory':'testDirectory', " +
                                            "'backupFile':'null'}");

        assertTrue(testConfig.hashCode() != 0);
    }

    @Test
    public void testInstanceWithFile() {
        BackupConfig testConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                    .setBackupFile(Paths.get("testFile"));
        assertEquals(testConfig.toString(), "{'artifactName':'codenvy', " +
                                            "'artifactVersion':'null', " +
                                            "'backupDirectory':'target/backup/codenvy', " +
                                            "'backupFile':'testFile'}");

        assertTrue(testConfig.hashCode() != 0);
    }

    @Test
    public void testGetCurrentTime() {
        BackupConfig config = new BackupConfig();
        assertNotNull(config.getCurrentDate());
    }

    @Test
    public void testDefaultFields() {
        assertNotNull(ORIGIN_DEFAULT_BACKUP_DIRECTORY);
        assertNotNull(ORIGIN_BASE_TMP_DIRECTORY);
        assertTrue(Files.exists(ORIGIN_BASE_TMP_DIRECTORY.getParent()));
    }

    @Test
    public void testGetArtifactTempDirectory() {
        BackupConfig testConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME);
        assertEquals(testConfig.obtainArtifactTempDirectory().toString(), "target/tmp_backup/codenvy/codenvy");
    }

    @Test
    public void testGetTempDirectory() {
        assertEquals(BackupConfig.getTempDirectory().toString(), "target/tmp_backup/codenvy");
    }

    @Test(dataProvider = "GetComponentTempPath")
    public void testGetComponentTempPath(String parentDir, BackupConfig.Component component, String expectedResult) {
        Path result = BackupConfig.getComponentTempPath(Paths.get(parentDir), component);
        assertEquals(result, Paths.get(expectedResult));
    }

    @DataProvider(name = "GetComponentTempPath")
    public static Object[][] GetComponentTempPath() {
        return new Object[][]{
            {"test", BackupConfig.Component.MONGO, "test/mongo"},
            {"test", BackupConfig.Component.LDAP, "test/ldap/ldap.ldif"},
            {"test", BackupConfig.Component.FS, "test/fs"}
        };
    }

    @Test
    public void testGenerateBackupFilePath() {
        BackupConfig spyTestConfig = spy(new BackupConfig());
        doReturn(new Date(1278139510000l)).when(spyTestConfig).getCurrentDate();  // 1278139510000l == '07/03/2010 9:45:10'
        assertEquals(spyTestConfig.generateBackupFilePath(), Paths.get("target/backup/codenvy/backup_03-Jul-2010_09-45-10.tar"));
    }

    @Test
    public void testAddGzipExtension() {
        Path result = BackupConfig.addGzipExtension(Paths.get("test/backup.tar"));
        assertEquals(result, Paths.get("test/backup.tar.gz"));
    }

    @Test
    public void testRemoveGzipExtension() {
        Path result = BackupConfig.removeGzipExtension(Paths.get("test/backup.tar.gz"));
        assertEquals(result, Paths.get("test/backup.tar"));
    }

    @Test
    public void testClone() {
        BackupConfig testConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                    .setArtifactVersion(TEST_VERSION)
                                                    .setBackupDirectory(Paths.get("someDir"))
                                                    .setBackupFile(Paths.get("someDir/someFile"));

        BackupConfig cloneTestConfig = testConfig.clone();
        testConfig.setBackupFile(Paths.get("anotherFile"));

        assertEquals(cloneTestConfig.toString(), "{'artifactName':'codenvy', " +
                                                 "'artifactVersion':'1.0.0', " +
                                                 "'backupDirectory':'someDir', " +
                                                 "'backupFile':'someDir/someFile'}");
    }
}
