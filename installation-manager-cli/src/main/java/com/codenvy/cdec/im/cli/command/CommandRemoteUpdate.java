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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codenvy.cli.CommandInterface;

/**
 * Parameters and execution of 'codenvy remote CDEC update' command.
 *
 * @author Alexander Reshetnyak
 */
@Parameters(commandDescription = "CDEC update remote client has access to")
public class CommandRemoteUpdate implements CommandInterface {

    @Parameter(names = {"-h", "--help"}, description = "Prints this help")
    private boolean help;

    public boolean getHelp() {
        return help;
    }

    public boolean hasSubCommands() {
        return false;
    }

    public boolean hasMandatoryParameters() {
        return false;
    }

    public String getCommandName() {
        return "cdec:update";
    }

    public String getParentCommandName() {
        return "remote";
    }

    public String getUsageLongDescription() {
        return("Usage installation cdec:update TODO.");
    }

    public String getUsageDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: codenvy remote cdec:update [<args>]");
        return sb.toString();
    }

    public String getHelpDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        return sb.toString();
    }

    public void execute() {
        System.out.println("cdec:update not yet implemented");
    }
}