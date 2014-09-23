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

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "im", name = "download", description = "Download artifacts")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String version;

    protected Void doExecute() {
        try {
            init();

            if (artifactName != null && version != null) {
                printResponse(installationManagerProxy.download(artifactName, version, getCredentialsRep()));
            } else if (artifactName != null) {
                printResponse(installationManagerProxy.download(artifactName, getCredentialsRep()));
            } else {
                printResponse(installationManagerProxy.download(getCredentialsRep()));
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}