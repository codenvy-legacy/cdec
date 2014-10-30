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
package com.codenvy.im.artifacts;

import com.codenvy.im.agent.Agent;
import com.codenvy.im.agent.SecureShellAgent;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.command.RemoteCommand;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 * */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String        updateEndpoint;

    public enum InstallType {
        SINGLE_NODE_WITH_PUPPET_MASTER,
        SINGLE_NODE_WITHOUT_PUPPET_MASTER,
        MULTI_NODE_WITH_PUPPET_MASTER
    }

    @Inject
    public CDECArtifact(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                        HttpTransport transport) {
        super(NAME);
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
    }

    @Override
    public void install(Path pathToBinaries) throws CommandException {
        List<Command> installCommands = getInstallCommands(InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER);

        for (Command command : installCommands) {
            command.execute();
        }
    }

//    private void installPuppetMaster(PuppetMasterConfig config,
//                                     List<String> hosts,
//                                     Path pathToCdecArchive) throws IOException {
//        SecureShellAgent ssh = new SecureShellAgent(config.getHost(),
//                                          config.getSSHPort(),
//                                          config.getUser(),
//                                          config.getPassword()); //TODO or Authentication via  public key!!!
//
//        // 1.1 Validate all hosts name and set their if need. //TODO Set ?
//        validateHostsByDomainName(ssh, hosts);
//
//        // 1.2 Add rule for firewall for puppet master.
//        addFirewallRuleForPuppetMaster(ssh, config.getPuppetMasterPort());
//
//        // 1.3 Install puppet master;
//        installPuppetServer(ssh, config.getPuppetResourceUrl(), config.getPuppetServerVersion());
//
//        // 1.4 Install unzip if need //TODO
//        installUnzip(ssh);
//
//        // 1.5 Upload CDEC in puppet master.
//        String remoteBinariesPath = uploadCDEC(ssh, pathToCdecArchive);
//
//        // 1.6 Unzip CDEC in puppet;
//        unpackCdecInPuppet(ssh, remoteBinariesPath);
//
//        // 1.7 Configure CDEC in puppet master;
//        configureCdecOnPuppetmaster(ssh, config);
//
//        // 1.8 Start puppet master
//        startPuppetMaster(ssh);
//    }
//
//    // 2) On other hosts :
//    // 1.4 Start puppet client;
//    private void installPuppetClient(PuppetClientConfig config,
//                                     List<String> instantsHosts,
//                                     String puppetMasterHost) throws IOException {
//        SecureShellAgent ssh = new SecureShellAgent(config.getHost(),
//                                          config.getSSHPort(),
//                                          config.getUser(),
//                                          config.getPassword()); //TODO or Authentication via  public key!!!
//        // 1.1 Validate all hosts name and set their if need; //TODO set ?
//        validateHostsByDomainName(ssh, instantsHosts);
//
//        // 1.2 Install puppet client;
//        installPuppetAgent(ssh, config.getPuppetResourceUrl(), config.getPuppetVersion());
//
//        // 1.3 Configure puppet client;
//        configurePuppetAgent(ssh, puppetMasterHost);
//    }
//
//    private void configurePuppetAgent(SecureShellAgent ssh, String puppetMasterHost) {
//
//
//    }
//
//    private void installPuppetAgent(SecureShellAgent ssh, String puppetResourceUrl, String puppetClientVersion) throws IOException {
//        // Disable SELinux //TODO Debian ???
//
//        String result = ssh.execute("sudo setenforce 0");
//        result = ssh.execute("sudo cp /etc/selinux/config /etc/selinux/config.bak");
//        result = ssh.execute("sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config");
//
//        //if (result) validate result!!!
//        //TODO
//
//        result = ssh.execute("sudo rpm -ivh " + puppetResourceUrl);
//        //if (result) validate result!!!
//        //TODO
//
//        result = ssh.execute("sudo yum install " + puppetClientVersion + " -y");
//        //if (result) validate result!!!
//        //TODO
//
//    }
//
//    private void startPuppetMaster(SecureShellAgent ssh) throws IOException {
//        ssh.execute("sudo service puppetmaster start");
//        //if (result) validate result!!!
//        //TODO
//    }
//
//    private void configureCdecOnPuppetmaster(SecureShellAgent ssh, PuppetMasterConfig config) {
//        //TODO
//    }
//
//    private void unpackCdecInPuppet(SecureShellAgent ssh, String remouteBinariesPath) throws IOException {
//        ssh.execute("sudo unzip " + remouteBinariesPath + " /etc/puppet/");
//        //if (result) validate result!!!
//        //TODO
//    }
//
//    private String uploadCDEC(SecureShellAgent ssh, Path pathToCdecArchive) {
//        //TODO
//        // http://www.jcraft.com/jsch/examples/ScpTo.java.html
//
//        //if (result) validate result!!!
//        //TODO
//        return null;
//    }
//
//    private void installUnzip(SecureShellAgent ssh) throws IOException {
//        String result = ssh.execute("sudo yum install unzip");
//        //if (result) validate result!!!
//        //TODO
//    }
//
//    private void installPuppetServer(SecureShellAgent ssh,
//                                     String puppetResourceUrl,
//                                     String puppetServerVersion) throws IOException {
//        String result = ssh.execute("sudo rpm -ivh " + puppetResourceUrl);
//        //if (result) validate result!!!
//        //TODO
//
//        result = ssh.execute("sudo yum install " + puppetServerVersion + " -y");
//        //if (result) validate result!!!
//        //TODO
//    }
//
//    private void validateHostsByDomainName(SecureShellAgent ssh, List<String> hosts) {
//        //TODO
//    }
//
//    private void addFirewallRuleForPuppetMaster(SecureShellAgent ssh, int port) throws IOException {
//        //TODO  check puppet-master-port
//
//
////        String result = ssh.execute("sudo iptables " + rule); //TODO or echo in /etc/sysconfig/iptables
//
//        //if (result) validate result!!!
//        //TODO
//    }
//
//    private void signNodesOnPuppetMaster() {
//        //TODO
//    }

    @Override
    public String getInstalledVersion(String accessToken) throws IOException {
        return "UNKNOWN";  // TODO
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public boolean isInstallable(Version versionToInstall, String accessToken) {
        return false; // temporarily
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }

    /**
     * CDEC installation sequence.
     * 1) On Puppet Master host :
     * 1.1 Validate all hosts name and set their if need. //TODO set ?
     * 1.2 Add rule in firewall for puppet master.
     * 1.3 Install puppet master;
     * 1.4 Install unzip if need //TODO
     * 1.5 Upload CDEC in puppet master.
     * 1.6 Unzip CDEC in puppet;
     * 1.7 Configure CDEC in puppet master;
     * 1.8 Start puppet master
     * 2) On other hosts :
     * 1.1 Validate all hosts name and set their if need; //TODO set ?
     * 1.2 Install puppet client;
     * 1.3 Configure puppet client;
     * 1.4 Start puppet client;
     * 3) Sign nodes connection request on puppet master;
     * 1.1 Validate nodes requests available;
     * 1.2 Sign nodes connection request.
     */
    private List<Command> getInstallCommands(InstallType type) {
        switch (type) {
            case SINGLE_NODE_WITHOUT_PUPPET_MASTER:
                return getInstallCdecOnSingleNodeWithoutPuppetMasterCommands();

            case SINGLE_NODE_WITH_PUPPET_MASTER:
            case MULTI_NODE_WITH_PUPPET_MASTER:
//                CdecConfig installationConfig = new CdecConfig();
//
//                installPuppetMaster(installationConfig.getPuppetMaster(),
//                                    installationConfig.getHostsName(),
//                                    pathToBinaries);
//
//                for (PuppetClientConfig clientConfig : installationConfig.getPuppetClients()) {
//                    //            installPuppetClient(clientConfig, insta);  // TODO
//                }
//
//                signNodesOnPuppetMaster();
            default:
                return new ArrayList<>();
        }
    }

    private List<Command> getInstallCdecOnSingleNodeWithoutPuppetMasterCommands() {
        final CdecConfig config = ConfigFactory.loadConfig(InstallType.SINGLE_NODE_WITHOUT_PUPPET_MASTER);

        List<Command> commands = new ArrayList<>();

        final Agent agent = new SecureShellAgent(
            config.getHost(),
            Integer.valueOf(config.getSSHPort()),
            config.getUser(),
            config.getPrivateKeyFileAbsolutePath(),
            null
        );

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
}
