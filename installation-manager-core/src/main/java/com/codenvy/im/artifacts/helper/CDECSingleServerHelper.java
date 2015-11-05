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
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.commands.WaitOnAliveArtifactCommand;
import com.codenvy.im.commands.WaitOnAliveArtifactOfCorrectVersionCommand;
import com.codenvy.im.commands.decorators.PuppetErrorInterrupter;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.TarUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import org.eclipse.che.commons.annotation.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.commands.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createFileRestoreOrBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createForcePuppetAgentCommand;
import static com.codenvy.im.commands.CommandLibrary.createPackCommand;
import static com.codenvy.im.commands.CommandLibrary.createPatchCommand;
import static com.codenvy.im.commands.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.createStartServiceCommand;
import static com.codenvy.im.commands.CommandLibrary.createStopServiceCommand;
import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static com.codenvy.im.managers.BackupConfig.Component.FS;
import static com.codenvy.im.managers.BackupConfig.Component.LDAP;
import static com.codenvy.im.managers.BackupConfig.Component.LDAP_ADMIN;
import static com.codenvy.im.managers.BackupConfig.Component.MONGO;
import static com.codenvy.im.managers.BackupConfig.getComponentTempPath;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class CDECSingleServerHelper extends CDECArtifactHelper {

    public CDECSingleServerHelper(CDECArtifact original, ConfigManager configManager) {
        super(original, configManager);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo() throws IOException {
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

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(Version versionToInstall, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    createFileRestoreOrBackupCommand("/etc/selinux/config"),
                    createCommand("if sudo test -f /etc/selinux/config; then " +
                                  "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                  "        sudo setenforce 0; " +
                                  "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                  "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                  "    fi " +
                                  "fi ")),
                                        "Disable SELinux");

            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createCommand("if ! sudo grep -Eq \"127.0.0.1.*puppet\" /etc/hosts; then\n" +
                                      " echo '127.0.0.1 puppet' | sudo tee --append /etc/hosts > /dev/null\n" +
                                      "fi"));
                    add(createCommand(format("if ! sudo grep -Fq \"%1$s\" /etc/hosts; then\n" +
                                             "  echo \"127.0.0.1 %1$s\" | sudo tee --append /etc/hosts > /dev/null\n" +
                                             "fi", config.getHostUrl())));
                    add(createCommand(
                        "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                        + format("then sudo yum -y -q install %s", config.getValue(Config.PUPPET_RESOURCE_URL))
                        + "; fi"));
                    add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_SERVER_VERSION))));
                    add(createCommand(format("sudo yum -y -q install %s", config.getValue(Config.PUPPET_AGENT_VERSION))));

                    if (OSUtils.getVersion().equals("6")) {
                        add(createCommand("sudo chkconfig --add puppetmaster"));
                        add(createCommand("sudo chkconfig puppetmaster on"));
                        add(createCommand("sudo chkconfig --add puppet"));
                        add(createCommand("sudo chkconfig puppet on"));
                    } else {
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
                    }

                }}, "Install puppet binaries");

            case 2:
                return createCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3:
                List<Command> commands = new ArrayList<>();

                commands.add(createFileRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));


                commands.add(createCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                                  "YOUR_DNS_NAME", config.getHostUrl())));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(
                        createPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                return new MacroCommand(commands, "Configure puppet master");

            case 4:
                return new MacroCommand(ImmutableList.of(
                    createFileRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
                    createCommand("sudo sed -i '1i[master]' /etc/puppet/puppet.conf"),
                    createCommand(format("sudo sed -i '2i  certname = %s' /etc/puppet/puppet.conf", config.getHostUrl())),
                    createCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                         "  dns_alt_names = puppet,%s\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())),
                    createCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                         "  show_diff = true\\n" +
                                         "  pluginsync = true\\n" +
                                         "  report = true\\n" +
                                         "  default_schedules = false\\n" +
                                         "  certname = %s\\n" +
                                         "  runinterval = 300\\n" +
                                         "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())),
                    // log puppet messages into the /var/log/puppet/puppet-agent.log file instead of /var/log/messages
                    createCommand("sudo sh -c 'echo -e \"\\nPUPPET_EXTRA_OPTS=--logdest /var/log/puppet/puppet-agent.log\\n\" >> /etc/sysconfig/puppetagent'")),
                                        "Configure puppet agent");

            case 5:
                if (OSUtils.getVersion().equals("6")) {
                    return createCommand("sudo service puppetmaster start");
                } else {
                    return createCommand("sudo systemctl start puppetmaster");
                }

            case 6:
                if (OSUtils.getVersion().equals("6")) {
                    return new MacroCommand(ImmutableList.<Command>of(
                        createCommand("sleep 30"),
                        createCommand("sudo service puppet start")),
                                            "Launch puppet agent");
                } else {
                    return new MacroCommand(ImmutableList.<Command>of(
                        createCommand("sleep 30"),
                        createCommand("sudo systemctl start puppet")),
                                            "Launch puppet agent");
                }

            case 7:
                Command command = createCommand("doneState=\"Installing\"; " +
                                                "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                                "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                                "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                                "    sleep 30; " +
                                                "done");
                return new PuppetErrorInterrupter(command, configManager);

            case 8:
                return new PuppetErrorInterrupter(new WaitOnAliveArtifactCommand(original), configManager);

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
                return createCommand(format("rm -rf %1$s; " +
                                            "mkdir %1$s; " +
                                            "unzip -o %2$s -d %1$s", getTmpCodenvyDir(), pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                commands.add(createCommand(format("sed -i 's/YOUR_DNS_NAME/%s/g' %s",
                                                  config.getHostUrl(),
                                                  Paths.get(getTmpCodenvyDir(), Config.SINGLE_SERVER_PROPERTIES))));
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createPropertyReplaceCommand(getTmpCodenvyDir() + "/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createPropertyReplaceCommand(getTmpCodenvyDir() + "/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }
                return new MacroCommand(commands, "Configure Codenvy");

            case 2:
                return createPatchCommand(Paths.get(getTmpCodenvyDir(), "patches"),
                                          CommandLibrary.PatchType.BEFORE_UPDATE,
                                          installOptions);

            case 3:
                return createCommand(format("sudo rm -rf %1$s/files; " +
                                            "sudo rm -rf %1$s/modules; " +
                                            "sudo rm -rf %1$s/manifests; " +
                                            "sudo rm -rf %1$s/patches; " +
                                            "sudo mv %2$s/* %1$s", getPuppetDir(), getTmpCodenvyDir()));

            case 4:
                return new PuppetErrorInterrupter(new WaitOnAliveArtifactOfCorrectVersionCommand(original, versionToUpdate), configManager);

            case 5:
                return createPatchCommand(Paths.get(getPuppetDir(), "patches"),
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
     * - create temp dir
     * - stop services
     * - dump LDAP user db into {backup_directory}/ldap/ldap.ldif file
     * - dump LDAP_ADMIN db into {backup_directory}/ldap_admin/ldap.ldif file
     * - dump MONGO data into {backup_directory}/mongo dir
     * - pack dumps into backup file
     * - pack filesystem data into the {backup_file}/fs folder
     * - start services
     * - wait until API server starts
     * - remove temp dir
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getBackupCommand(BackupConfig backupConfig) throws IOException {
        List<Command> commands = new ArrayList<>();
        Config codenvyConfig = configManager.loadInstalledCodenvyConfig();
        Path tempDir = backupConfig.obtainArtifactTempDirectory();
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // re-create local temp dir
        commands.add(createCommand(format("rm -rf %s", tempDir)));
        commands.add(createCommand(format("mkdir -p %s", tempDir)));

        // stop services
        commands.add(createStopServiceCommand("puppet"));
        commands.add(createStopServiceCommand("crond"));
        commands.add(createStopServiceCommand("codenvy"));
        commands.add(createStopServiceCommand("slapd"));

        // dump LDAP user db into {backup_directory}/ldap/ldap.ldif file
        Path ldapUserBackupPath = getComponentTempPath(tempDir, LDAP);
        commands.add(createCommand(format("mkdir -p %s", ldapUserBackupPath.getParent())));
        commands.add(createCommand(format("sudo slapcat > %s", ldapUserBackupPath)));

        // dump LDAP_ADMIN db into {backup_directory}/ldap_admin/ldap.ldif file
        Path ldapAdminBackupPath = getComponentTempPath(tempDir, LDAP_ADMIN);
        commands.add(createCommand(format("mkdir -p %s", ldapAdminBackupPath.getParent())));
        commands.add(createCommand(format("sudo slapcat -b '%s' > %s",
                                          codenvyConfig.getValue(Config.ADMIN_LDAP_DN),
                                          ldapAdminBackupPath)));

        // dump MONGO data into {backup_directory}/mongo dir
        Path mongoBackupPath = getComponentTempPath(tempDir, MONGO);
        commands.add(createCommand(format("mkdir -p %s", mongoBackupPath)));
        commands.add(createCommand(format("/usr/bin/mongodump -u%s -p%s -o %s --authenticationDatabase admin --quiet",
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                          codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                          mongoBackupPath)));

        Path adminDatabaseBackup = mongoBackupPath.resolve("admin");
        commands.add(createCommand(format("rm -rf %s", adminDatabaseBackup)));  // remove useless 'admin' database

        // pack dumps into backup file
        commands.add(createPackCommand(tempDir, backupFile, ".", false));

        // pack filesystem data into the {backup_file}/fs folder
        commands.add(createPackCommand(Paths.get("/home/codenvy/codenvy-data"), backupFile, "fs/.", true));

        // start services
        commands.add(createStartServiceCommand("puppet"));

        // wait until API server starts
        commands.add(new WaitOnAliveArtifactCommand(original));

        // remove temp dir
        commands.add(createCommand(format("rm -rf %s", tempDir)));

        return new MacroCommand(commands, "Backup data commands");
    }

    /**
     * Given:
     * - path to backup file
     * - codenvy config
     *
     * Commands:
     * - create temp dir
     * - stop services
     * - restore LDAP user db from {temp_backup_directory}/ldap/ladp.ldif file
     * - restore LDAP_ADMIN db from {temp_backup_directory}/ldap_admin/ladp.ldif file
     * - restore mongo from {temp_backup_directory}/mongo folder
     * - restore filesystem data from {backup_file}/fs folder
     * - start services
     * - wait until API server restarts
     * - remove temp dir
     *
     * @return MacroCommand which holds all commands
     */
    @Override
    public Command getRestoreCommand(BackupConfig backupConfig) throws IOException {
        List<Command> commands = new ArrayList<>();
        Config codenvyConfig = configManager.loadInstalledCodenvyConfig();
        Path tempDir = backupConfig.obtainArtifactTempDirectory();
        Path backupFile = Paths.get(backupConfig.getBackupFile());

        // unpack backupFile into the tempDir
        TarUtils.unpackAllFiles(backupFile, tempDir);

        // stop services
        commands.add(createStopServiceCommand("puppet"));
        commands.add(createStopServiceCommand("crond"));
        commands.add(createStopServiceCommand("codenvy"));
        commands.add(createStopServiceCommand("slapd"));

        // restore LDAP user db from {temp_backup_directory}/ldap/ladp.ldif file
        Path ldapUserBackupPath = getComponentTempPath(tempDir, LDAP);
        if (Files.exists(ldapUserBackupPath)) {
            commands.add(createCommand("sudo rm -rf /var/lib/ldap"));
            commands.add(createCommand("sudo mkdir -p /var/lib/ldap"));
            commands.add(createCommand(format("sudo slapadd -q <%s", ldapUserBackupPath)));
            commands.add(createCommand("sudo chown -R ldap:ldap /var/lib/ldap"));
        }

        // restore LDAP_ADMIN db from {temp_backup_directory}/ldap_admin/ladp.ldif file
        Path ldapAdminBackupPath = getComponentTempPath(tempDir, LDAP_ADMIN);
        if (Files.exists(ldapAdminBackupPath)) {
            commands.add(createCommand("sudo rm -rf /var/lib/ldapcorp"));
            commands.add(createCommand("sudo mkdir -p /var/lib/ldapcorp"));
            commands.add(createCommand(format("sudo slapadd -q -b '%s' <%s",
                                              codenvyConfig.getValue(Config.ADMIN_LDAP_DN),
                                              ldapAdminBackupPath)));
            commands.add(createCommand("sudo chown -R ldap:ldap /var/lib/ldapcorp"));
        }

        // restore mongo from {temp_backup_directory}/mongo folder
        Path mongoBackupPath = getComponentTempPath(tempDir, MONGO);
        if (Files.exists(mongoBackupPath)) {
            // remove all databases expect 'admin' one
            commands.add(createCommand(format("/usr/bin/mongo -u %s -p %s --authenticationDatabase admin --quiet --eval " +
                                              "'db.getMongo().getDBNames().forEach(function(d){if (d!=\"admin\") db.getSiblingDB(d).dropDatabase()})'",
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY))));
            commands.add(createCommand(format("/usr/bin/mongorestore -u%s -p%s %s --authenticationDatabase admin --drop --quiet",
                                              // suppress stdout to avoid hanging up SecureSSH
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_USERNAME_PROPERTY),
                                              codenvyConfig.getValue(Config.MONGO_ADMIN_PASSWORD_PROPERTY),
                                              mongoBackupPath)));
        }
        // restore filesystem data from {backup_file}/fs folder
        Path fsBackupPath = getComponentTempPath(tempDir, FS);
        if (Files.exists(fsBackupPath)) {
            commands.add(createCommand("sudo rm -rf /home/codenvy/codenvy-data/fs"));
            commands.add(createCommand(format("sudo cp -r %s /home/codenvy/codenvy-data", fsBackupPath)));
            commands.add(createCommand("sudo chown -R codenvy:codenvy /home/codenvy/codenvy-data/fs"));
        }

        // start services
        commands.add(createStartServiceCommand("slapd"));
        commands.add(createStartServiceCommand("puppet"));

        // wait until API server restarts
        commands.add(new WaitOnAliveArtifactCommand(original));

        // remove temp dir
        commands.add(createCommand(format("rm -rf %s", tempDir)));

        return new MacroCommand(commands, "Restore data commands");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Command getUpdateConfigCommand(Config config, Map<String, String> properties) throws IOException {
        List<Command> commands = new ArrayList<>();

        // modify codenvy single server config
        String singleServerPropertiesFilePath = configManager.getPuppetConfigFile(Config.SINGLE_SERVER_PROPERTIES).toString();
        commands.add(createFileBackupCommand(singleServerPropertiesFilePath));
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            commands.add(createPropertyReplaceCommand(singleServerPropertiesFilePath, "$" + entry.getKey(), entry.getValue()));
        }

        String singleServerBasePropertiesFilePath = configManager.getPuppetConfigFile(Config.SINGLE_SERVER_BASE_PROPERTIES).toString();
        commands.add(createFileBackupCommand(singleServerBasePropertiesFilePath));
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            commands.add(createPropertyReplaceCommand(singleServerBasePropertiesFilePath, "$" + entry.getKey(), entry.getValue()));
        }

        // force applying updated puppet config on puppet agent of API node
        commands.add(createForcePuppetAgentCommand());

        // wait until API server restarts
        commands.add(new WaitOnAliveArtifactCommand(original));

        return new MacroCommand(commands, "Change config commands");
    }

    /**
     * - remove /home/codenvy/archives and /home/codenvy-im/archives
     * - stop Codenvy API server
     * - force applying puppet config for puppet agent
     * - wait until Codenvy API server be started
     */
    @Override
    public Command getReinstallCommand(Config config, @Nullable Version installedVersion) throws IOException {
        List<Command> commands = new ArrayList<>();

        // remove /home/codenvy/archives and /home/codenvy-im/archives
        commands.add(createCommand("sudo rm -rf /home/codenvy/archives"));
        commands.add(createCommand("sudo rm -rf /home/codenvy-im/archives"));

        // stop Codenvy API server
        commands.add(createStopServiceCommand("codenvy"));

        // force applying puppet config for puppet agent
        commands.add(createForcePuppetAgentCommand());

        // wait until API server starts
        commands.add(new PuppetErrorInterrupter(new WaitOnAliveArtifactCommand(original), configManager));

        return new MacroCommand(commands, "Re-install Codenvy binaries");
    }
}
