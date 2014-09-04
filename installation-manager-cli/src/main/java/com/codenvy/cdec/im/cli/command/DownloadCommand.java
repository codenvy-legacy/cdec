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
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;

import com.codenvy.cdec.user.UserCredentials;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "im", name = "download", description = "Download artifacts")
public class DownloadCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to download", required = false, multiValued = false)
    private String version;

    protected Void doExecute() throws Exception {
        init();

        try {
            UserCredentials userCredentials = new UserCredentials(getAuthToken(), "accountId");  // TODO read real accountId
            JacksonRepresentation<UserCredentials> userCredentialsRep = new JacksonRepresentation<UserCredentials>(userCredentials);
            
            if (artifactName != null && version != null) {
                printResult(installationManagerProxy.download(artifactName, version, userCredentialsRep));
            } else if (artifactName != null) {
                printResult(installationManagerProxy.download(artifactName, userCredentialsRep));
            } else {
                printResult(installationManagerProxy.download(userCredentialsRep));
            }
        } catch (IllegalStateException | ResourceException e) {
            printError(e);
        }

        return null;
    }
}