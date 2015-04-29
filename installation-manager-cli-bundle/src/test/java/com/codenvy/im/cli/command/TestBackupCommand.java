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
package com.codenvy.im.cli.command;

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.facade.InstallationManagerFacade;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnnov */
public class TestBackupCommand  extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private InstallationManagerFacade mockInstallationManagerProxy;
    @Mock
    private CommandSession            commandSession;

    private BackupConfig testBackupConfig;

    private String testBackupDirectory = "test/backup/directory";
    private String testArtifact        = CDECArtifact.NAME;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new BackupCommand());
        spyCommand.facade = mockInstallationManagerProxy;

        performBaseMocks(spyCommand, true);
    }


    @Test
    public void testBackup() throws Exception {
        testBackupConfig = new BackupConfig().setArtifactName(testArtifact);

        String okServiceResponse = "{\n"
                                   + "  \"status\" : \"OK\"\n"
                                   + "}";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).backup(testBackupConfig);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, okServiceResponse + "\n");
    }

    @Test
    public void testBackupToDirWithEmptyName() throws Exception {
        testBackupConfig = new BackupConfig().setArtifactName(testArtifact);

        String okServiceResponse = "{\n"
                                   + "  \"status\" : \"OK\"\n"
                                   + "}";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).backup(testBackupConfig);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("directory", "");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, okServiceResponse + "\n");
    }

    @Test
    public void testBackupToDirectory() throws Exception {
        testBackupConfig = new BackupConfig().setArtifactName(testArtifact)
                                             .setBackupDirectory(testBackupDirectory);

        String okServiceResponse = "{\n"
                                   + "  \"status\" : \"OK\"\n"
                                   + "}";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).backup(testBackupConfig);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("directory", testBackupDirectory);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, okServiceResponse + "\n");
    }

    @Test
    public void testBackupThrowsError() throws Exception {
        testBackupConfig = new BackupConfig().setArtifactName(testArtifact);

        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";

        doThrow(new RuntimeException("Server Error Exception"))
            .when(mockInstallationManagerProxy).backup(testBackupConfig);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }
}
