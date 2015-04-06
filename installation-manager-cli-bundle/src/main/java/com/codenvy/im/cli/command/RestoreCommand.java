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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.nio.file.Paths;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "im-restore", description = "Restore Codenvy")
public class RestoreCommand extends AbstractIMCommand {

    @Argument(name = "backup", description = "Relative path to backup file. There should be <backup_name>.tar.gz file with the compressed <backup_name>.tar file with the same backup name. For example: backup file codenvy_backup_19-Mar-2015_16-52-06.tar.gz which consists of codenvy_backup_19-Mar-2015_16-52-06.tar", required = true, multiValued = false, index = 0)
    private String backup;

    @Override
    protected void doExecuteCommand() throws Exception {
        String artifactToBackup = CDECArtifact.NAME;
        BackupConfig config = new BackupConfig().setArtifactName(artifactToBackup);

        config.setBackupFile(Paths.get(backup));

        try {
            console.showProgressor();
            console.printResponse(facade.restore(config));
        } finally {
            console.hideProgressor();
        }
    }
}
