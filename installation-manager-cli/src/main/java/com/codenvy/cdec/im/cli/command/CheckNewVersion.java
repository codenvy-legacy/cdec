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

/**
 * @author Alexander Reshetnyak
 */

import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.RestletClientFactory;
import com.codenvy.cli.command.builtin.AbsCommand;

import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ResourceException;

import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Parameters and execution of 'cdec:check' command.
 *
 * @author Alexander Reshetnyak
 */
@Command(scope = "cdec", name = "check", description = "Update CDEC...")
public class CheckNewVersion extends AbsCommand {

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
            response = installationManagerProxy.doCheckNewVersions("v1");

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
                buffer.a("Following new artifacts are available for update :");

                for (int i = 0; i < artifacts.length(); i++) {
                    JSONObject artifact = artifacts.getJSONObject(i);
                    buffer.a("artifact " + i + ": " + artifact.toString());
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