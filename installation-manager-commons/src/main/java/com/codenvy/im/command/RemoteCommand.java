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

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class RemoteCommand implements Command {
    private String description;

    private Map<String, String> defaultParameters = new HashMap<>();

    private String commandTemplate;

    private Agent agent;

    public RemoteCommand(String commandTemplate, Agent agent, String description) {
        this.commandTemplate = commandTemplate;
        this.agent = agent;
        this.description = description;
    }

    public RemoteCommand(String commandTemplate, Agent agent, String description, Map<String, String> defaultParameters) {
        this.commandTemplate = commandTemplate;
        this.agent = agent;
        this.description = description;
        this.defaultParameters = defaultParameters;
    }

    @Override public String execute() throws CommandException {
        try {
            return agent.execute(commandTemplate, defaultParameters);
        } catch (AgentException e) {
            throw new CommandException(toString(), e);
        }
    }

    @Override public String execute(int timeoutMillis) throws CommandException {
        try {
            return agent.execute(commandTemplate, timeoutMillis, defaultParameters);
        } catch (AgentException e) {
            throw new CommandException(toString(), e);
        }
    }

    @Override public String execute(Map<String, String> parameters) throws CommandException {
        try {
            return agent.execute(commandTemplate, parameters);
        } catch (AgentException e) {
            throw new CommandException(format("Error of executing next command: %s", toString(parameters)), e);
        }
    }

    @Override public String execute(int timeoutMillis, Map<String, String> parameters) throws CommandException {
        try {
            return agent.execute(commandTemplate, timeoutMillis, parameters);
        } catch (AgentException e) {
            throw new CommandException(toString(parameters), e);
        }
    }

    @Override public String toString() {
        return toString(defaultParameters);
    }

    public String toString(Map<String, String> parameters) {
        return format("Remote command: '%s'. Description: '%s'. Parameters: '%s'.",
                      this.commandTemplate,
                      this.description,
                      parameters);
    }
}
