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
package com.codenvy.im.artifacts.helper;

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.command.CheckInstalledVersionCommand;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.CommandFactory;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.StoreIMConfigPropertyCommand;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.service.InstallationManagerConfig;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.backup.BackupConfig.Component.LDAP;
import static com.codenvy.im.backup.BackupConfig.Component.MONGO;
import static com.codenvy.im.command.CommandFactory.createCopyFromRemoteToLocalCommand;
import static com.codenvy.im.command.CommandFactory.createLocalPackCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentFileRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandFactory.createRemotePackCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentFileRestoreOrBackupCommand;
import static com.codenvy.im.command.SimpleCommand.createLocalAgentCommand;
import static com.codenvy.im.node.NodeConfig.extractConfigFrom;
import static com.codenvy.im.node.NodeConfig.extractConfigsFrom;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class CDECMultiServerHelper extends CDECArtifactHelper {

    public CDECMultiServerHelper(CDECArtifact original) {
        super(original);
    }

    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of(
            "Disable SELinux on nodes",
            "Install puppet master on master node and puppet agents on other nodes",
            "Unzip Codenvy binaries",
            "Configure puppet master",
            "Configure puppet agents for each node",
            "Launch puppet master",
            "Launch puppet agents on each node",
            "Install Codenvy (~10 min)",
            "Boot Codenvy (~10 min)"
        );
    }

    /**
     * @throws com.codenvy.im.agent.AgentException if there was a problem with LocalAgent or SecureShellAgent instantiation.
     * @throws IllegalArgumentException if there is no Site node config.
     * @throws IllegalStateException if local OS version != 7
     */
    @Override
    public Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException,
                                                                                                                          IllegalArgumentException,
                                                                                                                          IllegalStateException  {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        if (!OSUtils.getVersion().equals("7")) {
            throw new IllegalStateException("Multi-server installation is supported only on CentOS 7");
        }

        final List<NodeConfig> nodeConfigs = extractConfigsFrom(config);
        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    // disable selinux on puppet agent
                    createShellAgentFileRestoreOrBackupCommand("/etc/selinux/config", nodeConfigs),
                    createShellAgentCommand("if sudo test -f /etc/selinux/config; then " +
                                            "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                            "        sudo setenforce 0; " +
                                            "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                            "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                            "    fi " +
                                            "fi ", nodeConfigs),

                    // disable selinux on puppet master
                    createLocalAgentFileRestoreOrBackupCommand("/etc/selinux/config"),
                    createLocalAgentCommand("if sudo test -f /etc/selinux/config; then " +
                                            "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                            "        sudo setenforce 0; " +
                                            "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                            "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                            "    fi " +
                                            "fi ")

                ), "Disable SELinux");


            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    // install puppet master
                    add(createLocalAgentCommand(
                        "yum list installed | grep puppetlabs-release.noarch; "
                        + "if [ $? -ne 0 ]; "
                        + format("then sudo yum -y -q install %s", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi"));
                    add(createLocalAgentCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_SERVER_VERSION))));

                    add(createLocalAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                                " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user.target" +
                                                ".wants/puppetmaster.service'" +
                                                "; fi"));
                    add(createLocalAgentCommand("sudo systemctl enable puppetmaster"));

                    // install puppet agents on each node
                    add(createShellAgentCommand(
                        "yum list installed | grep puppetlabs-release.noarch; "
                        + "if [ $? -ne 0 ]; "
                        + format("then sudo yum -y -q install %s ", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi", nodeConfigs));
                    add(createShellAgentCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION)), nodeConfigs));  // -q here is needed to avoid hung up of ssh

                    add(createShellAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                                " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                                ".wants/puppet.service'" +
                                                "; fi", nodeConfigs));
                    add(createShellAgentCommand("sudo systemctl enable puppet", nodeConfigs));

                    // open puppet master to puppet agent
                    add(createLocalAgentCommand("systemctl | grep iptables.service; "  // disable 'iptables' service
                                                + "if [ $? -eq 0 ]; "
                                                + "then "
                                                + "  sudo service iptables stop; "
                                                + "fi; "

                                                // install 'firewalld', if needed
                                                + "yum list installed | grep firewalld; "
                                                + "if [ $? -ne 0 ]; "
                                                + "then "
                                                + "  sudo yum -y -q install firewalld; "
                                                + "fi; "

                                                // start 'firewalld' service, if needed
                                                + "systemctl | grep firewalld.service; "
                                                + "if [ $? -ne 0 ]; "
                                                + "then "
                                                + "  sudo service firewalld start; "
                                                + "fi; "

                                                // open firewall port 8140 permanently
                                                // http://stackoverflow.com/questions/24729024/centos-7-open-firewall-port
                                                + "sudo firewall-cmd --zone=public --add-port=8140/tcp --permanent; "
                                                + "sudo firewall-cmd --reload; "));
                }}, "Install puppet binaries");

            case 2:
                return createLocalAgentCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3: {
                List<Command> commands = new ArrayList<>();

                commands.add(createLocalAgentFileRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalAgentPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalAgentPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                for (Map.Entry<String, String> e : ConfigUtil.getPuppetNodesConfigReplacement(nodeConfigs).entrySet()) {
                    String replacingToken = e.getKey();
                    String replacement = e.getValue();

                    commands.add(createLocalAgentReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_NODES_PROPERTIES, replacingToken, replacement));
                }

                commands.add(createLocalAgentFileRestoreOrBackupCommand("/etc/puppet/puppet.conf"));
                commands.add(createLocalAgentReplaceCommand("/etc/puppet/puppet.conf",
                                                            "\\[main\\]",
                                                            format("\\[master\\]\\n" +
                                                                   "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                                   "    ca = true\\n" +
                                                                   "    ssldir = /var/lib/puppet/ssl\\n" +
                                                                   "    pluginsync = true\\n" +
                                                                   "\\n" +
                                                                   "\\[main\\]\\n" +
                                                                   "    certname = %s\\n" +
                                                                   "    privatekeydir = $ssldir/private_keys { group = service }\\n" +
                                                                   "    hostprivkey = $privatekeydir/$certname.pem { mode = 640 }\\n" +
                                                                   "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                                   "\\n",
                                                                   config.getValue(Config.PUPPET_MASTER_HOST_NAME_PROPERTY))));

                // remove "[agent]" section
                commands.add(createLocalAgentReplaceCommand("/etc/puppet/puppet.conf",
                                                            "\\[agent\\].*",
                                                            ""));

                // make it possible to sign up nodes' certificates automatically
                String autosignFileContent = "";
                for (NodeConfig node : nodeConfigs) {
                    autosignFileContent += format("%s\n", node.getHost());
                }
                commands.add(createLocalAgentCommand(format("sudo sh -c \"echo -e '%s' > /etc/puppet/autosign.conf\"",
                                                            autosignFileContent)));

                return new MacroCommand(commands, "Configure puppet master");
            }

            case 4: {
                List<Command> commands = new ArrayList<>();
                commands.add(createShellAgentFileRestoreOrBackupCommand("/etc/puppet/puppet.conf", nodeConfigs));
                commands.add(createShellAgentCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                            "  server = %s\\n" +
                                                            "  runinterval = 420\\n" +
                                                            "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                                            config.getValue(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
                                                     nodeConfigs));

                for (NodeConfig node : nodeConfigs) {
                    commands.add(createShellAgentCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                                "  show_diff = true\\n" +
                                                                "  pluginsync = true\\n" +
                                                                "  report = true\\n" +
                                                                "  default_schedules = false\\n" +
                                                                "  certname = %s\\n/g' /etc/puppet/puppet.conf", node.getHost()), node));
                }

                return new MacroCommand(commands, "Configure puppet agent");
            }

            case 5:
                return createLocalAgentCommand("sudo systemctl start puppetmaster");

            case 6:
                return new MacroCommand(ImmutableList.of(createShellAgentCommand("sudo systemctl start puppet", nodeConfigs)),
                                        "Launch puppet agent");

            case 7:
                NodeConfig siteNodeConfig = extractConfigFrom(config, NodeConfig.NodeType.SITE);
                if (siteNodeConfig == null) {
                    throw new IllegalArgumentException("Site node config not found.");
                }

                return createShellAgentCommand("doneState=\"Installing\"; " +
                                               "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                               "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                               "    sleep 30; " +
                                               "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                               "done", extractConfigFrom(config, NodeConfig.NodeType.SITE));

            case 8:
                return new MacroCommand(ImmutableList.of(
                    StoreIMConfigPropertyCommand.createSaveCodenvyHostDnsCommand(config.getHostUrl()),
                    StoreIMConfigPropertyCommand.createSavePuppetMasterHostDnsCommand(config.getValue(InstallationManagerConfig.PUPPET_MASTER_HOST_NAME)),
                    new CheckInstalledVersionCommand(original, versionToInstall)
                ),
                                        "Check if Codenvy has already installed");

            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }

    }

    @Override
    public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return createLocalAgentCommand(format("rm -rf /tmp/codenvy; " +
                                                      "mkdir /tmp/codenvy/; " +
                                                      "unzip -o %s -d /tmp/codenvy", pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                final List<NodeConfig> nodeConfigs = extractConfigsFrom(config);
                for (Map.Entry<String, String> e : ConfigUtil.getPuppetNodesConfigReplacement(nodeConfigs).entrySet()) {
                    String replacingToken = e.getKey();
                    String replacement = e.getValue();

                    commands.add(createLocalAgentReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_NODES_PROPERTIES, replacingToken, replacement));
                }
                return new MacroCommand(commands, "Configure Codenvy");

            case 2:
                return CommandFactory.createPatchCommand(Paths.get("/tmp/codenvy/patches/"), original.getInstalledVersion(), versionToUpdate);

            case 3:
                return createLocalAgentCommand("sudo rm -rf /etc/puppet/files; " +
                                               "sudo rm -rf /etc/puppet/modules; " +
                                               "sudo rm -rf /etc/puppet/manifests; " +
                                               "sudo mv /tmp/codenvy/* /etc/puppet");

            case 4:
                return new CheckInstalledVersionCommand(original, versionToUpdate);

            default:
                throw new IllegalArgumentException(format("Step number %d is out of update range", step));
        }
    }

    @Override
    public Command getBackupCommand(BackupConfig backupConfig, ConfigUtil codenvyConfigUtil) throws IOException {
        List<Command> commands = new ArrayList<>();

        Config config = codenvyConfigUtil.loadInstalledCodenvyConfig(original.getInstalledType());
        NodeConfig apiNode = NodeConfig.extractConfigFrom(config, NodeConfig.NodeType.API);
        NodeConfig dataNode = NodeConfig.extractConfigFrom(config, NodeConfig.NodeType.DATA);

        Path localBackupTempDirectory = backupConfig.obtainArtifactBackupTempDirectory();
        Path remoteBackupTempDirectory = Paths.get("/tmp/codenvy");
        Path backupFile = backupConfig.obtainBackupTarPack();

        // stop services on API node
        commands.add(CommandFactory.createRemoteStopServiceCommand("puppet", apiNode));
        commands.add(CommandFactory.createRemoteStopServiceCommand("crond", apiNode));
        commands.add(CommandFactory.createRemoteStopServiceCommand("codenvy", apiNode));
        commands.add(CommandFactory.createRemoteStopServiceCommand("codenvy-codeassistant ", apiNode));

        // stop services on DATA node
        commands.add(CommandFactory.createRemoteStopServiceCommand("puppet", dataNode));
        commands.add(CommandFactory.createRemoteStopServiceCommand("crond", dataNode));
        commands.add(CommandFactory.createRemoteStopServiceCommand("slapd", dataNode));

        // add filesystem data from API node to the {backup_file}/fs folder, and copy it to local node at the first
        Path tempApiNodeBackupFile = remoteBackupTempDirectory.resolve(backupConfig.getBackupFile().getFileName().toString());
        commands.add(createShellAgentCommand(format("mkdir -p %s", tempApiNodeBackupFile.getParent()), apiNode));
        commands.add(createRemotePackCommand(Paths.get("/home/codenvy/codenvy-data/data"), tempApiNodeBackupFile, "fs/.", apiNode));

        commands.add(createCopyFromRemoteToLocalCommand(tempApiNodeBackupFile,
                                                        backupFile,
                                                        apiNode));

        commands.add(createShellAgentCommand(format("rm -rf %s", remoteBackupTempDirectory), apiNode));

        // dump mongo from DATA node to local backup directory
        Path remoteMongoBackupPath = backupConfig.obtainBaseTempDirectory(remoteBackupTempDirectory, MONGO);
        Path localMongoBackupPath = backupConfig.obtainBaseTempDirectory(localBackupTempDirectory, MONGO);

        commands.add(createShellAgentCommand(format("mkdir -p %s", remoteMongoBackupPath), dataNode));
        commands.add(createShellAgentCommand(format("/usr/bin/mongodump -uSuperAdmin -p%s -o %s --authenticationDatabase admin",
                                                    config.getMongoAdminPassword(),
                                                    remoteMongoBackupPath), dataNode));

        commands.add(createLocalAgentCommand(format("mkdir -p %s", localMongoBackupPath)));
        commands.add(createCopyFromRemoteToLocalCommand(remoteMongoBackupPath,
                                                        localMongoBackupPath.getParent(),
                                                        dataNode));

        commands.add(createShellAgentCommand(format("rm -rf %s", localMongoBackupPath), dataNode));

        // dump LDAP from DATA node to local backup directory
        Path remoteLdapBackupPath = backupConfig.obtainBaseTempDirectory(remoteBackupTempDirectory, LDAP);
        Path localLdapBackupPath = backupConfig.obtainBaseTempDirectory(localBackupTempDirectory, LDAP);

        commands.add(createShellAgentCommand(format("mkdir -p %s", remoteLdapBackupPath.getParent()), dataNode));
        commands.add(createShellAgentCommand(format("sudo slapcat > %s", remoteLdapBackupPath), dataNode));

        commands.add(createLocalAgentCommand(format("mkdir -p %s", localLdapBackupPath.getParent())));
        commands.add(createCopyFromRemoteToLocalCommand(remoteLdapBackupPath,
                                                        localLdapBackupPath,
                                                        dataNode));

        commands.add(createShellAgentCommand(format("rm -rf %s", remoteLdapBackupPath), dataNode));

        // add dumps to the backup file
        commands.add(createLocalPackCommand(localBackupTempDirectory, backupFile, "."));

        // start services
        commands.add(CommandFactory.createRemoteStartServiceCommand("puppet", apiNode));
        commands.add(CommandFactory.createRemoteStartServiceCommand("puppet", dataNode));

        return new MacroCommand(commands, "Backup data commands");
    }
}
