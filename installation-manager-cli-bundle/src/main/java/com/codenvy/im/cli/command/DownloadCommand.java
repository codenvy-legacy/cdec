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
import org.apache.karaf.shell.commands.Option;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "im-download", description = "Download artifacts")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String version;

    @Option(name = "--list-local", aliases = "-l", description = "To show the downloaded list of artifacts", required = false)
    private boolean listLocal;

    @Option(name = "--check-remote", aliases = "-c", description = "To check on remote versions to see if new version is available",
            required = false)
    private boolean checkRemote;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (listLocal) {
                if (artifactName != null && version != null) {
                    printResponse(installationManagerProxy.getDownloads(artifactName, version));
                } else if (artifactName != null) {
                    printResponse(installationManagerProxy.getDownloads(artifactName));
                } else {
                    printResponse(installationManagerProxy.getDownloads());
                }

            } else if (checkRemote) {
                printResponse(installationManagerProxy.getUpdates(getCredentialsRep()));

            } else {

                printInfo("Downloading might takes several minutes depending on your internet connection. Please wait. \n");
                if (artifactName != null && version != null) {
                    printResponse(installationManagerProxy.download(artifactName, version, getCredentialsRep()));
                } else if (artifactName != null) {
                    printResponse(installationManagerProxy.download(artifactName, getCredentialsRep()));
                } else {
                    printResponse(installationManagerProxy.download(getCredentialsRep()));
                }
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}
