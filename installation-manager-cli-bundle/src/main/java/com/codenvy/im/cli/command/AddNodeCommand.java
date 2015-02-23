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

import com.codenvy.im.node.NodeConfig;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/**
 * @author Dmytro Nochevnov
 */
@Command(scope = "codenvy", name = "add-node", description = "Add node into Codenvy")
public class AddNodeCommand extends AbstractIMCommand {

    @Option(name = "--builder", aliases = "-b", description = "DNS of builder node", required = false)
    private String builderDns;

    @Option(name = "--runner", aliases = "-r", description = "DNS of runner node", required = false)
    private String runnerDns;

    @Override
    protected void doExecuteCommand() throws Exception {
        if (builderDns != null) {
            NodeConfig node = new NodeConfig(NodeConfig.NodeType.BUILDER, builderDns);
            try {
                console.showProgressor();
                console.printResponse(service.addNode(node));
            } finally {
                console.hideProgressor();
            }
        }

        if (runnerDns != null) {
            NodeConfig node = new NodeConfig(NodeConfig.NodeType.RUNNER, runnerDns);
            try {
                console.showProgressor();
                console.printResponse(service.addNode(node));
            } finally {
                console.hideProgressor();
            }
        }
    }
}
