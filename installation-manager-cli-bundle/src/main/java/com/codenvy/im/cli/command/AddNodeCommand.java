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
package com.codenvy.im.cli.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "add-node", description = "Add node into Codenvy")
public class AddNodeCommand extends AbstractIMCommand {

    @Argument(name = "dns", description = "DNS name of adding node.", required = true, multiValued = false, index = 0)
    private String dns;

    @Override
    protected void doExecuteCommand() throws Exception {
        if (dns != null && !dns.isEmpty()) {
            try {
                console.showProgressor();
                console.printResponse(service.addNode(dns));
            } finally {
                console.hideProgressor();
            }
        }
    }
}
