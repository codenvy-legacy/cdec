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

import jline.internal.Log;

import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;
import org.restlet.resource.ResourceException;

import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.RestletClientFactory;
import com.codenvy.cdec.im.service.response.ArtifactInfo;
import com.codenvy.cdec.utils.Commons;
import com.codenvy.cli.command.builtin.AbsCommand;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.Color.RED;

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
        
        Ansi buffer = Ansi.ansi();
                
        String response;
        
        try {
            response = installationManagerProxy.checkUpdates();
            
            if (response == null) {
                buffer.fg(RED);
                buffer.a("Incomplete response.");
            } else {
                String output = Commons.getPrettyPrintingJson(response);
                
                buffer.fg(GREEN);
                buffer.a(output);
            }
            
        } catch (ResourceException re) {
            buffer.fg(RED);
            buffer.a("There was an error " + re.getStatus().toString() + ".\n"
                   + "details: " + re.getMessage());
        }

        buffer.reset();
        System.out.println(buffer.toString());

        return null;
    }
}