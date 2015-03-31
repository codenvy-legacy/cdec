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
package com.codenvy.im.node;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.command.CheckInstalledVersionCommand;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.im.command.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.command.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.command.ReadMasterHostNameCommand.fetchMasterHostName;
import static com.codenvy.im.command.SimpleCommand.createCommand;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
@Singleton
public class NodeManager {
    private ConfigUtil   configUtil;
    private CDECArtifact cdecArtifact;

    @Inject
    public NodeManager(ConfigUtil configUtil, CDECArtifact cdecArtifact) throws IOException {
        this.configUtil = configUtil;
        this.cdecArtifact = cdecArtifact;
    }

    /**
     * @throws IllegalArgumentException if node type isn't supported, or if there is adding node in the list of additional nodes
     * @param dns
     */
    public NodeConfig add(String dns) throws IOException, IllegalArgumentException {
        if (!InstallType.MULTI_SERVER.equals(cdecArtifact.getInstalledType())) {
            throw new IllegalStateException("You can add node to Multi-Server Codenvy only");
        }

        Config config = getCodenvyConfig(configUtil);
        AdditionalNodesConfigUtil nodesConfigUtil = getNodesConfigUtil(config);
        NodeConfig node = nodesConfigUtil.recognizeNodeConfigFromDns(dns);

        // check if Codenvy is alive
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        String property = nodesConfigUtil.getPropertyNameBy(node.getType());
        if (property == null) {
            throw new IllegalArgumentException("This type of node isn't supported");
        }

        validate(node);

        Command addNodeCommand = getAddNodeCommand(currentCodenvyVersion, property, nodesConfigUtil, node, config);
        addNodeCommand.execute();

        return node;
    }

    /**
     * @return commands to add node into puppet master config and wait until node becomes alive
     */
    protected Command getAddNodeCommand(Version currentCodenvyVersion, String property, AdditionalNodesConfigUtil nodesConfigUtil, NodeConfig node, Config config) throws NodeException {
        List<Command> commands = new ArrayList<>();

        String value = nodesConfigUtil.getValueWithNode(node);
        NodeConfig apiNode = NodeConfig.extractConfigFrom(config, NodeConfig.NodeType.API);

        try {
            // modify puppet master config
            String puppetMasterConfigFilePath = "/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES;
            commands.add(createFileBackupCommand(puppetMasterConfigFilePath));
            commands.add(createPropertyReplaceCommand(puppetMasterConfigFilePath,
                                                      "$" + property,
                                                      value));

            // add node into the autosign list of puppet master
            commands.add(createCommand(format("sudo sh -c \"echo -e '%s' >> /etc/puppet/autosign.conf\"",
                                              node.getHost())));

            String puppetMasterNodeDns = fetchMasterHostName();

            // install puppet agents on adding node
            commands.add(createCommand("yum list installed | grep puppetlabs-release.noarch; "
                                       + "if [ $? -ne 0 ]; "
                                       + format("then sudo yum -y -q install %s", config.getValue(Config.PUPPET_RESOURCE_URL))
                                       + "; fi",
                                       node));
            commands.add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION)), node));

            commands.add(createCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                       " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                       ".wants/puppet.service'" +
                                       "; fi",
                                       node));
            commands.add(createCommand("sudo systemctl enable puppet", node));

            // configure puppet agent
            commands.add(createFileBackupCommand("/etc/puppet/puppet.conf", node));
            commands.add(createCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                              "  server = %s\\n" +
                                              "  runinterval = 420\\n" +
                                              "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                              puppetMasterNodeDns),
                                       node));

            commands.add(createCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                              "  show_diff = true\\n" +
                                              "  pluginsync = true\\n" +
                                              "  report = true\\n" +
                                              "  default_schedules = false\\n" +
                                              "  certname = %s\\n/g' /etc/puppet/puppet.conf",
                                              node.getHost()),
                                       node));

            // start puppet agent
            commands.add(createCommand("sudo systemctl start puppet", node));

            // wait until server on additional node is installed
            commands.add(createCommand("doneState=\"Installing\"; " +
                                       "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                       "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                       "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                       "    sleep 30; " +
                                       "done",
                                       node));

            // wait until there is a changed configuration on API server
            commands.add(createCommand(format("testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; " +
                                              "while true; do " +
                                              "    if sudo grep \"%s$\" ${testFile}; then break; fi; " +
                                              "    sleep 5; " +  // sleep 5 sec
                                              "done; " +
                                              "sleep 15; # delay to involve into start of rebooting api server", value),
                                       apiNode));

            // wait until API server restarts
            commands.add(new CheckInstalledVersionCommand(cdecArtifact, currentCodenvyVersion));
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }

        return new MacroCommand(commands, "Add node commands");
    }

    /**
     * @throws IllegalArgumentException if node type isn't supported, or if there is no removing node in the list of additional nodes
     */
    public NodeConfig remove(String dns) throws IOException, IllegalArgumentException {
        if (!InstallType.MULTI_SERVER.equals(cdecArtifact.getInstalledType())) {
            throw new IllegalStateException("You can remove node from Multi-Server Codenvy only");
        }

        Config config = getCodenvyConfig(configUtil);
        AdditionalNodesConfigUtil nodesConfigUtil = getNodesConfigUtil(config);

        // check if Codenvy is alive
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        NodeConfig.NodeType nodeType = nodesConfigUtil.recognizeNodeTypeFromConfigBy(dns);
        if (nodeType == null) {
            throw new NodeException(format("Node '%s' is not found in Codenvy configuration among additional nodes", dns));
        }

        String property = nodesConfigUtil.getPropertyNameBy(nodeType);
        if (property == null) {
            throw new IllegalArgumentException(format("Node type '%s' isn't supported", nodeType));
        }

        Command command = getRemoveNodeCommand(new NodeConfig(nodeType, dns), config, nodesConfigUtil, currentCodenvyVersion, property);
        command.execute();

        return new NodeConfig(nodeType, dns);
    }

    protected Command getRemoveNodeCommand(NodeConfig node,
                                          Config config,
                                          AdditionalNodesConfigUtil nodesConfigUtil,
                                          Version currentCodenvyVersion,
                                          String property) throws NodeException {
        try {
            String value = nodesConfigUtil.getValueWithoutNode(node);
            String puppetMasterConfigFilePath = "/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES;
            NodeConfig apiNode = NodeConfig.extractConfigFrom(config, NodeConfig.NodeType.API);

            return new MacroCommand(ImmutableList.of(
                // modify puppet master config
                createFileBackupCommand(puppetMasterConfigFilePath),
                createPropertyReplaceCommand(puppetMasterConfigFilePath,
                                             "$" + property,
                                             value),

                // wait until there node is removed from configuration on API server
                createCommand(format("testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; " +
                                     "while true; do " +
                                     "    if ! sudo grep \"%s\" ${testFile}; then break; fi; " +
                                     "    sleep 5; " +  // sleep 5 sec
                                     "done; " +
                                     "sleep 15; # delay to involve into start of rebooting api server", node.getHost()),
                              apiNode),

                // wait until API server restarts
                new CheckInstalledVersionCommand(cdecArtifact, currentCodenvyVersion),

                // remove out-date puppet agent's certificate
                createCommand(format("sudo puppet cert clean %s", node.getHost())),
                createCommand("sudo service puppetmaster restart"),

                // stop puppet agent on removing node and remove out-date certificate
                createCommand("sudo service puppet stop", node),
                createCommand("sudo rm -rf /var/lib/puppet/ssl", node)
            ), "Remove node commands");
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }
    }

    protected boolean isPuppetAgentActive(NodeConfig node) throws AgentException {
        Command getPuppetAgentStatusCommand = getShellAgentCommand("sudo service puppet status", node);

        String result;
        try {
            result = getPuppetAgentStatusCommand.execute();
        } catch (CommandException e) {
            return false;
        }

        return result != null
               && result.contains("Loaded: loaded")
               && result.contains("(running)");
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

    protected Config getCodenvyConfig(ConfigUtil configUtil) throws IOException {
        return configUtil.loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);
    }

    /** for testing propose */
    protected AdditionalNodesConfigUtil getNodesConfigUtil(Config config) {
        return new AdditionalNodesConfigUtil(config);
    }
}
