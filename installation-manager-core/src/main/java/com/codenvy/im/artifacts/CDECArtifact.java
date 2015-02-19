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
import com.codenvy.im.command.StoreIMConfigPropertyCommand;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.node.NodeConfig;
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

import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentRestoreOrBackupCommand;
import static com.codenvy.im.command.SimpleCommand.createLocalAgentCommand;
import static com.codenvy.im.node.NodeConfig.extractConfigFrom;
import static com.codenvy.im.node.NodeConfig.extractConfigsFrom;
import static com.codenvy.im.utils.Commons.getVersionsList;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "codenvy";

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
        return ImmutableList.of("Unzip Codenvy binaries to /tmp/codenvy",
                                "Configure Codenvy",
                                "Patch resources",
                                "Move Codenvy binaries to /etc/puppet",
                                "Update Codenvy");
    }

    /**
     * {@inheritDoc}
     * @throws IOException
     * @throws IllegalArgumentException if step number is out of range, or if installation type != CODENVY_SINGLE_SERVER
     */
    @Override
    public Command getUpdateCommand(Version versionToUpdate, Path pathToBinaries, InstallOptions installOptions) throws IOException, IllegalArgumentException {
        if (installOptions.getInstallType() != InstallOptions.InstallType.CODENVY_SINGLE_SERVER) {
            throw new IllegalArgumentException("Only update to single-server version is supported");
        }

        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return createLocalAgentCommand(format("rm -rf /tmp/codenvy; " +
                                                      "mkdir /tmp/codenvy/; " +
                                                      "unzip -o %s -d /tmp/codenvy", pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                commands.add(createLocalAgentCommand(format("sed -i 's/%s/%s/g' /tmp/codenvy/manifests/nodes/single_server/single_server.pp",
                                                            "YOUR_DNS_NAME", config.getHostUrl())));
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalAgentPropertyReplaceCommand("/tmp/codenvy/" + Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }
                return new MacroCommand(commands, "Configure Codenvy");

            case 2:
                return getPatchCommand(Paths.get("/tmp/codenvy/patches/"), versionToUpdate);

            case 3:
                return createLocalAgentCommand("sudo rm -rf /etc/puppet/files; " +
                                               "sudo rm -rf /etc/puppet/modules; " +
                                               "sudo rm -rf /etc/puppet/manifests; " +
                                               "sudo mv /tmp/codenvy/* /etc/puppet");

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
                return getInstallInfoMultiServer();

            case CODENVY_SINGLE_SERVER:
            default:
                return getInstallInfoSingleServer();
        }
    }

    private List<String> getInstallInfoSingleServer() {
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
                return getInstallCommandsMultiServer(versionToInstall, pathToBinaries, config, step);

            case CODENVY_SINGLE_SERVER:
            default:
                return getInstallCommandsSingleServer(versionToInstall, pathToBinaries, config, step);
        }
    }

    private Command getInstallCommandsSingleServer(Version versionToInstall, Path pathToBinaries, final Config config, int step) {
        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                        createLocalAgentRestoreOrBackupCommand("/etc/selinux/config"),
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
                            + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                            + "; fi"));
                    add(createLocalAgentCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_SERVER_VERSION))));
                    add(createLocalAgentCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_AGENT_VERSION))));

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

                commands.add(createLocalAgentRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
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
                        createLocalAgentRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
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
                        new CheckInstalledVersionCommand(this, versionToInstall)
                    ),
                    "Check if Codenvy has already installed");


            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }
    }

    private List<String> getInstallInfoMultiServer() {
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
     * @throws AgentException if there was a problem with LocalAgent or SecureShellAgent instantiation.
     * @throws IllegalArgumentException if there is no Site node config.
     * @throws IllegalStateException if local OS version != 7
     */
    private Command getInstallCommandsMultiServer(Version versionToInstall, Path pathToBinaries, final Config config, int step) throws AgentException,
                                                                                                                                          IllegalArgumentException,
                                                                                                                                          IllegalStateException {
        if (!OSUtils.getVersion().equals("7")) {
            throw new IllegalStateException("Multi-serve installation is supported only on CentOS 7");
        }

        final List<NodeConfig> nodeConfigs = extractConfigsFrom(config);
        switch (step) {
            case 0:
                return new MacroCommand(ImmutableList.of(
                    // disable selinux on puppet agent
                    createShellAgentRestoreOrBackupCommand("/etc/selinux/config", nodeConfigs),
                    createShellAgentCommand("if sudo test -f /etc/selinux/config; then " +
                                            "    if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                            "        sudo setenforce 0; " +
                                            "        sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                            "        sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                            "    fi " +
                                            "fi ", nodeConfigs),

                    // disable selinux on puppet master
                    createLocalAgentRestoreOrBackupCommand("/etc/selinux/config"),
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
                            "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                            + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                            + "; fi"));
                    add(createLocalAgentCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_SERVER_VERSION))));

                    add(createLocalAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppetmaster.service ]; then" +
                                                " sudo ln -s '/usr/lib/systemd/system/puppetmaster.service' '/etc/systemd/system/multi-user.target" +
                                                ".wants/puppetmaster.service'" +
                                                "; fi"));
                    add(createLocalAgentCommand("sudo systemctl enable puppetmaster"));

                    // install puppet agents on each node
                    add(createShellAgentCommand(
                            "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                            + format("then sudo yum install %s -y", config.getProperty(Config.PUPPET_RESOURCE_URL))
                            + "; fi", nodeConfigs));
                    add(createShellAgentCommand(format("sudo yum install %s -y", config.getProperty(Config.PUPPET_AGENT_VERSION)), nodeConfigs));

                    add(createShellAgentCommand("if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then" +
                                                " sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target" +
                                                ".wants/puppet.service'" +
                                                "; fi", nodeConfigs));
                    add(createShellAgentCommand("sudo systemctl enable puppet", nodeConfigs));

                    // disable firewalld to open connection with puppet agents
                    add(createLocalAgentCommand("sudo service firewalld stop"));
                    add(createLocalAgentCommand("sudo systemctl disable firewalld"));

                    // install iptables and open port 8140 for puppet  TODO [ndp] isn't mandatory
                    /*
                        sudo yum install iptables-services
                        // add next row into /etc/sysconfig/iptables: '-A INPUT -p tcp -m state --state NEW -m tcp --dport 8140 -j ACCEPT'
                        sudoservice iptables
                    */
                }}, "Install puppet binaries");

            case 2:
                return createLocalAgentCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3: {
                List<Command> commands = new ArrayList<>();

                commands.add(createLocalAgentRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                commands.add(createLocalAgentCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));

                commands.add(createLocalAgentCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/%s",
                                                            "YOUR_DNS_NAME",
                                                            config.getHostUrl(),
                                                            Config.MULTI_SERVER_PROPERTIES)));

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

                commands.add(createLocalAgentRestoreOrBackupCommand("/etc/puppet/puppet.conf"));
                commands.add(createLocalAgentReplaceCommand("/etc/puppet/puppet.conf",
                                                            "\\[main\\]",
                                                            format("\\[master\\]\\n" +
                                                                   "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                                   "    ca = true\\n" +
                                                                   "    ssldir = /var/lib/puppet/ssl\\n" +
                                                                   "\\n" +
                                                                   "\\[main\\]\\n" +
                                                                   "    certname = %s\\n" +
                                                                   "    privatekeydir = $ssldir/private_keys { group = service }\\n" +
                                                                   "    hostprivkey = $privatekeydir/$certname.pem { mode = 640 }\\n" +
                                                                   "    autosign = $confdir/autosign.conf { mode = 664 }\\n" +
                                                                   "\\n",
                                                                   config.getProperty(Config.PUPPET_MASTER_HOST_NAME_PROPERTY))));

                // remove "[agent]" section
                commands.add(createLocalAgentReplaceCommand("/etc/puppet/puppet.conf",
                                                            "\\[agent\\].*",
                                                            ""));

                // make it possible to sign up nodes' certificates automatically
                commands.add(createLocalAgentCommand("sudo sh -c \"echo '*' > /etc/puppet/autosign.conf\""));

                return new MacroCommand(commands, "Configure puppet master");
            }

            case 4: {
                List<Command> commands = new ArrayList<>();
                commands.add(createShellAgentRestoreOrBackupCommand("/etc/puppet/puppet.conf", nodeConfigs));
                commands.add(createShellAgentCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                            "  server = %s\\n" +
                                                            "  runinterval = 420\\n" +
                                                            "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf",
                                                            config.getProperty(Config.PUPPET_MASTER_HOST_NAME_PROPERTY)),
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
                    new CheckInstalledVersionCommand(this, versionToInstall)
                ),
                "Check if Codenvy has already installed");

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
                commands.add(createLocalAgentCommand(format("sudo bash %s", patchFile)));
            }
        }

        return new MacroCommand(commands, "Patch resources");
    }
}
