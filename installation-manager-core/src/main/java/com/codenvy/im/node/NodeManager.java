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

import java.io.IOException;
import java.util.Map;

import static com.codenvy.im.command.CommandFactory.createLocalAgentBackupCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentBackupCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentCommand;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class NodeManager {
    private AdditionalNodesConfigUtil nodesConfigUtil;
    private Config                    config;

    public NodeManager() throws IOException {
        ConfigUtil configUtil = INJECTOR.getInstance(ConfigUtil.class);
        config = getCodenvyConfig(configUtil);
        this.nodesConfigUtil = new AdditionalNodesConfigUtil(config);
    }

    /**
     * @throws IllegalArgumentException if node type isn't supported, or if there is adding node in the list of additional nodes
     */
    public void add(NodeConfig node) throws IOException, IllegalArgumentException {
        // check if Codenvy is alive
        CDECArtifact cdecArtifact = INJECTOR.getInstance(CDECArtifact.class);
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        String property = nodesConfigUtil.getPropertyNameBy(node.getType());
        if (property == null) {
            throw new IllegalArgumentException("This type of node can't have several instances");
        }

        String value = nodesConfigUtil.getValueWithNode(node);

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

                // check if there is a puppet agent started on adding node
                // service puppet status
                //                puppet: unrecognized service


                // install puppet agents on adding node
                createShellAgentCommand(
                    "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                    + format("then sudo yum install %s -y", config.getValueOf(Config.PUPPET_RESOURCE_URL))
                    + "; fi", node),
                createShellAgentCommand(format("sudo yum install %s -y", config.getValueOf(Config.PUPPET_AGENT_VERSION)), node),

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
                                               config.getValueOf(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
                                        node),

                createShellAgentCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                        "  show_diff = true\\n" +
                                                        "  pluginsync = true\\n" +
                                                        "  report = true\\n" +
                                                        "  default_schedules = false\\n" +
                                                        "  certname = %s\\n/g' /etc/puppet/puppet.conf", node.getHost()), node),

                // start puppet agent
                createShellAgentCommand("sudo systemctl start puppet", node),



                // wait until server on additional node is installed
                createShellAgentCommand("doneState=\"Installing\"; " +
                                        "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                        "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                        "    sleep 30; " +
                                        "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                        "done", node),

                // wait until there is a changed configuration on API server

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
    public void remove(String dns) throws IOException, IllegalArgumentException {
        // check if Codenvy is alive
        CDECArtifact cdecArtifact = INJECTOR.getInstance(CDECArtifact.class);
        Version currentCodenvyVersion = cdecArtifact.getInstalledVersion();

        NodeConfig.NodeType nodeType = nodesConfigUtil.recognizeNodeTypeBy(dns);
        if (nodeType == null) {
            throw new NodeException(format("Node '%s' is not found in Codenvy configuration among additional nodes", dns));
        }

        String property = nodesConfigUtil.getPropertyNameBy(nodeType);
        if (property == null) {
            throw new IllegalArgumentException(format("Node type '%s' isn't supported", nodeType));
        }

        try {
            String value = nodesConfigUtil.getValueWithoutNode(new NodeConfig(nodeType, dns));
            String puppetMasterConfigFilePath = "/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES;
            Command command = new MacroCommand(ImmutableList.of(
                // modify puppet master config
                createLocalAgentBackupCommand(puppetMasterConfigFilePath),
                createLocalAgentPropertyReplaceCommand(puppetMasterConfigFilePath,
                                                       "$" + property,
                                                       value),

                // wait until there is changed configuration on API server

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

    protected Config getCodenvyConfig(ConfigUtil configUtil) throws IOException {
        Map<String, String> properties = configUtil.loadInstalledCodenvyProperties(InstallOptions.InstallType.CODENVY_MULTI_SERVER);
        return new Config(properties);
    }

}
