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
package com.codenvy.cdec.im.cli.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "im", name = "install", description = "Install updates")
public class InstallCommand extends AbstractIMCommand {


    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (artifactName != null && version != null) {
                printResult(installationManagerProxy.install(artifactName, version, getCredentialsRep()));
            } else if (artifactName != null) {
                printResult(installationManagerProxy.install(artifactName, getCredentialsRep()));
            } else {
                printResult(installationManagerProxy.install(getCredentialsRep()));
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}
