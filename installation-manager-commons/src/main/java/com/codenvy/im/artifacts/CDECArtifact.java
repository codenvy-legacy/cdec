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

import com.codenvy.api.core.rest.shared.dto.ApiInfo;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.command.SimpleCommand;
import com.codenvy.im.config.CodenvySingleServerConfig;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Commons;
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

import static com.codenvy.im.command.SimpleCommand.createLocalCommand;
import static com.codenvy.im.config.CodenvySingleServerConfig.Property.HOST_URL;
import static com.codenvy.im.config.CodenvySingleServerConfig.Property.PUPPET_AGENT_VERSION;
import static com.codenvy.im.config.CodenvySingleServerConfig.Property.PUPPET_RESOURCE_URL;
import static com.codenvy.im.config.CodenvySingleServerConfig.Property.PUPPET_SERVER_VERSION;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String apiNodeUrl;

    @Inject
    public CDECArtifact(@Named("cdec.api-node.url") String apiNodeUrl, HttpTransport transport) {
        super(NAME);
        this.transport = transport;
        this.apiNodeUrl = apiNodeUrl;
    }

    /** {@inheritDoc} */
    @Override
    public Version getInstalledVersion(String authToken) throws IOException {
        String response;
        try {
            response = transport.doOption(combinePaths(apiNodeUrl, "api/"), authToken);
        } catch (IOException e) {
            return null;
        }

        try {
            ApiInfo apiInfo = Commons.fromJson(response, ApiInfo.class);
            return Version.valueOf(apiInfo.getIdeVersion());
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return new ArrayList<String>() {{
            add("Disable SELinux");
            add("Install puppet server");
            add("Unzip CDEC binaries");
            add("Configure puppet master");
            add("Launch puppet master");
            add("Install puppet agent");
            add("Configure puppet agent");
            add("Launch puppet agent");
        }};
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(final Version version,
                                     final Path pathToBinaries,
                                     final InstallOptions installOptions) throws IOException {
        
        final CodenvySingleServerConfig config = new CodenvySingleServerConfig(installOptions.getConfigProperties());

        int step = installOptions.getStep();
        switch (step) {
            case 0:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("sudo setenforce 0"));
                    add(createLocalCommand("sudo cp /etc/selinux/config /etc/selinux/config.bak"));
                    add(createLocalCommand("sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config"));
                    add(createLocalCommand("sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config"));
                }}, "Disable SELinux");

            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; then " +
                                           format("sudo yum install %s -y", config.getValue(PUPPET_RESOURCE_URL)) + "; fi"));
                    add(createLocalCommand(format("sudo yum install %s -y", config.getValue(PUPPET_SERVER_VERSION))));
                }}, "Install puppet server");

            case 2:
                return new SimpleCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()),
                                         initLocalAgent(),
                                         "Unzip CDEC binaries");

            case 3:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand(
                            "if [ ! -f /etc/puppet/fileserver.conf.back ]; " +
                            "then sudo cp /etc/puppet/fileserver.conf /etc/puppet/fileserver.conf.back; " +
                            "else sudo cp /etc/puppet/fileserver.conf.back /etc/puppet/fileserver.conf; fi"));
                    add(createLocalCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                    add(createLocalCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                    add(createLocalCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));
//                    add(createLocalCommand(replaceCommand("aio_host_url", config.getDnsName())));
//                    add(createLocalCommand(replaceCommand("mongo_admin_pass", config.getMongoAdminPassword())));
//                    add(createLocalCommand(replaceCommand("mongo_user_pass", config.getMongoUserPassword())));
//                    add(createLocalCommand(replaceCommand("mongo_orgservice_user_pwd", config.getMongoOrgserviceUserPassword()
// )));
//                    add(createLocalCommand(replaceCommand("user_ldap_password", config.getUserLdapPassword())));
//                    add(createLocalCommand(replaceCommand("admin_ldap_user_name", config.getAdminLdapUserName())));
//                    add(createLocalCommand(replaceCommand("admin_ldap_password", config.getAdminLdapPassword())));
//                    add(createLocalCommand(replaceCommand("haproxy_statistic_pass", config.getHaproxyStatisticPassword())));
//                    add(createLocalCommand(replaceCommand("mysql_root_password", config.getMysqlRootPassword())));
//                    add(createLocalCommand(replaceCommand("zabbix_db_pass", config.getZabbixDatabasePassword())));
//                    add(createLocalCommand(replaceCommand("zabbix_admin_email", config.getZabbixAdminEmail())));
//                    add(createLocalCommand(replaceCommand("zabbix_admin_password", config.getZabbixAdminPassword())));
//                    add(createLocalCommand(replaceCommand("jmx_username", config.getJmxUserName())));
//                    add(createLocalCommand(replaceCommand("jmx_password", config.getJmxPassword())));
                }}, "Configure puppet master");

            case 4:
                // TODO
//                sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
//            sudo chkconfig ${SERVICE_NAME} on &>/dev/null
                return new SimpleCommand("sudo service puppetmaster start",
                                         initLocalAgent(),
                                         "Launch puppet master");

            case 5:
                return new SimpleCommand(format("sudo yum install %s -y", config.getValue(PUPPET_AGENT_VERSION)),
                                         initLocalAgent(),
                                         "Install puppet agent");
            case 6:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand(
                            "if [ ! -f /etc/puppet/puppet.conf.back ]; " +
                            "then sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back; " +
                            "else sudo cp /etc/puppet/puppet.conf.back /etc/puppet/puppet.conf; fi"));
                    add(createLocalCommand(format("sudo sed -i 's/[main]/[main]\n" +
                                                  "  server = %s\n" +
                                                  "  runinterval = 300\n" +
                                                  "  configtimeout = 600\n/g' /etc/puppet/puppet.conf", config.getValue(HOST_URL))));
                    add(createLocalCommand(format("sudo sed -i 's/[agent]/[agent]\n" +
                                                  "  show_diff = true\n" +
                                                  "  pluginsync = true\n" +
                                                  "  report = true\n" +
                                                  "  default_schedules = false\n" +
                                                  "  certname = %s\n/g' /etc/puppet/puppet.conf", config.getValue(HOST_URL))));
                }}, "Configure puppet agent");


            case 7:
                // TODO
//                sudo chkconfig --add ${SERVICE_NAME} &>/dev/null
//            sudo chkconfig ${SERVICE_NAME} on &>/dev/null

                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("sudo service puppet start"));
                    add(createLocalCommand("puppet cert --list --all"));
                    add(createLocalCommand(format("puppet cert --sign %s", config.getValue(HOST_URL))));
                }}, "Launch puppet agent");

            default:
                throw new IllegalArgumentException(format("Step number %d is out of range", step));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }

    private String replaceCommand(String property, String value) throws ConfigException {
        return format("sudo sed -i 's/%s = .*/%s = \"%s\"/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                      property,
                      property,
                      value);
    }
}
