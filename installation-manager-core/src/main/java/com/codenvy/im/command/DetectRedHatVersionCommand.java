/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.command;

import com.codenvy.im.agent.LocalAgent;

/**
 * This command retrieves the version of RedHat distribution Linux.
 *
 * @author Anatoliy Bazko
 */
// TODO [AB] test
public class DetectRedHatVersionCommand extends SimpleCommand {
    public DetectRedHatVersionCommand() {
        super("if [ ! -f /etc/redhat-release ]; then" +
              "     echo \"This isn't RedHat Linux\" 1>&2; exit 1;" +
              " else" +
              "     cat /etc/redhat-release | sed 's/.* \\([0-9.]*\\) .*/\\1/';" +
              " fi", new LocalAgent(), "Gets CentOS version");
    }
}
