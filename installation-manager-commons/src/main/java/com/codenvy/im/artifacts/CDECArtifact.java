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

import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.SecureShell;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

/** @author Anatoliy Bazko */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String        updateEndpoint;

    @Inject
    public CDECArtifact(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                        HttpTransport transport) {
        super(NAME);
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
    }

    /**
     * CDEC installation sequence.
     * <p/>
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
     *
     * @param pathToBinaries
     * @throws IOException
     */
    @Override
    public void install(Path pathToBinaries) throws IOException {
        CDECInstallationConfiguration installationConfig = new CDECInstallationConfiguration();

        installPuppetMaster(installationConfig.getPuppetMaster(),
                            installationConfig.getHostsName(),
                            pathToBinaries);

        for (PuppetClientConfiguration clientConfig : installationConfig.getPuppetClients()) {
//            installPuppetClient(clientConfig, insta);  // TODO
        }

        signNodesOnPuppetMaster();

    }

    private void installPuppetMaster(PuppetMasterConfiguration config,
                                     List<String> hosts,
                                     Path pathToCdecArchive) throws IOException {
        SecureShell ssh = new SecureShell(config.getHost(),
                                          config.getSSHPort(),
                                          config.getUser(),
                                          config.getPassword()); //TODO or Authentication via  public key!!!

        // 1.1 Validate all hosts name and set their if need. //TODO Set ?
        validateHostsByDomainName(ssh, hosts);

        // 1.2 Add rule for firewall for puppet master.
        addFirewallRuleForPuppetMaster(ssh, config.getPuppetMasterPort());

        // 1.3 Install puppet master;
        installPuppetServer(ssh, config.getPuppetResourceUrl(), config.getPuppetServerVersion());

        // 1.4 Install unzip if need //TODO
        installUnzip(ssh);

        // 1.5 Upload CDEC in puppet master.
        String remoteBinariesPath = uploadCDEC(ssh, pathToCdecArchive);

        // 1.6 Unzip CDEC in puppet;
        unpackCdecInPuppet(ssh, remoteBinariesPath);

        // 1.7 Configure CDEC in puppet master;
        configureCdecOnPuppetmaster(ssh, config);

        // 1.8 Start puppet master
        startPuppetMaster(ssh);
    }

    // 2) On other hosts :
    // 1.4 Start puppet client;
    private void installPuppetClient(PuppetClientConfiguration config,
                                     List<String> instantsHosts,
                                     String puppetMasterHost) throws IOException {
        SecureShell ssh = new SecureShell(config.getHost(),
                                          config.getSSHPort(),
                                          config.getUser(),
                                          config.getPassword()); //TODO or Authentication via  public key!!!
        // 1.1 Validate all hosts name and set their if need; //TODO set ?
        validateHostsByDomainName(ssh, instantsHosts);

        // 1.2 Install puppet client;
        installPuppetAgent(ssh, config.getPuppetResourceUrl(), config.getPuppetClientVersion());

        // 1.3 Configure puppet client;
        configurePuppetAgent(ssh, puppetMasterHost);
    }

    private void configurePuppetAgent(SecureShell ssh, String puppetMasterHost) {


    }

    private void installPuppetAgent(SecureShell ssh, String puppetResourceUrl, String puppetClientVersion) throws IOException {
        // Disable SELinux //TODO Debian ???

        String result = ssh.execute("sudo setenforce 0");
        result = ssh.execute("sudo cp /etc/selinux/config /etc/selinux/config.bak");
        result = ssh.execute("sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config");

        //if (result) validate result!!!
        //TODO

        result = ssh.execute("sudo rpm -ivh " + puppetResourceUrl);
        //if (result) validate result!!!
        //TODO

        result = ssh.execute("sudo yum install " + puppetClientVersion + " -y");
        //if (result) validate result!!!
        //TODO

    }

    private void startPuppetMaster(SecureShell ssh) throws IOException {
        ssh.execute("sudo service puppetmaster start");
        //if (result) validate result!!!
        //TODO
    }

    private void configureCdecOnPuppetmaster(SecureShell ssh, PuppetMasterConfiguration config) {
        //TODO
    }

    private void unpackCdecInPuppet(SecureShell ssh, String remouteBinariesPath) throws IOException {
        ssh.execute("sudo unzip " + remouteBinariesPath + " /etc/puppet/");
        //if (result) validate result!!!
        //TODO
    }

    private String uploadCDEC(SecureShell ssh, Path pathToCdecArchive) {
        //TODO
        // http://www.jcraft.com/jsch/examples/ScpTo.java.html

        //if (result) validate result!!!
        //TODO
        return null;
    }

    private void installUnzip(SecureShell ssh) throws IOException {
        String result = ssh.execute("sudo yum install unzip");
        //if (result) validate result!!!
        //TODO
    }

    private void installPuppetServer(SecureShell ssh,
                                     String puppetResourceUrl,
                                     String puppetServerVersion) throws IOException {
        String result = ssh.execute("sudo rpm -ivh " + puppetResourceUrl);
        //if (result) validate result!!!
        //TODO

        result = ssh.execute("sudo yum install " + puppetServerVersion + " -y");
        //if (result) validate result!!!
        //TODO
    }

    private void validateHostsByDomainName(SecureShell ssh, List<String> hosts) {
        //TODO
    }

    private void addFirewallRuleForPuppetMaster(SecureShell ssh, int port) throws IOException {
        //TODO  check puppet-master-port


//        String result = ssh.execute("sudo iptables " + rule); //TODO or echo in /etc/sysconfig/iptables

        //if (result) validate result!!!
        //TODO
    }

    private void signNodesOnPuppetMaster() {
        //TODO
    }

    @Override
    public String getInstalledVersion(String accessToken) throws IOException {
        return "UNKNOWN";
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
}
