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
import com.codenvy.im.commands.decorators.PuppetErrorInterrupter;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.TarUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.commands.CommandLibrary.createCompressCommand;
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
import static com.codenvy.im.commands.CommandLibrary.createUncompressCommand;
import static com.codenvy.im.commands.CommandLibrary.createUnpackCommand;
import static com.codenvy.im.commands.MacroCommand.createCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static com.codenvy.im.managers.BackupConfig.Component.ANALYTICS_DATA;
import static com.codenvy.im.managers.BackupConfig.Component.ANALYTICS_LOGS;
import static com.codenvy.im.managers.BackupConfig.Component.FS;
import static com.codenvy.im.managers.BackupConfig.Component.LDAP;
import static com.codenvy.im.managers.BackupConfig.Component.LDAP_ADMIN;
import static com.codenvy.im.managers.BackupConfig.Component.MONGO;
import static com.codenvy.im.managers.BackupConfig.Component.MONGO_ANALYTICS;
import static com.codenvy.im.managers.BackupConfig.getComponentTempPath;
import static com.codenvy.im.managers.NodeConfig.extractConfigFrom;
import static com.codenvy.im.managers.NodeConfig.extractConfigsFrom;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class CDECMultiServerHelper extends CDECArtifactHelper {

    public CDECMultiServerHelper(CDECArtifact original, ConfigManager configManager) {
        super(original, configManager);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo() throws IOException {
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
                                                      "  certname = %s\\n" +
                                                      "  syslogfacility = local6\\n/g' /etc/puppet/puppet.conf", node.getHost()), node));
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

                Command command = createCommand("doneState=\"Installing\"; " +
                                                "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                                "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                                "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                                "    sleep 30; " +
                                                "done", extractConfigFrom(config, NodeConfig.NodeType.SITE));
                return new PuppetErrorInterrupter(command, nodeConfigs, configManager);

            case 8:
                return new PuppetErrorInterrupter(new CheckInstalledVersionCommand(original, versionToInstall), nodeConfigs, configManager);

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
                return new PuppetErrorInterrupter(new CheckInstalledVersionCommand(original, versionToUpdate), configManager);

            case 5:
                return createPatchCommand(Paths.get("/etc/puppet/patches/"),
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
     * - re-create local temp dir for the artifact backup data
     * - stop services on API node
     * - stop services on DATA node
     * - stop services on ANALYTICS node
     * - copy backup file into api node, pack filesystem data of API node to the {backup_file}/fs folder into backup file, and then copy it to local temp dir
     * - create dump of MONGO at the DATA node, copy it to {local_backup_dir}/mongo
     * - create dump of MONGO_ANALYTICS at the ANALYTICS node, copy it to {local_backup_dir}/mongo_analytics
     * - compress analytics data dir of ANALYTICS node into analytics_data.tar.gz, copy pack into local temp dir and unpack into {local_backup_dir}/analytics_data
     * - copy logs dir of ANALYTICS node into remote temp dir, compress them into analytics_logs.tar.gz, copy pack into local temp dir and unpack into {local_backup_dir}/analytics_logs
     * - create dump of LDAP user db at the DATA node, copy it to {local_backup_dir}/ldap
     * - create dump of LDAP_ADMIN db at the DATA node, copy it to {local_backup_dir}/ldap_admin
     * - add dumps to the local backup file
     * - start services
     * - wait until API server starts
     * - remove local and remote temp dirs
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getBackupCommand(BackupConfig backupConfig) throws IOException {
        List<Command> commands = new ArrayList<>();

        Config codenvyConfig = configManager.loadInstalledCodenvyConfig();
        NodeConfig apiNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.API);
        NodeConfig dataNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.DATA);
        NodeConfig analyticsNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.ANALYTICS);

        Path localTempDir = backupConfig.obtainArtifactTempDirectory();
        Path remoteTempDir = Paths.get("/tmp/codenvy");
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // re-create local temp dir
        commands.add(createCommand(format("rm -rf %s", localTempDir)));
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

        // stop services on ANALYTICS node
        commands.add(createStopServiceCommand("codenvy", analyticsNode));
        commands.add(createStopServiceCommand("puppet", analyticsNode));

        // copy backup file into api node, pack filesystem data of API node to the {backup_file}/api/fs folder into backup file, and then copy it to local temp dir
        String backupFilename = Paths.get(backupConfig.getBackupFile()).getFileName().toString();
        Path remoteBackupFile = remoteTempDir.resolve(backupFilename);
        commands.add(createCommand(format("mkdir -p %s", remoteBackupFile.getParent()), apiNode));
        commands.add(createCopyFromLocalToRemoteCommand(backupFile,
                                                        remoteBackupFile,
                                                        apiNode));
        commands.add(createPackCommand(Paths.get("/home/codenvy/codenvy-data/data"), remoteBackupFile, "fs/.", apiNode));
        commands.add(createCopyFromRemoteToLocalCommand(remoteBackupFile,
                                                        backupFile,
                                                        apiNode));

        // create dump of MONGO at the DATA node, copy it to {local_backup_dir}/mongo
        Path remoteMongoBackupPath = getComponentTempPath(remoteTempDir, MONGO);
        Path localMongoBackupPath = getComponentTempPath(localTempDir, MONGO);

        commands.add(createCommand(format("mkdir -p %s", remoteMongoBackupPath), dataNode));
        commands.add(createCommand(format("/usr/bin/mongodump -u%s -p%s -o %s --authenticationDatabase admin > /dev/null",  // suppress stdout to avoid hanging up SecureSSh
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                          remoteMongoBackupPath), dataNode));

        Path adminDatabaseBackup = remoteMongoBackupPath.resolve("admin");
        commands.add(createCommand(format("rm -rf %s", adminDatabaseBackup), dataNode));  // remove useless 'admin' database

        commands.add(createCommand(format("mkdir -p %s", localMongoBackupPath)));
        commands.add(createCopyFromRemoteToLocalCommand(remoteMongoBackupPath,
                                                        localMongoBackupPath.getParent(),
                                                        dataNode));

        // create dump of MONGO at the ANALYTICS node, copy it to {local_backup_dir}/mongo_analytics
        Path remoteMongoAnalyticsBackupPath = getComponentTempPath(remoteTempDir, MONGO_ANALYTICS);
        Path localMongoAnalyticsBackupPath = getComponentTempPath(localTempDir, MONGO_ANALYTICS);
        commands.add(createCommand(format("mkdir -p %s", remoteMongoAnalyticsBackupPath), analyticsNode));
        commands.add(createCommand(format("/usr/bin/mongodump -u%s -p%s -o %s --authenticationDatabase admin > /dev/null",  // suppress stdout to avoid hanging up SecureSSh
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                          remoteMongoAnalyticsBackupPath), analyticsNode));

        adminDatabaseBackup = remoteMongoAnalyticsBackupPath.resolve("admin");
        commands.add(createCommand(format("rm -rf %s", adminDatabaseBackup), analyticsNode));  // remove useless 'admin' database

        commands.add(createCommand(format("mkdir -p %s", localMongoAnalyticsBackupPath)));
        commands.add(createCopyFromRemoteToLocalCommand(remoteMongoAnalyticsBackupPath,
                                                        localMongoAnalyticsBackupPath.getParent(),
                                                        analyticsNode));

        // compress analytics data dir of ANALYTICS node into analytics_data.tar.gz, copy pack into local temp dir and unpack into {local_backup_dir}/analytics_data
        String tempAnalyticsDataBackupFileName = ANALYTICS_DATA.toString().toLowerCase() + ".tar.gz";
        Path remoteAnalyticsDataBackupFile = remoteTempDir.resolve(tempAnalyticsDataBackupFileName);
        Path localAnalyticsDataBackupFile = localTempDir.resolve(tempAnalyticsDataBackupFileName);
        Path localAnalyticsDataBackupPath = getComponentTempPath(localTempDir, ANALYTICS_DATA);

        commands.add(createCompressCommand(Paths.get("/home/codenvy/analytics_data"), remoteAnalyticsDataBackupFile, ".", analyticsNode));
        commands.add(createCopyFromRemoteToLocalCommand(remoteAnalyticsDataBackupFile,
                                                        localTempDir,
                                                        analyticsNode));

        commands.add(createCommand(format("mkdir -p %s", localAnalyticsDataBackupPath)));
        commands.add(createUncompressCommand(localAnalyticsDataBackupFile, localAnalyticsDataBackupPath));
        commands.add(createCommand(format("rm -rf %s", localAnalyticsDataBackupFile)));


        // copy logs dir of ANALYTICS node into remote temp dir, compress them into analytics_logs.tar.gz, copy pack into local temp dir and unpack into {local_backup_dir}/analytics_logs
        commands.add(createCommand(format("sudo cp -r /home/codenvy/logs %s", remoteTempDir), analyticsNode));  // we need to make copy to avoid error "tar: ./2015/07/17/messages: file changed as we read it"

        String tempAnalyticsLogsBackupFileName = ANALYTICS_LOGS.toString().toLowerCase() + ".tar.gz";
        Path remoteAnalyticsLogsBackupFile = remoteTempDir.resolve(tempAnalyticsLogsBackupFileName);
        Path localAnalyticsLogsBackupFile = localTempDir.resolve(tempAnalyticsLogsBackupFileName);
        Path localAnalyticsLogsBackupPath = getComponentTempPath(localTempDir, ANALYTICS_LOGS);
        commands.add(createCompressCommand(remoteTempDir.resolve("logs"), remoteAnalyticsLogsBackupFile, ".", analyticsNode));
        commands.add(createCopyFromRemoteToLocalCommand(remoteAnalyticsLogsBackupFile,
                                                        localTempDir,
                                                        analyticsNode));

        commands.add(createCommand(format("mkdir -p %s", localAnalyticsLogsBackupPath)));
        commands.add(createUncompressCommand(localAnalyticsLogsBackupFile, localAnalyticsLogsBackupPath));
        commands.add(createCommand(format("rm -rf %s", localAnalyticsLogsBackupFile)));

        // create dump of LDAP user db at the DATA node, copy it to {local_backup_dir}/ldap
        Path remoteLdapUserBackupPath = getComponentTempPath(remoteTempDir, LDAP);
        Path localLdapUserBackupPath = getComponentTempPath(localTempDir, LDAP);

        commands.add(createCommand(format("mkdir -p %s", remoteLdapUserBackupPath.getParent()), dataNode));
        commands.add(createCommand(format("sudo slapcat > %s", remoteLdapUserBackupPath), dataNode));

        commands.add(createCommand(format("mkdir -p %s", localLdapUserBackupPath.getParent())));
        commands.add(createCopyFromRemoteToLocalCommand(remoteLdapUserBackupPath,
                                                        localLdapUserBackupPath,
                                                        dataNode));

        // create dump of LDAP_ADMIN db at the DATA node, copy it to {local_backup_dir}/ldap_admin
        Path remoteLdapAdminBackupPath = getComponentTempPath(remoteTempDir, LDAP_ADMIN);
        Path localLdapAdminBackupPath = getComponentTempPath(localTempDir, LDAP_ADMIN);

        commands.add(createCommand(format("mkdir -p %s", remoteLdapAdminBackupPath.getParent()), dataNode));
        commands.add(createCommand(format("sudo slapcat -b '%s'> %s",
                                          codenvyConfig.getValue(Config.ADMIN_LDAP_DN),
                                          remoteLdapAdminBackupPath), dataNode));

        commands.add(createCommand(format("mkdir -p %s", localLdapAdminBackupPath.getParent())));
        commands.add(createCopyFromRemoteToLocalCommand(remoteLdapAdminBackupPath,
                                                        localLdapAdminBackupPath,
                                                        dataNode));

        commands.add(createCommand(format("rm -rf %s", remoteTempDir), dataNode));  // cleanup data node

        // add dumps to the local backup file
        commands.add(createPackCommand(localTempDir, backupFile, ".", false));

        // start services
        commands.add(createStartServiceCommand("puppet", dataNode));
        commands.add(createStartServiceCommand("puppet", analyticsNode));
        commands.add(createStartServiceCommand("puppet", apiNode));

        // wait until API server restarts
        if (original.getInstalledVersion() != null) {
            commands.add(new CheckInstalledVersionCommand(original, original.getInstalledVersion()));
        }

        // remove local temp dir
        commands.add(createCommand(format("rm -rf %s", localTempDir)));

        // remove remote temp dirs
        commands.add(createCommand(format("sudo rm -rf %s", remoteTempDir), dataNode));
        commands.add(createCommand(format("sudo rm -rf %s", remoteTempDir), analyticsNode));
        commands.add(createCommand(format("sudo rm -rf %s", remoteTempDir), dataNode));
        commands.add(createCommand(format("sudo rm -rf %s", remoteTempDir), apiNode));

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
     * - stop services on ANALYTICS node
     * - restore filesystem data at the API node from {backup_file}/fs folder
     * - restore MONGO data at the DATA node from {temp_backup_directory}/mongo folder
     * - restore MONGO_ANALYTICS data at the DATA node from {temp_backup_directory}/mongo_analytics folder
     * - restore ANALYTICS_DATA at the ANALYTICS node from {backup_file }/analytics_data
     * - restore LDAP at the DATA node from {temp_backup_directory}/ldap/ldap.ldif file
     * - start services
     * - remove local and remote temp dirs
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getRestoreCommand(BackupConfig backupConfig) throws IOException {
        List<Command> commands = new ArrayList<>();

        Config codenvyConfig = configManager.loadInstalledCodenvyConfig();
        NodeConfig apiNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.API);
        NodeConfig dataNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.DATA);
        NodeConfig analyticsNode = NodeConfig.extractConfigFrom(codenvyConfig, NodeConfig.NodeType.ANALYTICS);

        Path localTempDir = backupConfig.obtainArtifactTempDirectory();
        Path remoteTempDir = Paths.get("/tmp/codenvy");
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // unpack backupFile into the tempDir
        TarUtils.unpackAllFiles(backupFile, localTempDir);

        // stop services on API node
        commands.add(createStopServiceCommand("puppet", apiNode));
        commands.add(createStopServiceCommand("crond", apiNode));
        commands.add(createStopServiceCommand("codenvy", apiNode));
        commands.add(createStopServiceCommand("codenvy-codeassistant ", apiNode));

        // stop services on DATA node
        commands.add(createStopServiceCommand("puppet", dataNode));
        commands.add(createStopServiceCommand("crond", dataNode));
        commands.add(createStopServiceCommand("slapd", dataNode));

        // stop services on ANALYTICS node
        commands.add(createStopServiceCommand("puppet", analyticsNode));

        // restore filesystem data at the API node from {backup_file}/fs folder
        String backupFileName = Paths.get(backupConfig.getBackupFile()).getFileName().toString();
        Path remoteBackupFile = remoteTempDir.resolve(backupFileName);
        Path localFsBackupPath = getComponentTempPath(localTempDir, FS);
        if (Files.exists(localFsBackupPath)) {
            commands.add(createCommand(format("mkdir -p %s", remoteBackupFile.getParent()), apiNode));
            commands.add(createCopyFromLocalToRemoteCommand(backupFile,
                                                            remoteBackupFile,
                                                            apiNode));

            commands.add(createCommand("sudo rm -rf /home/codenvy/codenvy-data/data/fs", apiNode));
            commands.add(createUnpackCommand(remoteBackupFile, Paths.get("/home/codenvy/codenvy-data/data"), "fs", apiNode));
            commands.add(createCommand("sudo chown -R codenvy:codenvy /home/codenvy/codenvy-data/data/fs", apiNode));
        }

        // restore MONGO data at the DATA node from {temp_backup_directory}/mongo folder
        Path remoteMongoBackupPath = getComponentTempPath(remoteTempDir, MONGO);
        Path localMongoBackupPath = getComponentTempPath(localTempDir, MONGO);
        if (Files.exists(localMongoBackupPath)) {
            commands.add(createCommand(format("mkdir -p %s", remoteMongoBackupPath), dataNode));
            commands.add(createCopyFromLocalToRemoteCommand(localMongoBackupPath,
                                                            remoteMongoBackupPath.getParent(),
                                                            dataNode));

            // remove all databases except 'admin' one
            commands.add(createCommand(format("/usr/bin/mongo -u %s -p %s --authenticationDatabase admin --quiet --eval " +
                                              "'db.getMongo().getDBNames().forEach(function(d){if (d!=\"admin\") db.getSiblingDB(d).dropDatabase()})'",
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY)), dataNode));

            commands.add(createCommand(format("/usr/bin/mongorestore -u%s -p%s %s --authenticationDatabase admin --drop > /dev/null",  // suppress stdout to avoid hanging up SecureSSH
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                              remoteMongoBackupPath.getParent()), dataNode));
        }

        // restore MONGO data at the ANALYTICS node from {temp_backup_directory}/mongo_analytics folder
        Path remoteMongoAnalyticsBackupPath = getComponentTempPath(remoteTempDir, MONGO_ANALYTICS);
        Path localMongoAnalyticsBackupPath = getComponentTempPath(localTempDir, MONGO_ANALYTICS);
        if (Files.exists(localMongoAnalyticsBackupPath)) {
            commands.add(createCommand(format("mkdir -p %s", remoteMongoAnalyticsBackupPath), analyticsNode));
            commands.add(createCopyFromLocalToRemoteCommand(localMongoAnalyticsBackupPath,
                                                            remoteMongoAnalyticsBackupPath.getParent(),
                                                            analyticsNode));

            // remove all databases expect 'admin' one
            commands.add(createCommand(format("/usr/bin/mongo -u %s -p %s --authenticationDatabase admin --quiet --eval 'db.getMongo().getDBNames().forEach(function(d){if (d!=\"admin\") db.getSiblingDB(d).dropDatabase()})'",
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY)), analyticsNode));

            commands.add(createCommand(format("/usr/bin/mongorestore -u%s -p%s %s --authenticationDatabase admin --drop > /dev/null",  // suppress stdout to avoid hanging up SecureSSh
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                              remoteMongoAnalyticsBackupPath.getParent()), analyticsNode));
        }

        // restore ANALYTICS_DATA at the ANALYTICS node from {backup_file}/analytics_data
        Path localAnalyticsDataBackupPath = getComponentTempPath(localTempDir, ANALYTICS_DATA);
        Path remoteAnalyticsDataBackupPath = getComponentTempPath(remoteTempDir, ANALYTICS_DATA);
        if (Files.exists(localAnalyticsDataBackupPath)) {
            commands.add(createCopyFromLocalToRemoteCommand(localAnalyticsDataBackupPath,
                                                            remoteAnalyticsDataBackupPath,
                                                            analyticsNode));
            commands.add(createCommand("sudo rm -rf /home/codenvy/analytics_data", analyticsNode));
            commands.add(createCommand(format("sudo cp -r %s /home/codenvy", remoteAnalyticsDataBackupPath), analyticsNode));
            commands.add(createCommand("sudo chown -R codenvy:codenvy /home/codenvy/analytics_data", analyticsNode));
        }

        // restore LDAP user db from {temp_backup_directory}/ldap/ladp.ldif file
        Path localLdapUserBackupPath = getComponentTempPath(localTempDir, LDAP);
        Path remoteLdapUserBackupPath = getComponentTempPath(remoteTempDir, LDAP);
        if (Files.exists(localLdapUserBackupPath)) {
            commands.add(createCommand("sudo rm -rf /var/lib/ldap", dataNode));
            commands.add(createCommand("sudo mkdir -p /var/lib/ldap", dataNode));

            commands.add(createCommand(format("mkdir -p %s", remoteLdapUserBackupPath.getParent()), dataNode));
            commands.add(createCopyFromLocalToRemoteCommand(localLdapUserBackupPath,
                                                            remoteLdapUserBackupPath,
                                                            dataNode));
            commands.add(createCommand(format("sudo slapadd -q <%s", remoteLdapUserBackupPath), dataNode));

            commands.add(createCommand("sudo chown -R ldap:ldap /var/lib/ldap", dataNode));
        }

        // restore LDAP_ADMIN db from {temp_backup_directory}/ldap_admin/ladp.ldif file
        Path localLdapAdminBackupPath = getComponentTempPath(localTempDir, LDAP_ADMIN);
        Path remoteLdapAdminBackupPath = getComponentTempPath(remoteTempDir, LDAP_ADMIN);
        if (Files.exists(localLdapAdminBackupPath)) {
            commands.add(createCommand("sudo rm -rf /var/lib/ldapcorp", dataNode));
            commands.add(createCommand("sudo mkdir -p /var/lib/ldapcorp", dataNode));

            commands.add(createCommand(format("mkdir -p %s", remoteLdapAdminBackupPath.getParent()), dataNode));
            commands.add(createCopyFromLocalToRemoteCommand(localLdapAdminBackupPath,
                                                            remoteLdapAdminBackupPath,
                                                            dataNode));
            commands.add(createCommand(format("sudo slapadd -q -b '%s' <%s",
                                              codenvyConfig.getValue(Config.ADMIN_LDAP_DN),
                                              remoteLdapAdminBackupPath), dataNode));

            commands.add(createCommand("sudo chown -R ldap:ldap /var/lib/ldapcorp", dataNode));
        }

        // start services
        commands.add(createStartServiceCommand("slapd", dataNode));
        commands.add(createStartServiceCommand("puppet", dataNode));
        commands.add(createStartServiceCommand("puppet", analyticsNode));
        commands.add(createStartServiceCommand("puppet", apiNode));

        // wait until API server restarts
        if (original.getInstalledVersion() != null) {
            commands.add(new CheckInstalledVersionCommand(original, original.getInstalledVersion()));
        }

        // remove local temp dir
        commands.add(createCommand(format("rm -rf %s", localTempDir)));

        // remove remote temp dirs
        commands.add(createCommand(format("rm -rf %s", remoteTempDir), dataNode));
        commands.add(createCommand(format("rm -rf %s", remoteTempDir), analyticsNode));
        commands.add(createCommand(format("rm -rf %s", remoteTempDir), dataNode));
        commands.add(createCommand(format("rm -rf %s", remoteTempDir), apiNode));

        return new MacroCommand(commands, "Restore data commands");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Command getUpdateConfigCommand(Config config, Map<String, String> properties) throws IOException {
        List<Command> commands = new ArrayList<>();

        // modify codenvy multi server config
        String multiServerPropertiesFilePath = configManager.getPuppetConfigFile(Config.MULTI_SERVER_PROPERTIES).toString();
        commands.add(createFileBackupCommand(multiServerPropertiesFilePath));
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            commands.add(createPropertyReplaceCommand(multiServerPropertiesFilePath, "$" + entry.getKey(), entry.getValue()));
        }

        String multiServerBasePropertiesFilePath = configManager.getPuppetConfigFile(Config.MULTI_SERVER_BASE_PROPERTIES).toString();
        commands.add(createFileBackupCommand(multiServerBasePropertiesFilePath));
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            commands.add(createPropertyReplaceCommand(multiServerBasePropertiesFilePath, "$" + entry.getKey(), entry.getValue()));
        }

        // force applying updated puppet config on puppet agent at the all nodes (don't take into account additional nodes)
        final List<NodeConfig> nodes = extractConfigsFrom(config);
        for (NodeConfig node : nodes) {
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
