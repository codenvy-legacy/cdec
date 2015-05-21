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
import com.codenvy.im.commands.CheckInstalledVersionCommand;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.commands.CommandLibrary.createCopyFromLocalToRemoteCommand;
import static com.codenvy.im.commands.CommandLibrary.createCopyFromRemoteToLocalCommand;
import static com.codenvy.im.commands.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createFileRestoreOrBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createForcePuppetAgentCommand;
import static com.codenvy.im.commands.CommandLibrary.createPackCommand;
import static com.codenvy.im.commands.CommandLibrary.createPatchCommand;
import static com.codenvy.im.commands.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.createReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.createStartServiceCommand;
import static com.codenvy.im.commands.CommandLibrary.createStopServiceCommand;
import static com.codenvy.im.commands.CommandLibrary.createUnpackCommand;
import static com.codenvy.im.commands.MacroCommand.createCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static com.codenvy.im.managers.BackupConfig.Component.LDAP;
import static com.codenvy.im.managers.BackupConfig.Component.MONGO;
import static com.codenvy.im.managers.BackupConfig.getComponentTempPath;
import static com.codenvy.im.managers.NodeConfig.extractConfigFrom;
import static com.codenvy.im.managers.NodeConfig.extractConfigsFrom;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class CDECMultiServerHelper extends CDECArtifactHelper {

    public CDECMultiServerHelper(CDECArtifact original) {
        super(original);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of(
            "Disable SELinux on nodes",
            "Install puppet master and agents on nodes",
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
                    CommandLibrary.createFileRestoreOrBackupCommand("/etc/selinux/config", nodeConfigs),
                    createCommand("if sudo test -f /etc/selinux/config; then " +
                                  "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                  "        sudo setenforce 0; " +
                                  "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                  "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                  "    fi " +
                                  "fi ", nodeConfigs),

                    // disable selinux on puppet master
                    createFileRestoreOrBackupCommand("/etc/selinux/config"),
                    createCommand("if sudo test -f /etc/selinux/config; then " +
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
                    add(createCommand(
                        "yum list installed | grep puppetlabs-release.noarch; "
                        + "if [ $? -ne 0 ]; "
                        + format("then sudo yum -y -q install %s", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi"));
                    add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_SERVER_VERSION))));

                    add(createCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                      " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user.target" +
                                      ".wants/puppetmaster.service'" +
                                      "; fi"));
                    add(createCommand("sudo systemctl enable puppetmaster"));

                    add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION))));

                    // install puppet agent
                    add(createCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                      " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user" +
                                      ".target" +
                                      ".wants/puppetmaster.service'" +
                                      "; fi"));
                    add(createCommand("sudo systemctl enable puppetmaster"));
                    add(createCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                      " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                      ".wants/puppet.service'" +
                                      "; fi"));
                    add(createCommand("sudo systemctl enable puppet"));

                    // install puppet agents on each node
                    add(createCommand(
                        "yum list installed | grep puppetlabs-release.noarch; "
                        + "if [ $? -ne 0 ]; "
                        + format("then sudo yum -y -q install %s ", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi", nodeConfigs));
                    add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION)), nodeConfigs));  // -q here is needed to avoid hung up of ssh

                    add(createCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                      " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                      ".wants/puppet.service'" +
                                      "; fi", nodeConfigs));
                    add(createCommand("sudo systemctl enable puppet", nodeConfigs));
                }}, "Install puppet binaries");

            case 2:
                return createCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3: {
                List<Command> commands = new ArrayList<>();

                commands.add(createFileRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                for (Map.Entry<String, String> e : ConfigManager.getPuppetNodesConfigReplacement(nodeConfigs).entrySet()) {
                    String replacingToken = e.getKey();
                    String replacement = e.getValue();

                    commands.add(createReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_NODES_PROPERTIES, replacingToken, replacement));
                }

                commands.add(createFileRestoreOrBackupCommand("/etc/puppet/puppet.conf"));
                commands.add(createReplaceCommand("/etc/puppet/puppet.conf",
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
                commands.add(createCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                     "  show_diff = true\\n" +
                                     "  pluginsync = true\\n" +
                                     "  report = true\\n" +
                                     "  default_schedules = false\\n" +
                                     "  certname = %s\\n" +
                                     "  runinterval = 300\\n" +
                                     "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())));

                // make it possible to sign up nodes' certificates automatically
                String autosignFileContent = "";
                for (NodeConfig node : nodeConfigs) {
                    autosignFileContent += format("%s\n", node.getHost());
                }
                commands.add(createCommand(format("sudo sh -c \"echo -e '%s' > /etc/puppet/autosign.conf\"",
                                                  autosignFileContent)));

                return new MacroCommand(commands, "Configure puppet master and agent on puppet master node");
            }

            case 4: {
                List<Command> commands = new ArrayList<>();
                commands.add(CommandLibrary.createFileRestoreOrBackupCommand("/etc/puppet/puppet.conf", nodeConfigs));
                commands.add(createCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                  "  server = %s\\n" +
                                                  "  runinterval = 420\\n" +
                                                  "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                                  config.getValue(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
                                           nodeConfigs));

                for (NodeConfig node : nodeConfigs) {
                    commands.add(createCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                      "  show_diff = true\\n" +
                                                      "  pluginsync = true\\n" +
                                                      "  report = true\\n" +
                                                      "  default_schedules = false\\n" +
                                                      "  certname = %s\\n/g' /etc/puppet/puppet.conf", node.getHost()), node));
                }

                return new MacroCommand(commands, "Configure puppet agents on each node");
            }

            case 5:
                return createCommand("sudo systemctl start puppetmaster");

            case 6:
                return new MacroCommand(ImmutableList.of(createCommand("sudo systemctl start puppet", nodeConfigs)),
                                        "Launch puppet agent");

            case 7:
                NodeConfig siteNodeConfig = extractConfigFrom(config, NodeConfig.NodeType.SITE);
                if (siteNodeConfig == null) {
                    throw new IllegalArgumentException("Site node config not found.");
                }

                return createCommand("doneState=\"Installing\"; " +
                                     "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                     "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                     "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                     "    sleep 30; " +
                                     "done", extractConfigFrom(config, NodeConfig.NodeType.SITE));

            case 8:
                return new CheckInstalledVersionCommand(original, versionToInstall);

            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }

    }

    /** {@inheritDoc} */
    @Override
    public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return createCommand(format("rm -rf /tmp/codenvy; " +
                                            "mkdir /tmp/codenvy/; " +
                                            "unzip -o %s -d /tmp/codenvy", pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createPropertyReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createPropertyReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                final List<NodeConfig> nodeConfigs = extractConfigsFrom(config);
                for (Map.Entry<String, String> e : ConfigManager.getPuppetNodesConfigReplacement(nodeConfigs).entrySet()) {
                    String replacingToken = e.getKey();
                    String replacement = e.getValue();

                    commands.add(createReplaceCommand("/tmp/codenvy/" + Config.MULTI_SERVER_NODES_PROPERTIES, replacingToken, replacement));
                }
                return new MacroCommand(commands, "Configure Codenvy");

            case 2:
                return createPatchCommand(Paths.get("/tmp/codenvy/patches/"),
                                          CommandLibrary.PatchType.BEFORE_UPDATE,
                                          installOptions);

            case 3:
                return createCommand("sudo rm -rf /etc/puppet/files; " +
                                     "sudo rm -rf /etc/puppet/modules; " +
                                     "sudo rm -rf /etc/puppet/manifests; " +
                                     "sudo rm -rf /etc/puppet/patches; " +
                                     "sudo mv /tmp/codenvy/* /etc/puppet");

            case 4:
                return new CheckInstalledVersionCommand(original, versionToUpdate);

            case 5:
                return createPatchCommand(Paths.get("/tmp/codenvy/patches/"),
                                          CommandLibrary.PatchType.AFTER_UPDATE,
                                          installOptions);

            default:
                throw new IllegalArgumentException(format("Step number %d is out of update range", step));
        }
    }

    /**
     * Given:
     * - path to backup file
     * - path to local backup dir for artifact
     * - codenvy config
     *
     * Commands:
     * - obtain config of API and DATA nodes from puppet-master config file
     * - create local temp dir for the artifact backup data
     * - stop services on API node
     * - stop services on DATA node
     * - copy backup file into api node, pack filesystem data of API node to the {backup_file}/fs folder into backup file, and then copy it to local temp dir
     * - create dump of MONGO at the DATA node, copy it to {local_backup_dir}/mongo
     * - create dump of LDAP at the DATA node, copy it to {local_backup_dir}/ldap
     * - add dumps to the local backup file
     * - start services
     * - wait until API server starts
     * - remove local temp dir
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getBackupCommand(BackupConfig backupConfig, ConfigManager codenvyConfigManager) throws IOException {
        List<Command> commands = new ArrayList<>();

        Config codenvyConfig = codenvyConfigManager.loadInstalledCodenvyConfig();
        NodeConfig apiNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.API);
        NodeConfig dataNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.DATA);

        Path localTempDir = backupConfig.obtainArtifactTempDirectory();
        Path remoteTempDir = Paths.get("/tmp/codenvy");
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // create local temp dir
        commands.add(createCommand(format("mkdir -p %s", localTempDir)));

        // stop services on API node
        commands.add(createStopServiceCommand("puppet", apiNode));
        commands.add(createStopServiceCommand("crond", apiNode));
        commands.add(createStopServiceCommand("codenvy", apiNode));
        commands.add(createStopServiceCommand("codenvy-codeassistant ", apiNode));

        // stop services on DATA node
        commands.add(createStopServiceCommand("puppet", dataNode));
        commands.add(createStopServiceCommand("crond", dataNode));
        commands.add(createStopServiceCommand("slapd", dataNode));

        // copy backup file into api node, pack filesystem data of API node to the {backup_file}/fs folder into backup file, and then copy it to local temp dir
        String backupFilename = Paths.get(backupConfig.getBackupFile()).getFileName().toString();
        Path tempApiNodeBackupFile = remoteTempDir.resolve(backupFilename);
        commands.add(createCommand(format("mkdir -p %s", tempApiNodeBackupFile.getParent()), apiNode));
        commands.add(createCopyFromLocalToRemoteCommand(backupFile,
                                                        tempApiNodeBackupFile,
                                                        apiNode));
        commands.add(createPackCommand(Paths.get("/home/codenvy/codenvy-data/data"), tempApiNodeBackupFile, "fs/.", apiNode));
        commands.add(createCopyFromRemoteToLocalCommand(tempApiNodeBackupFile,
                                                        backupFile,
                                                        apiNode));

        commands.add(createCommand(format("rm -rf %s", remoteTempDir), apiNode));  // cleanup api node

        // create dump of MONGO at the DATA node, copy it to {local_backup_dir}/mongo
        Path remoteMongoBackupPath = getComponentTempPath(remoteTempDir, MONGO);
        Path localMongoBackupPath = getComponentTempPath(localTempDir, MONGO);

        commands.add(createCommand(format("mkdir -p %s", remoteMongoBackupPath), dataNode));
        commands.add(createCommand(format("/usr/bin/mongodump -uSuperAdmin -p%s -o %s --authenticationDatabase admin",
                                          codenvyConfig.getMongoAdminPassword(),
                                          remoteMongoBackupPath), dataNode));

        Path adminDatabaseBackup = remoteMongoBackupPath.resolve("admin");
        commands.add(createCommand(format("rm -rf %s", adminDatabaseBackup), dataNode));  // remove useless 'admin' database

        commands.add(createCommand(format("mkdir -p %s", localMongoBackupPath)));
        commands.add(createCopyFromRemoteToLocalCommand(remoteMongoBackupPath,
                                                        localMongoBackupPath.getParent(),
                                                        dataNode));

        // create dump of LDAP at the DATA node, copy it to {local_backup_dir}/ldap
        Path remoteLdapBackupPath = getComponentTempPath(remoteTempDir, LDAP);
        Path localLdapBackupFilePath = getComponentTempPath(localTempDir, LDAP);

        commands.add(createCommand(format("mkdir -p %s", remoteLdapBackupPath.getParent()), dataNode));
        commands.add(createCommand(format("sudo slapcat > %s", remoteLdapBackupPath), dataNode));

        commands.add(createCommand(format("mkdir -p %s", localLdapBackupFilePath.getParent())));
        commands.add(createCopyFromRemoteToLocalCommand(remoteLdapBackupPath,
                                                        localLdapBackupFilePath,
                                                        dataNode));

        commands.add(createCommand(format("rm -rf %s", remoteTempDir), dataNode));  // cleanup data node

        // add dumps to the local backup file
        commands.add(createPackCommand(localTempDir, backupFile, ".", false));

        // start services
        commands.add(createStartServiceCommand("puppet", dataNode));
        commands.add(createStartServiceCommand("puppet", apiNode));

        // wait until API server restarts
        if (original.getInstalledVersion() != null) {
            commands.add(new CheckInstalledVersionCommand(original, original.getInstalledVersion()));
        }

        // remove local temp dir
        commands.add(createCommand(format("rm -rf %s", localTempDir)));

        return new MacroCommand(commands, "Backup data commands");
    }

    /**
     * Given:
     * - path to backup file
     * - codenvy config
     *
     * Commands:
     * - unpack backupFile into the tempDir
     * - stop services on API node
     * - stop services on DATA node
     * - restore filesystem data at the API node from {backup_file}/fs folder
     * - restore MONGO data at the DATA node from {temp_backup_directory}/mongo folder
     * - restore LDAP at the DATA node from {temp_backup_directory}/ldap/ldap.ldif file
     * - start services
     * - remove local temp dir
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getRestoreCommand(BackupConfig backupConfig, ConfigManager codenvyConfigManager) throws IOException {
        List<Command> commands = new ArrayList<>();

        Config codenvyConfig = codenvyConfigManager.loadInstalledCodenvyConfig();
        NodeConfig apiNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.API);
        NodeConfig dataNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.DATA);

        Path localTempDir = backupConfig.obtainArtifactTempDirectory();
        Path remoteTempDir = Paths.get("/tmp/codenvy");
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // unpack backupFile into the tempDir
        commands.add(createUnpackCommand(backupFile, localTempDir));

        // stop services on API node
        commands.add(createStopServiceCommand("puppet", apiNode));
        commands.add(createStopServiceCommand("crond", apiNode));
        commands.add(createStopServiceCommand("codenvy", apiNode));
        commands.add(createStopServiceCommand("codenvy-codeassistant ", apiNode));

        // stop services on DATA node
        commands.add(createStopServiceCommand("puppet", dataNode));
        commands.add(createStopServiceCommand("crond", dataNode));
        commands.add(createStopServiceCommand("slapd", dataNode));

        // restore filesystem data at the API node from {backup_file}/fs folder
        String backupFileName = Paths.get(backupConfig.getBackupFile()).getFileName().toString();
        Path apiNodeTempBackupFile = remoteTempDir.resolve(backupFileName);
        commands.add(createCommand(format("mkdir -p %s", apiNodeTempBackupFile.getParent()), apiNode));
        commands.add(createCopyFromLocalToRemoteCommand(backupFile,
                                                        apiNodeTempBackupFile,
                                                        apiNode));

        commands.add(createCommand("sudo rm -rf /home/codenvy/codenvy-data/fs", apiNode));
        commands.add(createUnpackCommand(apiNodeTempBackupFile, Paths.get("/home/codenvy/codenvy-data/data"), "fs", apiNode));

        commands.add(createCommand(format("rm -rf %s", remoteTempDir), apiNode));  // cleanup api node

        // restore MONGO data at the DATA node from {temp_backup_directory}/mongo folder
        Path remoteMongoBackupPath = getComponentTempPath(remoteTempDir, MONGO);
        Path localMongoBackupPath = getComponentTempPath(localTempDir, MONGO);

        commands.add(createCommand(format("mkdir -p %s", remoteMongoBackupPath), dataNode));
        commands.add(createCopyFromLocalToRemoteCommand(localMongoBackupPath,
                                                        remoteMongoBackupPath,
                                                        dataNode));

        commands.add(createCommand(format("/usr/bin/mongorestore -uSuperAdmin -p%s %s --authenticationDatabase admin --drop",
                                          codenvyConfig.getMongoAdminPassword(),
                                          remoteMongoBackupPath.getParent()), dataNode));

        // restore LDAP at the DATA node from {temp_backup_directory}/ldap/ldap.ldif file
        Path localLdapBackupPath = getComponentTempPath(localTempDir, LDAP);
        Path remoteLdapBackupPath = getComponentTempPath(remoteTempDir, LDAP);
        commands.add(createCommand(format("mkdir -p %s", remoteLdapBackupPath.getParent()), dataNode));
        commands.add(createCopyFromLocalToRemoteCommand(localLdapBackupPath,
                                                        remoteLdapBackupPath,
                                                        dataNode));

        commands.add(createCommand("sudo rm -rf /var/lib/ldap", dataNode));
        commands.add(createCommand("sudo mkdir -p /var/lib/ldap", dataNode));
        commands.add(createCommand(format("sudo slapadd -q <%s", remoteLdapBackupPath), dataNode));
        commands.add(createCommand("sudo chown ldap:ldap /var/lib/ldap", dataNode));
        commands.add(createCommand("sudo chown ldap:ldap /var/lib/ldap/*", dataNode));

        commands.add(createCommand(format("rm -rf %s", remoteTempDir), dataNode));  // cleanup data node

        // start services
        commands.add(createStartServiceCommand("puppet", apiNode));
        commands.add(createStartServiceCommand("puppet", dataNode));

        // wait until API server restarts
        if (original.getInstalledVersion() != null) {
            commands.add(new CheckInstalledVersionCommand(original, original.getInstalledVersion()));
        }

        // remove local temp dir
        commands.add(createCommand(format("rm -rf %s", localTempDir)));

        return new MacroCommand(commands, "Restore data commands");
    }

    @Override
    public Command getChangeConfigCommand(String property, String value, Config config) throws IOException {
        List<Command> commands = new ArrayList<>();

        // modify codenvy multi server config
        String multiServerPropertiesFilePath = Config.getPathToCodenvyConfigFile(Config.MULTI_SERVER_PROPERTIES).toString();
        commands.add(createFileBackupCommand(multiServerPropertiesFilePath));
        commands.add(createPropertyReplaceCommand(multiServerPropertiesFilePath, "$" + property, value));

        String multiServerBasePropertiesFilePath =  Config.getPathToCodenvyConfigFile(Config.MULTI_SERVER_BASE_PROPERTIES).toString();
        commands.add(createFileBackupCommand(multiServerBasePropertiesFilePath));
        commands.add(createPropertyReplaceCommand(multiServerBasePropertiesFilePath, "$" + property, value));

        // force applying updated puppet config on puppet agent at the all nodes (don't take into account additional nodes)
        final List<NodeConfig> nodes = extractConfigsFrom(config);
        for (NodeConfig node: nodes) {
            commands.add(createForcePuppetAgentCommand(node));
        }

        // wait until API server restarts
        Version installedVersion = original.getInstalledVersion();
        if (installedVersion != null) {
            commands.add(new CheckInstalledVersionCommand(original, installedVersion));
        }
        return new MacroCommand(commands, "Change config commands");
    }
}
