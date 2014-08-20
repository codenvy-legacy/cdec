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

import com.codenvy.cdec.im.service.response.Response;

import org.apache.karaf.shell.commands.Command;
import org.restlet.resource.ResourceException;

import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.RestletClientFactory;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cli.command.builtin.AbsCommand;

import static com.codenvy.cdec.im.cli.command.MessageHelper.*;

/**
 * TODO
 * Parameters and execution of 'cdec:check' command.
 *
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
@Command(scope = "cdec", name = "check", description = "Check all available updates.")
public class CheckUpdatesCommand extends AbsCommand {

    InstallationManagerService installationManagerProxy;

    /**
     * Check availability new version.
     */
    protected Object doExecute() throws Exception {
        init();

        installationManagerProxy = RestletClientFactory.getServiceProxy(InstallationManagerService.class);
        
        try {
            String response = installationManagerProxy.checkUpdates();
            
            if (response == null) {
                MessageHelper.printlnRed(INCOMPLETE_RESPONSE);
                return null;
            }

            MessageHelper.printlnGreen(Commons.getPrettyPrintingJson(response));
            
        } catch (ResourceException re) {
            MessageHelper.println(re);
        }

        return null;
    }
}