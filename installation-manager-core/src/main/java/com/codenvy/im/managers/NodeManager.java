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
package com.codenvy.im.managers;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.managers.helper.AdditionalNodesConfigHelperCodenvy3;
import com.codenvy.im.managers.helper.NodeManagerHelper;
import com.codenvy.im.managers.helper.NodeManagerHelperCodenvy3Impl;
import com.codenvy.im.managers.helper.NodeManagerHelperCodenvy4Impl;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Map;

import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
@Singleton
public class NodeManager {
    private ConfigManager configManager;

    private final Map<Integer, NodeManagerHelper> HELPERS;

    @Inject
    public NodeManager(ConfigManager configManager, CDECArtifact cdecArtifact) throws IOException {
        this.configManager = configManager;

        HELPERS = ImmutableMap.of(
            3, new NodeManagerHelperCodenvy3Impl(configManager, cdecArtifact),
            4, new NodeManagerHelperCodenvy4Impl(configManager)
        );
    }

    /**
     * @param dns
     * @throws IllegalArgumentException
     *         if node type isn't supported, or if there is adding node in the list of additional nodes
     */
    public NodeConfig add(String dns) throws IOException, IllegalArgumentException {
        getHelper().checkInstallType();

        Config config = configManager.loadInstalledCodenvyConfig();
        NodeConfig addingNode = getHelper().recognizeNodeConfigFromDns(dns);

        String nodeSshUser = config.getValue(Config.NODE_SSH_USER_NAME);
        addingNode.setUser(nodeSshUser);

        String property = getHelper().getPropertyNameBy(addingNode.getType());
        if (property == null) {
            throw new IllegalArgumentException("This type of node isn't supported");
        }

        validate(addingNode);

        Command addNodeCommand = getHelper().getAddNodeCommand(addingNode, property);
        addNodeCommand.execute();

        return addingNode;
    }

    /**
     * @throws IllegalArgumentException
     *         if node type isn't supported, or if there is no removing node in the list of additional nodes
     */
    public NodeConfig remove(String dns) throws IOException, IllegalArgumentException {
        getHelper().checkInstallType();

        Config config = configManager.loadInstalledCodenvyConfig();
        NodeConfig.NodeType nodeType = getHelper().recognizeNodeTypeFromConfigBy(dns);
        if (nodeType == null) {
            throw new NodeException(format("Node '%s' is not found in Codenvy configuration", dns));
        }

        String property = getHelper().getPropertyNameBy(nodeType);
        if (property == null) {
            throw new IllegalArgumentException(format("Node type '%s' isn't supported", nodeType));
        }

        String nodeSshUser = config.getValue(Config.NODE_SSH_USER_NAME);
        NodeConfig removingNode = new NodeConfig(nodeType, dns, nodeSshUser);

        Command command = getHelper().getRemoveNodeCommand(removingNode, property);
        command.execute();

        return removingNode;
    }

    protected void validate(NodeConfig node) throws NodeException {
        String testCommand = "sudo ls";
        try {
            Command nodeCommand = getShellAgentCommand(testCommand, node);
            nodeCommand.execute();
        } catch (AgentException | CommandException e) {
            throw new NodeException(e.getMessage(), e);
        }
    }

    /** for testing propose */
    protected Command getShellAgentCommand(String command, NodeConfig node) throws AgentException {
        return createCommand(command, node);
    }

    protected NodeManagerHelper getHelper() throws IOException {
        Version codenvyVersion = Version.valueOf(configManager.loadInstalledCodenvyConfig().getValue(Config.VERSION));
        if (codenvyVersion.compareToMajor(4) < 0) {
            return HELPERS.get(3);
        } else {
            return HELPERS.get(4);
        }
    }
}
