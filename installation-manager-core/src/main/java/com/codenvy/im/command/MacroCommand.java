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

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.config.NodeConfig;

import java.util.ArrayList;
import java.util.List;

/** @author Dmytro Nochevnov */
public class MacroCommand implements Command {
    private final String        description;
    private final List<Command> commands;

    public MacroCommand(List<Command> commands, String description) {
        this.commands = commands;
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        StringBuilder results = new StringBuilder();
        for (Command command : commands) {
            String result = command.execute();
            results.append(result).append("\n");
        }
        return results.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return commands.toString();
    }

    /**
     * Factory method. Creates MacroCommand which includes identical list of commands
     * with {@link com.codenvy.im.agent.SecureShellAgent} to be applied at every node in the given list.
     */
    public static Command createShellAgentCommand(String command, String description, List<NodeConfig> nodes) throws AgentException {
        final List<Command> commands = new ArrayList<>(nodes.size());
        for (NodeConfig node : nodes) {
            commands.add(SimpleCommand.createShellAgentCommand(command, node));
        }

        return new MacroCommand(commands, description);
    }

}
