/*
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
package com.codenvy.im.installer;

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.agent.SecureShellAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.RemoteCommand;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class Installer {
    private static final Logger LOG = LoggerFactory.getLogger(Installer.class);
    private LinkedList<Command> commands;
    private InstallOptions options;
    private Path pathToBinaries;

    public Installer(Path pathToBinaries, Type installType) throws ConfigException, AgentException {
        this.commands = getInstallCommands();
        this.pathToBinaries = pathToBinaries;
        this.options = new InstallOptions()
            .setType(installType)
            .setCommandsInfo(getCommandsInfo())
            .setId(UUID.randomUUID());

        LOG.info("\n--- " + toString());
    }

    public void executeNextCommand() {
        if (isFinished()) {
            return;
        }

        Command command = commands.pollFirst();

        LOG.info(format("\n--- Executing command %s...\n", command.toString()));
        String result = command.execute();
        LOG.info(format("Result: %s", result));
    }

    public boolean isFinished() {
        return commands.isEmpty();
    }

    /**
     * @throws ConfigException if required config parameter isn't present in configuration.
     * @throws AgentException if required agent isn't ready to perform commands.
     */
    private LinkedList<Command> getInstallCommands() throws AgentException, ConfigException {
        CdecConfig config = ConfigFactory.loadConfig(options.getType().toString());
        LOG.info(format("\n--- Config file '%s' is used to install CDEC.", config.getConfigSource()));

        return options.getType().getCommands(config, pathToBinaries);
    }

    private List<String> getCommandsInfo() {
        List<String> commandsInfo = new ArrayList<>();

        for (Command command: commands) {
            commandsInfo.add(command.toString());
        }

        return commandsInfo;
    }

    @Override
    public String toString() {
        return format("Installation '%s', type '%s', commands list: ",
                      options.getId(),
                      options.getType(),
                      Arrays.toString(getCommandsInfo().toArray()));
    }

    public InstallOptions getOptions() {
        return options;
    }

    public enum Type {
        CDEC_SINGLE_NODE_WITH_PUPPET_MASTER {
            @Override protected LinkedList<Command> getCommands(final CdecConfig config, Path pathToBinaries) throws
                                                                                                           AgentException,
                                                                                                           ConfigException {
                // check readiness of config
                if (config.getHost().isEmpty() || config.getSSHPort().isEmpty()) {
                    throw new ConfigException(format("Installation config file '%s' is incomplete.", config.getConfigSource()));
                }

                LinkedList<Command> commands = new LinkedList<>();

                final Agent agent;
                // select use password or auth key
                if (!config.getPassword().isEmpty()) {
                    agent = new SecureShellAgent(
                        config.getHost(),
                        Integer.valueOf(config.getSSHPort()),
                        config.getUser(),
                        config.getPassword()
                    );
                } else {
                    agent = new SecureShellAgent(
                        config.getHost(),
                        Integer.valueOf(config.getSSHPort()),
                        config.getUser(),
                        config.getPrivateKeyFileAbsolutePath(),
                        null
                    );
                }


                // TODO issue CDEC-59

                /**
                 * CDEC installation sequence.
                 * 1) On Puppet Master host :
                 * 1.1 Validate all hosts name and set their if need. //TODO set ?
                 * 1.2 Add rule in firewall for puppet master.
                 * 1.3 Install puppet master;
                 * 1.4 Install unzip if need //TODO
                 * 1.5 Upload CDEC in puppet master.
                 *
                 * // http://www.jcraft.com/jsch/examples/ScpTo.java.html
                 *
                 * 1.6 Unzip CDEC in puppet;
                 *
                 * ssh.execute("sudo unzip " + remouteBinariesPath + " /etc/puppet/");
                 *
                 * 1.7 Configure CDEC in puppet master;
                 * 1.8 Start puppet master
                 *
                 * ssh.execute("sudo service puppetmaster start");
                 *
                 * 2) On other hosts :
                 * 1.1 Validate all hosts name and set their if need; //TODO set ?
                 * 1.2 Install puppet client;
                 * 1.3 Configure puppet client;
                 *
                 * String result = ssh.execute("sudo iptables " + rule); //TODO or echo in /etc/sysconfig/iptables
                 *
                 * 1.4 Start puppet client;
                 * 3) Sign nodes connection request on puppet master;
                 * 1.1 Validate nodes requests available;
                 * 1.2 Sign nodes connection request.
                 */

                commands.addAll(new ArrayList<Command>() {{
                    add(new RemoteCommand("sudo setenforce 0", agent, "Disable SELinux"));
                    add(new RemoteCommand("sudo cp /etc/selinux/config /etc/selinux/config.bak", agent, "Disable SELinux"));
                    add(new RemoteCommand("sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config", agent,
                                          "Disable SELinux"));
                }});

                commands.addAll(new ArrayList<Command>() {{
                    add(new RemoteCommand(format("sudo rpm -ivh %s", config.getPuppetResourceUrl()), agent, "Install puppet client"));
                    add(new RemoteCommand(format("sudo yum install %s -y", config.getPuppetVersion()), agent, "Install puppet client"));
                }});

                return commands;
            }
        },

        CDEC_MULTI_NODES_WITH_PUPPET_MASTER {
            /** TODO issue CDEC-60 */
            @Override protected LinkedList<Command> getCommands(final CdecConfig config, Path pathToBinaries) throws
                                                                                                           AgentException,
                                                                                                           ConfigException {
                //                CdecConfig installationConfig = new CdecConfig();
                //
                //                installPuppetMaster(installationConfig.getPuppetMaster(),
                //                                    installationConfig.getHostsName(),
                //                                    pathToBinaries);
                //
                //                for (PuppetClientConfig clientConfig : installationConfig.getPuppetClients()) {
                //                    //            installPuppetClient(clientConfig, insta);
                //                }
                //
                //                signNodesOnPuppetMaster();
                return new LinkedList<>();
            }
        };

        protected LinkedList<Command> getCommands(final CdecConfig config, Path pathToBinaries) {
            return new LinkedList<>();
        }
    }
}
