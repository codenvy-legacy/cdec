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

        JsonRepresentation response;
        
        try {
            response = installationManagerProxy.checkUpdates();
            
            if (response == null) {
                buffer.fg(RED);
                buffer.a("Incomplete response.");
                return null;
            }
                        
            JSONArray artifacts = response.getJsonArray();

            if (artifacts.length() == 0) {
                buffer.fg(GREEN);
                buffer.a("All artifacts are up-to-date.");
                
            } else {
                buffer.fg(YELLOW);
                buffer.a("Following artifacts are available for update :");
                
                for (int i = 0; i < artifacts.length(); i++) {
                    JSONObject update = artifacts.getJSONObject(i);
                    String artifact = update.getString("artifact");
                    String version = update.getString("version");
                    
                    buffer.a("- update for artifact '" + artifact + "', version '" + version + "';");   
                }
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