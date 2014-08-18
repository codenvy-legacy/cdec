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
import com.codenvy.cli.command.builtin.AbsCommand;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;
import org.restlet.ext.jaxrs.internal.exceptions.IllegalPathException;
import org.restlet.ext.jaxrs.internal.exceptions.MissingAnnotationException;

import java.io.IOException;

import static org.fusesource.jansi.Ansi.Color.GREEN;

/**
 * @author Anatoliy Bazko
 */
@Command(scope = "im", name = "download", description = "Download updates")
public class DownloadCommand extends AbsCommand {

    @Argument(name = "artifact", description = "Specify the artifact to download", required = false, multiValued = false)
    private String artifactName;

    @Override
    protected Void doExecute() {
        init();

        try {
            InstallationManagerService service = RestletClientFactory.getServiceProxy(InstallationManagerService.class);
            service.doDownloadUpdates();
        } catch (MissingAnnotationException | IllegalPathException | IOException e) {
            e.printStackTrace();
        }

        // TODO

        Ansi buffer = Ansi.ansi();
        buffer.fg(GREEN);
        buffer.a("Update CDEC is not yet implement...");
        buffer.reset();
        System.out.println(buffer.toString());

        return null;
    }
}
