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
import org.apache.karaf.shell.console.impl.jline.Branding;
import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.fusesource.jansi.Ansi.Color.CYAN;

/**
 * Defines a global help for Codenvy commands available.
 * This will be used when using for example ./codenvy without arguments as the name of this command is empty and is in the codenvy prefix.
 *
 * @author Florent Benoit
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "", description = "Help")
public class HelpCommand extends AbstractIMCommand {

    @Override
    protected void doExecuteCommand() throws Exception {
        Ansi buffer = Ansi.ansi();

        // add the branding banner
        buffer.a(Branding.loadBrandingProperties().getProperty("banner"));

        // display commands
        buffer.a(INTENSITY_BOLD).a("COMMANDS").a(INTENSITY_BOLD_OFF).a("\n");

        String value = buildAsciiForm().withEntry(withCyan("remote"), "Add or remove remote Codenvy cloud references")
                                       .withEntry(withCyan("login"), "Login to a remote Codenvy cloud")
                                       .withEntry(withCyan("logout"), "Logout to a remote Codenvy cloud")
                                       .withEntry(withCyan("list"), "List workspaces, projects and processes")
                                       .withEntry(withCyan("clone-local"), "Clone a remote Codenvy project to a local directory")
                                       .withEntry(withCyan("build"), "Build a project")
                                       .withEntry(withCyan("run"), "Run a project")
                                       .withEntry(withCyan("logs"), "Display output logs for a runner or builder")
                                       .withEntry(withCyan("info"), "Display information for a project, runner, or builder")
                                       .withEntry(withCyan("open"), "Starts a browser session to access a project, builder or runner")
                                       .withEntry(withCyan("stop"), "Stop one or more runner processes")
                                       .withEntry(withCyan("create-project"), "Create a project")
                                       .withEntry(withCyan("create-factory"), "Create a factory")
                                       .withEntry(withCyan("privacy"), "Change privacy for a project")
                                       .withEntry(withCyan("delete-project"), "Delete a project")
                                       .withEntry(withCyan("push"), "Push local project changes back to Codenvy")
                                       .withEntry(withCyan("pull"), "Update project sync point directory created by clone-local")
                                       .withEntry(withCyan("im-config"), "Config installation manager")
                                       .withEntry(withCyan("im-download"), "Download artifacts or print the list of installed ones")
                                       .withEntry(withCyan("im-install"), "Install, update artifact or print the list of already installed ones")
                                       .withEntry(withCyan("im-subscription"), "Check Codenvy subscription")
                                       .alphabeticalSort().toAscii();
        buffer.a(value);

        // Display Remotes
        buffer.a("\n");
        buffer.a("\n");
        buffer.a(getMultiRemoteCodenvy().listRemotes());
        buffer.a("\n");
        buffer.a("To add a new remote, use 'remote add <remote-name> <URL>'");

        buffer.a("\n");
        buffer.a("Use '\u001B[1m[command] --help\u001B[0m' for help on a specific command.\r\n");
        System.out.println(buffer.toString());
    }

    private String withCyan(String name) {
        return Ansi.ansi().fg(CYAN).a(name).reset().toString();
    }

    /** {@inheritDoc} */
    @Override
    protected void validateIfUserLoggedIn() throws IllegalStateException {
    }
}
