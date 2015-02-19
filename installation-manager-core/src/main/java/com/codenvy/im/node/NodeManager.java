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
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.command.CommandFactory.createLocalAgentBackupCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentBackupCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentCommand;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
@Singleton
public class NodeManager {

    /**
     * @throws IllegalArgumentException if node type isn't supported, or if there is adding node in the list of additional nodes
     */
    public void add(NodeConfig node, String configFilePath) throws IOException, IllegalArgumentException {
        // check if Codenvy is alive
        CDECArtifact cdecArtifact = INJECTOR.getInstance(CDECArtifact.class);
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        String property = Config.ADDITIONAL_NODES_PROPERTIES.get(node.getType());
        if (property == null) {
            throw new IllegalArgumentException("This type of node can't have several instances");
        }

        Config config = getConfig(configFilePath, currentCodenvyVersion, true);
        String value = addNodeToAdditionalNodes(property, config, node);

        validate(node);

        // add node into puppet master config and wait until node becomes alive
        try {
            String puppetMasterConfigFilePath = "/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES;

            Command command = new MacroCommand(ImmutableList.of(
                // modify puppet master config
                createLocalAgentBackupCommand(puppetMasterConfigFilePath),
                createLocalAgentPropertyReplaceCommand(puppetMasterConfigFilePath,
                                                       "$" + property,
                                                       value),

                // modify local codenvy config
                createLocalAgentBackupCommand(configFilePath),
                createLocalAgentReplaceCommand(configFilePath,
                                               format("%s=.*", property),
                                               format("%s=%s", property, value)),

                // install puppet agents on adding node
                createShellAgentCommand(
                    "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                    + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                    + "; fi", node),
                createShellAgentCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_AGENT_VERSION)), node),

                createShellAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                        " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                        ".wants/puppet.service'" +
                                        "; fi", node),
                createShellAgentCommand("sudo systemctl enable puppet", node),

                // configure puppet agent
                createShellAgentBackupCommand("/etc/puppet/puppet.conf", node),
                createShellAgentCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                               "  server = %s\\n" +
                                               "  runinterval = 420\\n" +
                                               "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                               config.getProperty(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
                                        node),

                createShellAgentCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                        "  show_diff = true\\n" +
                                                        "  pluginsync = true\\n" +
                                                        "  report = true\\n" +
                                                        "  default_schedules = false\\n" +
                                                        "  certname = %s\\n/g' /etc/puppet/puppet.conf", node.getHost()), node),

                // start puppet agent
                createShellAgentCommand("sudo systemctl start puppet", node),

                // wait until runner/builder server are installed
                createShellAgentCommand("doneState=\"Installing\"; " +
                                        "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                        "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                        "    sleep 30; " +
                                        "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                        "done", node),

                // wait until API server restarts
                new CheckInstalledVersionCommand(cdecArtifact, currentCodenvyVersion)
            ), "Add node commands");
            command.execute();
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }
    }

    /**
     * @throws IllegalArgumentException if node type isn't supported, or if there is no removing node in the list of additional nodes
     */
    public void remove(String dns, String configFilePath) throws IOException, IllegalArgumentException {
        // check if Codenvy is alive
        CDECArtifact cdecArtifact = INJECTOR.getInstance(CDECArtifact.class);
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        Config config = getConfig(configFilePath, currentCodenvyVersion, true);

        NodeConfig.NodeType nodeType = config.resolveNodeTypeAmongAdditionalNodes(dns);
        if (nodeType == null) {
            throw new NodeException(format("Node '%s' is not found in Codenvy configuration among additional nodes", dns));
        }

        if (nodeType != NodeConfig.NodeType.BUILDER
            && nodeType != NodeConfig.NodeType.RUNNER) {
            throw new IllegalArgumentException(format("Node type '%s' isn't supported", nodeType));
        }

        // remove node from puppet master config
        String property = Config.ADDITIONAL_NODES_PROPERTIES.get(nodeType);
        String value = removeNodeFromAdditionalNodes(property, config, new NodeConfig(nodeType, dns));

        try {
            String puppetMasterConfigFilePath = "/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES;
            Command command = new MacroCommand(ImmutableList.of(
                // modify puppet master config
                createLocalAgentBackupCommand(puppetMasterConfigFilePath),
                createLocalAgentPropertyReplaceCommand(puppetMasterConfigFilePath,
                                                       "$" + property,
                                                       value),

                // modify local codenvy config
                createLocalAgentBackupCommand(configFilePath),
                createLocalAgentReplaceCommand(configFilePath,
                                               format("%s=.*", property),
                                               format("%s=%s", property, value)),

                // wait until API server restarts
                new CheckInstalledVersionCommand(cdecArtifact, currentCodenvyVersion)
            ), "Remove node commands");
            command.execute();
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }
    }

    protected void validate(NodeConfig node) throws NodeException {
        String testCommand = "sudo ls";
        try {
            Command nodeCommand = SimpleCommand.createShellAgentCommand(testCommand, node);
            nodeCommand.execute();
        } catch (AgentException | CommandException e) {
            throw new NodeException(e.getMessage(), e);
        }
    }

    protected Config getConfig(String configFilePath, Version currentCodenvyVersion, boolean isInstall) throws IOException {
        // get install codenvy properties
        ConfigUtil configUtil = INJECTOR.getInstance(ConfigUtil.class);
        Map<String, String> properties = configUtil.loadConfigProperties(configFilePath,
                                                                         currentCodenvyVersion,
                                                                         InstallOptions.InstallType.CODENVY_MULTI_SERVER,
                                                                         isInstall
        );

        return new Config(properties);
    }

    /**
     * @throws IllegalArgumentException if there is adding node in the list of additional nodes
     */
    protected String addNodeToAdditionalNodes(String additionalNodesProperty, Config config, NodeConfig addingNode) throws IllegalArgumentException {
        List<String> nodesUrls = config.extractCommaSeperatedValues(additionalNodesProperty);

        String nodeUrl = getAdditionalNodeUrl(addingNode);
        if (nodesUrls.contains(nodeUrl)) {
            throw new IllegalArgumentException(format("Node %s has been already used", addingNode.getHost()));
        }

        nodesUrls.add(nodeUrl);

        return StringUtils.join(nodesUrls, ',');
    }

    /**
     * @throws IllegalArgumentException if there is no removing node in the list of additional nodes
     */
    protected String removeNodeFromAdditionalNodes(String additionalNodesProperty, Config config, NodeConfig removingNode) throws IllegalArgumentException {
        List<String> nodesUrls = config.extractCommaSeperatedValues(additionalNodesProperty);

        String nodeUrl = getAdditionalNodeUrl(removingNode);
        if (!nodesUrls.contains(nodeUrl)) {
            throw new IllegalArgumentException(format("There is no node %s in the list of additional nodes", removingNode.getHost()));
        }

        nodesUrls.remove(nodeUrl);

        return StringUtils.join(nodesUrls, ",");
    }

    /**
     * @return link like "http://builder3.example.com:8080/builder/internal/builder", or "http://runner3.example.com:8080/runner/internal/runner"
     */
    private String getAdditionalNodeUrl(NodeConfig node) {
        return format("http://%1$s:8080/%2$s/internal/%2$s",
                      node.getHost(),
                      node.getType().toString().toLowerCase()
        );
    }

}
