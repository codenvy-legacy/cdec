/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

/** @author Anatoliy Bazko */
@Command(scope = "im", name = "download-list", description = "The list of downloaded artifacts")
public class DownloadsListCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact", required = false, multiValued = false)
    private String artifactName;

    @Override
    protected Void doExecute() {
        try {
            init();

            installationManagerProxy.getDownloads();
            if (artifactName != null) {
                printResponse(installationManagerProxy.getDownloads(artifactName));
            } else {
                printResponse(installationManagerProxy.getDownloads());
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}