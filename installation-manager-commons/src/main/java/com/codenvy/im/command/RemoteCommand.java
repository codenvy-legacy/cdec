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
package com.codenvy.im.command;

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.AgentException;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class RemoteCommand implements Command {
    private String description;

    private String command;

    private Agent agent;

    public RemoteCommand(String command, Agent agent, String description) {
        this.command = command;
        this.agent = agent;
        this.description = description;
    }

    @Override public String execute() throws CommandException {
        try {
            return agent.execute(command);
        } catch (AgentException e) {
            String errorMessage = "Remote command execution fail.";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += format(" Error: %s", e.getMessage());
            }

            throw new CommandException(errorMessage, e);
        }
    }

    @Override public String execute(int timeoutMillis) throws CommandException {
        try {
            return agent.execute(command, timeoutMillis);
        } catch (AgentException e) {
            String errorMessage = "Remote command execution fail.";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += format(" Error: %s", e.getMessage());
            }

            throw new CommandException(errorMessage, e);
        }
    }

    @Override
    public String toString() {
        return format("'%s' command: '%s'", this.description, this.command);
    }
}
