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

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.utils.BasedInjector;
import com.codenvy.cli.command.builtin.AbsCommand;

import org.apache.karaf.shell.commands.Command;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.Map;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

/**
 * Parameters and execution of 'cdec:check' command.
 *
 * @author Alexander Reshetnyak
 */
@Command(scope = "cdec", name = "check", description = "Update CDEC...")
public class CheckNewVersion extends AbsCommand {

    // TODO check class

    /**
     * Check availability new version.
     */
    protected Object doExecute() throws IOException {
        init();

        // TODO rest request
        InstallationManager installationManager = BasedInjector.getInstance().getInstance(InstallationManager.class);

        installationManager.checkNewVersions();
        Map<Artifact, String> newVersions = installationManager.getNewVersions();

        Ansi buffer = Ansi.ansi();

        if (newVersions.isEmpty()) {
            buffer.fg(GREEN);
            buffer.a("All artifacts are up-to-date.");
        } else {
            buffer.fg(YELLOW);
            buffer.a("Following new artifacts are available for update :");
            for (Map.Entry<Artifact, String> entry : newVersions.entrySet()) {
                buffer.a(entry.getKey().getName() + ":" + entry.getValue());
            }
        }

        buffer.reset();
        System.out.println(buffer.toString());

        return null;
    }
}