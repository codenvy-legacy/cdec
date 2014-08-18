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

import jline.internal.Log;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.fusesource.jansi.Ansi;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;

import com.codenvy.cdec.RestletClientFactory;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cli.command.builtin.AbsCommand;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.Color.RED;

/**
 * TODO
 * Parameters and execution of 'cdec:download' command.
 *
 * @author Dmytro Nochevnov
 */
@Command(scope = "cdec", name = "download", description = "Download update.")
public class DownloadCommand extends AbsCommand {

    InstallationManagerService installationManagerProxy;

    
    @Argument(index = 0, name = "artifact", description = "The name of artifact.", required = true, multiValued = false)
    String artifactName = "";

    @Argument(index = 1, name = "version", description = "The name of version of artifact.", required = false, multiValued = false)
    String version = "";

    
    /**
     * Download artifact.
     */
    protected Object doExecute() throws Exception {
        init();

        installationManagerProxy = RestletClientFactory.getServiceProxy(InstallationManagerService.class);
        
        Ansi buffer = Ansi.ansi();
        
        JsonRepresentation response;
        
        try {
            response = installationManagerProxy.download(artifactName, version);

            buffer.fg(GREEN);
            buffer.a("Start downloading artifact '" + artifactName + "', version '" + "'.");
                
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