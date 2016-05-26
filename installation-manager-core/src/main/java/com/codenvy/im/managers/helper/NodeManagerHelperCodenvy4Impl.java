/*
 *  2012-2016 Codenvy, S.A.
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
package com.codenvy.im.managers.helper;

import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.commands.decorators.PuppetErrorInterrupter;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeException;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.codenvy.im.commands.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createForcePuppetAgentCommand;
import static com.codenvy.im.commands.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.createWaitServiceActiveCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class NodeManagerHelperCodenvy4Impl extends NodeManagerHelper {

    public NodeManagerHelperCodenvy4Impl(ConfigManager configManager) {
        super(configManager);
    }

    @Override
    public Command getAddNodeCommand(NodeConfig node, String property) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        AdditionalNodesConfigHelper additionalNodesConfigHelper = getNodesConfigHelper(config);

        List<Command> commands = new ArrayList<>();

        try {
            // add node into the autosign list of puppet master
            commands.add(createCommand(format("if [ \"`grep \"%1$s\" /etc/puppet/autosign.conf`\" == \"\" ]; "
                                              + "then sudo sh -c \"echo -e '%1$s' >> /etc/puppet/autosign.conf\"; "
                                              + "fi",
                                              node.getHost())));

            String puppetMasterNodeDns;
            if (configManager.detectInstallationType() == InstallType.MULTI_SERVER) {
                puppetMasterNodeDns = configManager.fetchMasterHostName();
            } else  {
                puppetMasterNodeDns = config.getHostUrl();
            }

            // install and enable puppet agent on adding node
            commands.add(createCommand("yum clean all"));   // cleanup to avoid yum install failures
            commands.add(createCommand(format("if [ \"`yum list installed | grep puppetlabs-release`\" == \"\" ]; "
                                              + "then sudo yum -y -q install %s; "
                                              + "fi", config.getValue(Config.PUPPET_RESOURCE_URL)), node));
            commands.add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_PACKAGE)), node));
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

            // log puppet messages into the /var/log/puppet/puppet-agent.log file instead of /var/log/messages
            commands.add(createCommand("sudo sh -c 'echo -e \"\\nPUPPET_EXTRA_OPTS=--logdest /var/log/puppet/puppet-agent.log\\n\" >> /etc/sysconfig/puppetagent'", node));

            // start puppet agent
            commands.add(createCommand("sudo systemctl start puppet", node));

            // force applying updated puppet config at the adding node
            commands.add(createForcePuppetAgentCommand(node));

            // wait until docker on additional node is installed by puppet; interrupt on puppet puppet errors;
            commands.add(new PuppetErrorInterrupter(createWaitServiceActiveCommand("docker", node),
                                                    node,
                                                    configManager));

            // --- register node in the swarm
            // add node dns to the $swarm_nodes as a separate row
            String value = additionalNodesConfigHelper.getValueWithNode(node);
            Iterator<Path> propertiesFiles = configManager.getCodenvyPropertiesFiles(configManager.detectInstallationType());
            while (propertiesFiles.hasNext()) {
                Path file = propertiesFiles.next();

                commands.add(createFileBackupCommand(file));
                commands.add(createPropertyReplaceCommand(file, "$" + property, value));
            }

            // force applying updated puppet config on puppet agent locally
            commands.add(createForcePuppetAgentCommand());

            // wait until adding node is among registered nodes in the output of request on puppet master: curl http://{host_url}:23750/info
            // (see log in file /var/log/swarm.log on puppet master)
            commands.add(createCommand(format("doneState=\"Checking\"; " +
                                              "while [ \"${doneState}\" != \"Done\" ]; do " +
                                              "    curl http://%s:23750/info | grep '%s'; " +
                                              "    if [ $? -eq 0 ]; then doneState=\"Done\"; " +
                                              "    else sleep 5; " +
                                              "    fi; " +
                                              "done",
                                              config.getHostUrl(),
                                              node.getHost())));
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }

        return new MacroCommand(commands, "Add node commands");
    }

    @Override
    public Command getRemoveNodeCommand(NodeConfig node, String property) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        AdditionalNodesConfigHelper additionalNodesConfigHelper = getNodesConfigHelper(config);

        List<Command> commands = new ArrayList<>();
        try {
            String value = additionalNodesConfigHelper.getValueWithoutNode(node);

            // remove node's dns from the swarm_nodes config property
            Iterator<Path> propertiesFiles = configManager.getCodenvyPropertiesFiles(configManager.detectInstallationType());
            while (propertiesFiles.hasNext()) {
                Path file = propertiesFiles.next();

                commands.add(createFileBackupCommand(file));
                commands.add(createPropertyReplaceCommand(file, "$" + property, value));
            }

            // force applying updated puppet config on puppet agent locally
            commands.add(createForcePuppetAgentCommand());

            // wait until there is no removing node in the /usr/local/swarm/node_list
            commands.add(createCommand(format("testFile=\"/usr/local/swarm/node_list\"; " +
                                  "while true; do " +
                                  "    if ! sudo grep \"%s\" ${testFile}; then break; fi; " +
                                  "    sleep 5; " +  // sleep 5 sec
                                  "done; ", node.getHost())));

            // remove out-date puppet agent's certificate
            commands.add(createCommand(format("sudo puppet cert clean %s", node.getHost())));
            commands.add(createCommand("sudo systemctl restart puppetmaster"));

            // stop puppet agent on removing node and remove out-date certificate
            commands.add(createCommand("sudo systemctl stop puppet", node));
            commands.add(createCommand("sudo rm -rf /var/lib/puppet/ssl", node));
        } catch (Exception e) {
            throw new NodeException(e.getMessage(), e);
        }

        return new MacroCommand(commands, "Remove node commands");        
    }

    @Override
    public void checkInstallType() throws IllegalStateException {
        // Adding/removing nodes are supported in Single-Server and Multi-Server Codenvy.
        // So, do nothing.
    }

    @Override
    public AdditionalNodesConfigHelper getNodesConfigHelper(Config config) {
        return new AdditionalNodesConfigHelperCodenvy4Impl(config);
    }

    @Override 
    public NodeConfig.NodeType recognizeNodeTypeFromConfigBy(String dns) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        String additionalNodes = config.getValueWithoutSubstitution(Config.SWARM_NODES);   // don't substitute enclosed variables like the "$host_url"

        if (additionalNodes != null && additionalNodes.contains(dns)) {
            return NodeConfig.NodeType.MACHINE;
        }

        return null;
    }
}
