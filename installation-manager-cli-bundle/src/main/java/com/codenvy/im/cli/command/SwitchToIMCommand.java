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

/**
 * This command will use im subshell and then default commands to im namespace
 *
 * @author Florent Benoit
 * @author Anatoliy Bazko
 */
@Command(scope = "prompt", name = "default-im-namespace", description = "Switch to Installation Manager shell namespace")
public class SwitchToIMCommand extends AbstractIMCommand {

    /** Change to im subshell */
    @Override
    protected Void doExecute() throws Exception {
        session.put("SCOPE", "im:*");
        session.put("SUBSHELL", "im");

        init();

        return null;
    }

    @Override
    protected void validateIfUserLoggedIn() throws IllegalStateException {
        // do nothing
    }
}
