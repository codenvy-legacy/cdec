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

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.agent.SecureShellAgent;
import com.codenvy.im.config.NodeConfig;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class SimpleCommand implements Command {
    private final String description;
    private final String command;
    private final Agent  agent;

    public SimpleCommand(String command, Agent agent, String description) {
        this.agent = agent;
        this.description = description;
        this.command = command;
    }

    /** Factory method */
    public static SimpleCommand createLocalCommand(String command) {
        return new SimpleCommand(command, new LocalAgent(), null);
    }

    /** Factory method */
    public static SimpleCommand createShellCommand(String command,
                                                   final String host,
                                                   final int port,
                                                   final String user,
                                                   final String privateKeyFilePath) throws AgentException {
        return new SimpleCommand(command,
                                 new SecureShellAgent(host, port, user, privateKeyFilePath, null),
                                 null);
    }

    public static Command createShellCommandForNode(String command, NodeConfig node) throws AgentException {
        return SimpleCommand.createShellCommand(
            command,
            node.getHost(),
            node.getPort(),
            node.getUser(),
            node.getPrivateKeyFile()
        );
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        try {
            return agent.execute(command);
        } catch (AgentException e) {
            throw makeCommandException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return format("{'command'='%s', 'agent'='%s'}", command, agent);
    }

    protected CommandException makeCommandException(Exception e) {
        String errorMessage = "Command execution fail.";
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorMessage += format(" Error: %s", e.getMessage());
        }

        return new CommandException(errorMessage, e);
    }
}
