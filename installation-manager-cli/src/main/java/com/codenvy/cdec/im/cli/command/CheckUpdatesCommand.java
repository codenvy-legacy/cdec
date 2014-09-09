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

import org.apache.karaf.shell.commands.Command;

/**
 * @author Anatoliy Bazko
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
@Command(scope = "im", name = "check-updates", description = "Check availability to download new versions of the artifacts")
public class CheckUpdatesCommand extends AbstractIMCommand {

    /** {@inheritDoc} */
    @Override
    protected Void doExecute() {
        try {
            init();

            String response = installationManagerProxy.getUpdates(getCredentialsRep());
            printResult(response);
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }

}