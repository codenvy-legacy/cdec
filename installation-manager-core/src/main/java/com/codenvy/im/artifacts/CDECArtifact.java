/*
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
package com.codenvy.im.artifacts;

import com.codenvy.api.core.rest.shared.dto.ApiInfo;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.command.CheckInstalledVersionCommand;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.command.StoreIMConfigPropertyCommand;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.config.NodeConfig;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.service.InstallationManagerConfig;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.OSUtils;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import static com.codenvy.im.command.SimpleCommand.createLocalCommand;
import static com.codenvy.im.config.NodeConfig.extractConfigFrom;
import static com.codenvy.im.config.NodeConfig.extractConfigsFrom;
import static com.codenvy.im.utils.Commons.getVersionsList;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;

    @Inject
    public CDECArtifact(HttpTransport transport) {
        super(NAME);
        this.transport = transport;
    }

    /** {@inheritDoc} */
    @Override
    public Version getInstalledVersion() throws IOException {
        String response;
        try {
            String codenvyHostDns = InstallationManagerConfig.readCdecHostDns();
            if (codenvyHostDns == null) {
                return null;
            }

            String checkServiceUrl = format("http://%s/api/", codenvyHostDns);
            response = transport.doOption(checkServiceUrl, null);
        } catch (IOException e) {
            return null;
        }

        ApiInfo apiInfo = Commons.createDtoFromJson(response, ApiInfo.class);
        if (apiInfo.getIdeVersion() == null && apiInfo.getImplementationVersion().equals("0.26.0")) {
            return Version.valueOf("3.1.0"); // Old ide doesn't contain Ide Version property
        } else {
            return Version.valueOf(apiInfo.getIdeVersion());
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUpdateInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of("Unzip Codenvy binaries to /tmp/cdec",
                                "Configure Codenvy",
                                "Patch resources",
                                "Move Codenvy binaries to /etc/puppet",
                                "Update Codenvy");
    }

    /** {@inheritDoc} */
    @Override
    public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return createLocalCommand(format("rm -rf /tmp/cdec; " +
                                                 "mkdir /tmp/cdec/; " +
                                                 "unzip -o %s -d /tmp/cdec", pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                commands.add(createLocalCommand(format("sed -i 's/%s/%s/g' /tmp/cdec/manifests/nodes/single_server/single_server.pp",
                                                       "YOUR_DNS_NAME", config.getHostUrl())));
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalPropertyReplaceCommand("/tmp/cdec/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalPropertyReplaceCommand("/tmp/cdec/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }
                return new MacroCommand(commands, "Configure Codenvy");

            case 2:
                return getPatchCommand(Paths.get("/tmp/cdec/patches/"), versionToUpdate);

            case 3:
                return createLocalCommand("sudo rm -rf /etc/puppet/files; " +
                                          "sudo rm -rf /etc/puppet/modules; " +
                                          "sudo rm -rf /etc/puppet/manifests; " +
                                          "sudo mv /tmp/cdec/* /etc/puppet");

            case 4:
                return new CheckInstalledVersionCommand(this, versionToUpdate);

            default:
                throw new IllegalArgumentException(format("Step number %d is out of update range", step));
        }
    }

    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        switch (installOptions.getInstallType()) {
            case CODENVY_MULTI_SERVER:
                return getInstallInfoForMultiServer();

            case CODENVY_SINGLE_SERVER:
            default:
                return getInstallInfoForSingleServer();
        }
    }

    private List<String> getInstallInfoForSingleServer() {
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
    public Command getInstallCommand(final Version versionToInstall,
                                     final Path pathToBinaries,
                                     final InstallOptions installOptions) throws IOException {

        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (installOptions.getInstallType()) {
            case CODENVY_MULTI_SERVER:
                return getInstallCommandsForMultiServer(versionToInstall, pathToBinaries, config, step);

            case CODENVY_SINGLE_SERVER:
            default:
                return getInstallCommandsForSingleServer(versionToInstall, pathToBinaries, config, step);
        }
    }

    private Command getInstallCommandsForSingleServer(Version versionToInstall, Path pathToBinaries, final Config config, int step) {
        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    createLocalRestoreOrBackupCommand("/etc/selinux/config"),
                    createLocalCommand("if sudo test -f /etc/selinux/config; then " +
                                       "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                       "        sudo setenforce 0; " +
                                       "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                       "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                       "    fi " +
                                       "fi ")),
                    "Disable SELinux");

            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand(
                            "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                            + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                            + "; fi"));
                    add(createLocalCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_SERVER_VERSION))));
                    add(createLocalCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_AGENT_VERSION))));

                    if (OSUtils.getVersion().equals("6")) {
                        add(createLocalCommand("sudo chkconfig --add puppetmaster"));
                        add(createLocalCommand("sudo chkconfig puppetmaster on"));
                        add(createLocalCommand("sudo chkconfig --add puppet"));
                        add(createLocalCommand("sudo chkconfig puppet on"));
                    } else {
                        add(createLocalCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                               " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user.target" +
                                               ".wants/puppetmaster.service'" +
                                               "; fi"));
                        add(createLocalCommand("sudo systemctl enable puppetmaster"));
                        add(createLocalCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                               " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                               ".wants/puppet.service'" +
                                               "; fi"));
                        add(createLocalCommand("sudo systemctl enable puppet"));
                    }

                }}, "Install puppet binaries");

            case 2:
                return createLocalCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3:
                List<Command> commands = new ArrayList<>();

                commands.add(createLocalRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));


                commands.add(createLocalCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                                       "YOUR_DNS_NAME", config.getHostUrl())));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalPropertyReplaceCommand("/etc/puppet/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                return new MacroCommand(commands, "Configure puppet master");

            case 4:
                return new MacroCommand(ImmutableList.of(
                        createLocalRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
                        createLocalCommand("sudo sed -i '1i[master]' /etc/puppet/puppet.conf"),
                        createLocalCommand(format("sudo sed -i '2i  certname = %s' /etc/puppet/puppet.conf", config.getHostUrl())),
                        createLocalCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                  "  dns_alt_names = puppet,%s\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())),
                        createLocalCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
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
                    return createLocalCommand("sudo service puppetmaster start");
                } else {
                    return createLocalCommand("sudo systemctl start puppetmaster");
                }

            case 6:
                if (OSUtils.getVersion().equals("6")) {
                    return new MacroCommand(ImmutableList.<Command>of(
                            createLocalCommand("sleep 30"),
                            createLocalCommand("sudo service puppet start")),
                                            "Launch puppet agent");
                } else {
                    return new MacroCommand(ImmutableList.<Command>of(
                            createLocalCommand("sleep 30"),
                            createLocalCommand("sudo systemctl start puppet")),
                                            "Launch puppet agent");
                }

            case 7:
                return createLocalCommand("doneState=\"Installing\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                           "    sleep 30; " +
                                           "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                           "done");

            case 8:
                return new MacroCommand(ImmutableList.of(
                        StoreIMConfigPropertyCommand.createSaveCodenvyHostDnsCommand("localhost"),
                        new CheckInstalledVersionCommand(this, versionToInstall)
                    ),
                    "Check if CDEC has already installed");


            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }
    }

    private List<String> getInstallInfoForMultiServer() {
        return ImmutableList.of(
            "Disable SELinux on nodes",
            "Install puppet binaries",
            "Unzip Codenvy binaries",
            "Configure puppet master",
            "Configure puppet agents on each nodes",
            "Launch puppet master",
            "Launch puppet agents",
            "Install Codenvy (~10 min)",
            "Boot Codenvy (~10 min)"
        );
    }

    /**
     * @throws AgentException if there was a problem with LocalAgent or SecureShellAgent instantiation.
     * @throws IllegalArgumentException if there is no Site node config.
     * @throws IllegalStateException if local OS version != 7
     */
    private Command getInstallCommandsForMultiServer(Version versionToInstall, Path pathToBinaries, final Config config, int step) throws AgentException,
                                                                                                                                          IllegalArgumentException,
                                                                                                                                          IllegalStateException {
        if (!OSUtils.getVersion().equals("7")) {
            throw new IllegalStateException("Only installation of multi-node version of CDEC on Centos 7 is supported");
        }

        final List<NodeConfig> nodeConfigs = extractConfigsFrom(config);
        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    // disable selinux on puppet agent
                    createShellRestoreOrBackupCommand("/etc/selinux/config", nodeConfigs),
                    createShellCommand("if sudo test -f /etc/selinux/config; then " +
                                       "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                       "        sudo setenforce 0; " +
                                       "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                       "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                       "    fi " +
                                       "fi ", nodeConfigs),

                    // disable selinux on puppet master
                    createLocalRestoreOrBackupCommand("/etc/selinux/config"),
                    createLocalCommand("if sudo test -f /etc/selinux/config; then " +
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
                    add(createLocalCommand(
                        "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                        + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                        + "; fi"));
                    add(createLocalCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_SERVER_VERSION))));

                    add(createLocalCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                           " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user.target" +
                                           ".wants/puppetmaster.service'" +
                                           "; fi"));
                    add(createLocalCommand("sudo systemctl enable puppetmaster"));

                    // install puppet agents on each node
                    add(createShellCommand(
                        "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                        + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                        + "; fi", nodeConfigs));
                    add(createShellCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_AGENT_VERSION)), nodeConfigs));

                    add(createShellCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                           " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                           ".wants/puppet.service'" +
                                           "; fi", nodeConfigs));
                    add(createShellCommand("sudo systemctl enable puppet", nodeConfigs));

                    // disable firewalld to open connection with puppet agents
                    add(createLocalCommand("sudo service firewalld stop"));
                    add(createLocalCommand("sudo systemctl disable firewalld"));

                    // install iptables and open port 8140 for puppet  TODO [ndp] isn't mandatory
                    /*
                        sudo yum install iptables-services
                        // add next row into /etc/sysconfig/iptables: '-A INPUT -p tcp -m state --state NEW -m tcp --dport 8140 -j ACCEPT'
                        sudoservice iptables
                    */
                }}, "Install puppet binaries");

            case 2:
                return createLocalCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3: {
                List<Command> commands = new ArrayList<>();

                commands.add(createLocalRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createLocalCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));

                commands.add(createLocalCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/%s",
                                                       "YOUR_DNS_NAME",
                                                       config.getHostUrl(),
                                                       Config.MULTI_SERVER_PROPERTIES)));

                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalPropertyReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                for (Map.Entry<String, String> e : ConfigUtil.getPuppetNodesConfigReplacement(nodeConfigs)) {
                    String replacingToken = e.getKey();
                    String replacement = e.getValue();

                    commands.add(createLocalReplaceCommand("/etc/puppet/" + Config.MULTI_SERVER_NODES_PROPERTIES, replacingToken, replacement));
                }

                commands.add(createLocalRestoreOrBackupCommand("/etc/puppet/puppet.conf"));
                commands.add(createLocalReplaceCommand("/etc/puppet/puppet.conf",
                                                       "\\[main\\]",
                                                       format("\\[master\\]\\n" +
                                                              "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                              "    ca = true\\n" +
                                                              "    ssldir = /var/lib/puppet/ssl\\n" +
                                                              "\\n"+
                                                              "\\[main\\]\\n" +
                                                              "    certname = %s\\n" +
                                                              "    privatekeydir = $ssldir/private_keys { group = service }\\n" +
                                                              "    hostprivkey = $privatekeydir/$certname.pem { mode = 640 }\\n" +
                                                              "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                              "\\n",
                                                              config.getProperty(Config.PUPPET_MASTER_HOST_NAME_PROPERTY))));

                // remove "[agent]" section
                commands.add(createLocalReplaceCommand("/etc/puppet/puppet.conf",
                                                       "\\[agent\\].*",
                                                       ""));

                // make it possible to sign up nodes' certificates automatically
                commands.add(createLocalCommand("sudo sh -c \"echo '*' > /etc/puppet/autosign.conf\""));

                return new MacroCommand(commands, "Configure puppet master");
            }

            case 4: {
                List<Command> commands = new ArrayList<>();
                commands.add(createShellRestoreOrBackupCommand("/etc/puppet/puppet.conf", nodeConfigs));
                commands.add(createShellCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                              "  server = %s\\n" +
                                              "  runinterval = 420\\n" +
                                              "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                                       config.getProperty(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
                                                nodeConfigs));

                for (NodeConfig node : nodeConfigs) {
                    commands.add(createShellCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                  "  show_diff = true\\n" +
                                                  "  pluginsync = true\\n" +
                                                  "  report = true\\n" +
                                                  "  default_schedules = false\\n" +
                                                  "  certname = %s\\n/g' /etc/puppet/puppet.conf", node.getHost()), node));
                }

                return new MacroCommand(commands, "Configure puppet agent");
            }

            case 5:
                return createLocalCommand("sudo systemctl start puppetmaster");

            case 6:
                return new MacroCommand(ImmutableList.of(createShellCommand("sudo systemctl start puppet", nodeConfigs)),
                                        "Launch puppet agent");

            case 7:
                NodeConfig siteNodeConfig = extractConfigFrom(config, NodeConfig.NodeType.SITE);
                if (siteNodeConfig == null) {
                    throw new IllegalArgumentException("Site node config not found.");
                }

                return createShellCommand("doneState=\"Installing\"; " +
                                          "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                          "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                          "    sleep 30; " +
                                          "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                          "done", extractConfigFrom(config, NodeConfig.NodeType.SITE));

            case 8:
                return new MacroCommand(ImmutableList.of(
                    StoreIMConfigPropertyCommand.createSaveCodenvyHostDnsCommand(config.getHostUrl()),
                    new CheckInstalledVersionCommand(this, versionToInstall)
                ),
                "Check if CDEC has already installed");

            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }
    }

    protected Command getPatchCommand(Path patchDir, Version versionToUpdate) throws IOException {
        List<Command> commands;
        commands = new ArrayList<>();

        NavigableSet<Version> versions = getVersionsList(patchDir).subSet(getInstalledVersion(), false, versionToUpdate, true);
        Iterator<Version> iter = versions.iterator();
        while (iter.hasNext()) {
            Version v = iter.next();
            Path patchFile = patchDir.resolve(v.toString()).resolve("patch.sh");
            if (exists(patchFile)) {
                commands.add(createLocalCommand(format("sudo bash %s", patchFile)));
            }
        }

        return new MacroCommand(commands, "Patch resources");
    }

    protected Command createLocalPropertyReplaceCommand(String file, String property, String value) {
        String replacingToken = format("%s = .*", property);
        String replacement = format("%s = \"%s\"", property, value);
        return createLocalReplaceCommand(file, replacingToken, replacement);
    }

    protected Command createLocalReplaceCommand(String file, String replacingToken, String replacement) {
        return createLocalCommand(format("sudo sed -i 's/%s/%s/g' %s",
                                         replacingToken.replace("/", "\\/"),
                                         replacement.replace("/", "\\/"),
                                         file));
    }

    protected Command createLocalRestoreOrBackupCommand(final String file) {
        return createLocalCommand(getRestoreOrBackupCommand(file));
    }

    protected Command createShellRestoreOrBackupCommand(final String file, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createShellCommandsForEachNode(getRestoreOrBackupCommand(file), null, nodes);
    }

    protected String getRestoreOrBackupCommand(final String file) {
        final String backupFile = file + ".back";
        return format("if sudo test -f %2$s; then " +
                   "    if ! sudo test -f %1$s; then " +
                   "        sudo cp %2$s %1$s; " +
                   "    else " +
                   "        sudo cp %1$s %2$s; " +
                   "    fi " +
                   "fi",
                   backupFile,
                   file);
    }

    protected Command createShellCommand(String command, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createShellCommandsForEachNode(command, null, nodes);
    }

    protected Command createShellCommand(String command, NodeConfig node) throws AgentException {
        return SimpleCommand.createShellCommandForNode(command, node);
    }
}
