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
import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentFileRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandFactory.createLocalPackCommand;
import static com.codenvy.im.command.SimpleCommand.createLocalAgentCommand;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class CDECSingleServerHelper extends CDECArtifactHelper {

    public CDECSingleServerHelper(CDECArtifact original) {
        super(original);
    }

    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of("Disable SELinux",
                                "Install puppet binaries",
                                "Unzip Codenvy binaries",
                                "Configure puppet master",
                                "Configure puppet agent",
                                "Launch puppet master",
                                "Launch puppet agent",
                                "Install Codenvy (~25 min)",
                                "Boot Codenvy");
    }

    @Override
    public Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    createLocalAgentFileRestoreOrBackupCommand("/etc/selinux/config"),
                    createLocalAgentCommand("if sudo test -f /etc/selinux/config; then " +
                                            "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                            "        sudo setenforce 0; " +
                                            "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                            "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                            "    fi " +
                                            "fi ")),
                                        "Disable SELinux");

            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalAgentCommand(
                        "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                        + format("then sudo yum -y -q install %s", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi"));
                    add(createLocalAgentCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_SERVER_VERSION))));
                    add(createLocalAgentCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION))));

                    if (OSUtils.getVersion().equals("6")) {
                        add(createLocalAgentCommand("sudo chkconfig --add puppetmaster"));
                        add(createLocalAgentCommand("sudo chkconfig puppetmaster on"));
                        add(createLocalAgentCommand("sudo chkconfig --add puppet"));
                        add(createLocalAgentCommand("sudo chkconfig puppet on"));
                    } else {
                        add(createLocalAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                                    " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user" +
                                                    ".target" +
                                                    ".wants/puppetmaster.service'" +
                                                    "; fi"));
                        add(createLocalAgentCommand("sudo systemctl enable puppetmaster"));
                        add(createLocalAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                                    " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                                    ".wants/puppet.service'" +
                                                    "; fi"));
                        add(createLocalAgentCommand("sudo systemctl enable puppet"));
                    }

                }}, "Install puppet binaries");

            case 2:
                return createLocalAgentCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3:
                List<Command> commands = new ArrayList<>();

                commands.add(createLocalAgentFileRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));


                commands.add(createLocalAgentCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                                            "YOUR_DNS_NAME", config.getHostUrl())));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalAgentPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(
                        createLocalAgentPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                return new MacroCommand(commands, "Configure puppet master");

            case 4:
                return new MacroCommand(ImmutableList.of(
                    createLocalAgentFileRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
                    createLocalAgentCommand("sudo sed -i '1i[master]' /etc/puppet/puppet.conf"),
                    createLocalAgentCommand(format("sudo sed -i '2i  certname = %s' /etc/puppet/puppet.conf", config.getHostUrl())),
                    createLocalAgentCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                   "  dns_alt_names = puppet,%s\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())),
                    createLocalAgentCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                   "  show_diff = true\\n" +
                                                   "  pluginsync = true\\n" +
                                                   "  report = true\\n" +
                                                   "  default_schedules = false\\n" +
                                                   "  certname = %s\\n" +
                                                   "  runinterval = 300\\n" +
                                                   "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getHostUrl()))),
                                        "Configure puppet agent");

            case 5:
                if (OSUtils.getVersion().equals("6")) {
                    return createLocalAgentCommand("sudo service puppetmaster start");
                } else {
                    return createLocalAgentCommand("sudo systemctl start puppetmaster");
                }

            case 6:
                if (OSUtils.getVersion().equals("6")) {
                    return new MacroCommand(ImmutableList.<Command>of(
                        createLocalAgentCommand("sleep 30"),
                        createLocalAgentCommand("sudo service puppet start")),
                                            "Launch puppet agent");
                } else {
                    return new MacroCommand(ImmutableList.<Command>of(
                        createLocalAgentCommand("sleep 30"),
                        createLocalAgentCommand("sudo systemctl start puppet")),
                                            "Launch puppet agent");
                }

            case 7:
                return createLocalAgentCommand("doneState=\"Installing\"; " +
                                               "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                               "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                               "    sleep 30; " +
                                               "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                               "done");

            case 8:
                return new MacroCommand(ImmutableList.of(
                    StoreIMConfigPropertyCommand.createSaveCodenvyHostDnsCommand("localhost"),
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
                commands.add(createLocalAgentCommand(format("sed -i 's/%s/%s/g' /tmp/codenvy/%s",
                                                            "YOUR_DNS_NAME",
                                                            config.getHostUrl(),
                                                            Config.SINGLE_SERVER_PROPERTIES)));
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
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
        Path backupTempDirectory = backupConfig.obtainArtifactBackupTempDirectory();
        Path backupFile = backupConfig.obtainBackupTarPack();

        // stop services
        commands.add(CommandFactory.createLocalStopServiceCommand("puppet"));
        commands.add(CommandFactory.createLocalStopServiceCommand("crond"));
        commands.add(CommandFactory.createLocalStopServiceCommand("codenvy"));
        commands.add(CommandFactory.createLocalStopServiceCommand("slapd"));

        // dump LDAP into backup directory
        Path ldapBackupPath = backupConfig.obtainBaseTempDirectory(backupTempDirectory, LDAP);
        commands.add(createLocalAgentCommand(format("mkdir -p %s", ldapBackupPath.getParent())));
        commands.add(createLocalAgentCommand(format("sudo slapcat > %s", ldapBackupPath)));

        // dump mongo into backup directory
        Path mongoBackupPath = backupConfig.obtainBaseTempDirectory(backupTempDirectory, MONGO);
        commands.add(createLocalAgentCommand(format("mkdir -p %s", mongoBackupPath)));
        commands.add(createLocalAgentCommand(format("/usr/bin/mongodump -uSuperAdmin -p%s -o %s --authenticationDatabase admin",
                                                    config.getMongoAdminPassword(),
                                                    mongoBackupPath)));

        // add dumps to the backup file
        commands.add(createLocalPackCommand(backupTempDirectory, backupFile, "."));

        // add filesystem data to the {backup_file}/fs folder
        commands.add(createLocalPackCommand(Paths.get("/home/codenvy/codenvy-data"), backupFile, "fs/."));

        // start services
        commands.add(CommandFactory.createLocalStartServiceCommand("puppet"));

        return new MacroCommand(commands, "Backup data commands");
    }
}
