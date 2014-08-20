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

import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.RestletClientFactory;
import com.codenvy.cdec.im.service.response.Response;
import com.codenvy.cdec.im.service.response.StatusCode;
import com.codenvy.cli.command.builtin.AbsCommand;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import java.io.IOException;

import static com.codenvy.cdec.utils.Commons.getPrettyPrintingJson;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractIMCommand extends AbsCommand {

    protected final InstallationManagerService installationManagerProxy;

    public AbstractIMCommand() {
        try {
            installationManagerProxy = RestletClientFactory.createServiceProxy(InstallationManagerService.class);
        } catch (MissingAnnotationException | IllegalPathException | IOException e) {
            throw new IllegalStateException("Can't initialize proxy service");
        }
    }

    protected void printError(Exception ex) {
        Response response = new Response(new Response.Status(StatusCode.ERROR, ex.getMessage()));
        try {
            String message = getPrettyPrintingJson(response);
            System.out.println(ansi().fg(RED).a(message).reset());
        } catch (IOException | JSONException e) {
            Ansi ansi = ansi().fg(RED).a("Unexpected error: " + e.getMessage())
                              .newline().a("Suppressed error: " + ex.getMessage())
                              .reset();

            System.out.println(ansi);
        }
    }

    protected void printResult(String response) {
        try {
            String message = getPrettyPrintingJson(response);
            System.out.println(ansi().a(message));
        } catch (JSONException e) {
            System.out.println(ansi().fg(RED).a("Unexpected error: " + e.getMessage()).reset());
        }
    }
}
