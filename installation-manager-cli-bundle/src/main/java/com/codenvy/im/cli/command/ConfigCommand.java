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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/** @author Anatoliy Bazko */
@Command(scope = "codenvy", name = "im-config", description = "Config installation manager")
public class ConfigCommand extends AbstractIMCommand {


    @Option(name = "--download-dir", aliases = "--dd", description = "To set the download directory", required = false)
    private String downloadDir;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (downloadDir != null) {
                printResponse(installationManagerProxy.setConfig(downloadDir));
            } else {
                printResponse(installationManagerProxy.getConfig());
            }

        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}
