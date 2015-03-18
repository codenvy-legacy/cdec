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
import org.junit.After;
import org.junit.Before;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.*;

/** @author Dmytro Nochevnov  */
public class TestBackupConfig {
    private BackupConfig spyTestConfig;

    private static final Path ORIGIN_DEFAULT_BACKUP_DIRECTORY = BackupConfig.DEFAULT_BACKUP_DIRECTORY;
    private static final Path ORIGIN_BASE_TMP_DIRECTORY       = BackupConfig.BASE_TMP_DIRECTORY;

    private static final Path TEST_DEFAULT_BACKUP_DIRECTORY = Paths.get("backup/codenvy");
    private static final Path TEST_BASE_TMP_DIRECTORY       = Paths.get("tmp_backup/codenvy");


    @BeforeMethod
    public void setup() {
        spyTestConfig = spy(new BackupConfig());
        BackupConfig.DEFAULT_BACKUP_DIRECTORY = TEST_DEFAULT_BACKUP_DIRECTORY;
        BackupConfig.BASE_TMP_DIRECTORY = TEST_BASE_TMP_DIRECTORY;

        doReturn(new Date(1278139510000l)).when(spyTestConfig).getCurrentDate();  // return Date(07/03/2010 9:45:10)
    }

    @AfterMethod
    public void tearDown() {
        BackupConfig.DEFAULT_BACKUP_DIRECTORY = ORIGIN_DEFAULT_BACKUP_DIRECTORY;
        BackupConfig.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
    }

    @Test
    public void testEmptyInstance() {
        assertEquals(spyTestConfig.toString(), "{'artifactName':'null', " +
                                               "'backupDirectory':'backup/codenvy', " +
                                               "'backupFile':'backup/codenvy/backup_03-Jul-2010_09-45-10.tar'}");

        assertNull(spyTestConfig.getArtifactName());
        assertEquals(spyTestConfig.getBackupDirectory(), Paths.get("backup/codenvy"));
        assertEquals(spyTestConfig.getBackupFile(), Paths.get("backup/codenvy/backup_03-Jul-2010_09-45-10.tar"));
    }

    @Test
    public void testInstanceWithArtifact() {
        spyTestConfig.setArtifactName(CDECArtifact.NAME);
        assertEquals(spyTestConfig.toString(), "{'artifactName':'codenvy', " +
                                               "'backupDirectory':'backup/codenvy', " +
                                               "'backupFile':'backup/codenvy/codenvy_backup_03-Jul-2010_09-45-10.tar'}");
    }

    @Test
    public void testInstanceWithDirectory() {
        spyTestConfig.setArtifactName(CDECArtifact.NAME)
                     .setBackupDirectory(Paths.get("testDirectory"));
        assertEquals(spyTestConfig.toString(), "{'artifactName':'codenvy', " +
                                               "'backupDirectory':'testDirectory', " +
                                               "'backupFile':'testDirectory/codenvy_backup_03-Jul-2010_09-45-10.tar'}");
    }

    @Test
    public void testInstanceWithFile() {
        spyTestConfig.setArtifactName(CDECArtifact.NAME)
                     .setBackupFile(Paths.get("testFile"));
        assertEquals(spyTestConfig.toString(), "{'artifactName':'codenvy'," +
                                               " 'backupDirectory':'backup/codenvy'," +
                                               " 'backupFile':'testFile'}");
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
}
