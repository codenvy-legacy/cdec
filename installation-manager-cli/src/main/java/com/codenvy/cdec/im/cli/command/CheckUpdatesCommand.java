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

import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.karaf.shell.commands.Command;
import org.restlet.resource.ResourceException;

import com.codenvy.cli.command.builtin.MultiRemoteCodenvy;
import com.codenvy.cli.command.builtin.Remote;
import com.codenvy.cli.security.RemoteCredentials;
import com.codenvy.client.Codenvy;

/**
 * @author Anatoliy Bazko
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
@Command(scope = "im", name = "check-updates", description = "Check all available updates")
public class CheckUpdatesCommand extends AbstractIMCommand {

    /**
     * Check availability new version.
     */
    protected Void doExecute() throws Exception {
        init();
        
        String token = getToken();
        if (token == null) {
            return null;
        }
        
        try {
            String response = installationManagerProxy.getUpdates(token);
            printResult(response);
        } catch (ResourceException re) {
            printError(re);
        }

        return null;
    }

}