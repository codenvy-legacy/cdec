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
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.im.command.SimpleCommand.createLocalCommand;
import static com.codenvy.im.config.CodenvySingleServerConfig.Property.*;
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
        } catch (ConnectException e) {
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
            add("Boot Codenvy");
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
                    add(createLocalRestoreOrBackupCommand("/etc/selinux/config"));
                    add(createLocalCommand("if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                           "    sudo setenforce 0; " +
                                           "    sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                           "    sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                           "fi"));
                }}, "Disable SELinux");

            case 1:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand(
                            "if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; "
                            + format("then sudo yum install %s -y", config.getValue(PUPPET_RESOURCE_URL))
                            + "; fi"));
                    add(createLocalCommand(
                            format("sudo yum install %s -y", config.getValue(PUPPET_SERVER_VERSION))));
                }}, "Install puppet server");

            case 2:
                return createLocalCommand(format("sudo unzip -o %s -d /etc/puppet", pathToBinaries.toString()));

            case 3:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalRestoreOrBackupCommand("/etc/puppet/fileserver.conf"));
                    add(createLocalCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"));
                    add(createLocalCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"));
                    add(createLocalCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"));
                    add(createLocalCommand(
                            format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                   "YOUR_DNS_NAME", config.getValue(AIO_HOST_URL))));
                    add(createLocalReplaceCommand("$host_url", config.getValue(AIO_HOST_URL)));
                    add(createLocalReplaceCommand("$builder_max_execution_time", config.getValue(BUILDER_MAX_EXECUTION_TIME)));
                    add(createLocalReplaceCommand("$builder_waiting_time", config.getValue(BUILDER_WAITING_TIME)));
                    add(createLocalReplaceCommand("$builder_keep_result_time", config.getValue(BUILDER_KEEP_RESULT_TIME)));
                    add(createLocalReplaceCommand("$builder_queue_size", config.getValue(BUILDER_QUEUE_SIZE)));
                    add(createLocalReplaceCommand("$runner_default_app_mem_size", config.getValue(RUNNER_DEFAULT_APP_MEM_SIZE)));
                    add(createLocalReplaceCommand("$runner_workspace_max_memsize", config.getValue(RUNNER_WORKSPACE_MAX_MEMSIZE)));
                    add(createLocalReplaceCommand("$runner_app_lifetime", config.getValue(RUNNER_APP_LIFETIME)));
                    add(createLocalReplaceCommand("$runner_waiting_time", config.getValue(RUNNER_WAITING_TIME)));
                    add(createLocalReplaceCommand(
                            "$workspace_inactive_temporary_stop_time", config.getValue(WORKSPACE_INACTIVE_TEMPORARY_STOP_TIME)));
                    add(createLocalReplaceCommand(
                            "$workspace_inactive_persistent_stop_time", config.getValue(WORKSPACE_INACTIVE_PERSISTENT_STOP_TIME)));
                    add(createLocalReplaceCommand("$mongo_admin_pass", config.getValue(MONGO_ADMIN_PASS)));
                    add(createLocalReplaceCommand("$mongo_user_pass", config.getValue(MONGO_USER_PASS)));
                    add(createLocalReplaceCommand("$mongo_orgservice_user_pwd", config.getValue(MONGO_ORGSERVICE_USER_PWD)));
                    add(createLocalReplaceCommand("$user_ldap_password", config.getValue(USER_LDAP_PASSWORD)));
                    add(createLocalReplaceCommand("$admin_ldap_user_name", config.getValue(ADMIN_LDAP_USER_NAME)));
                    add(createLocalReplaceCommand("$admin_ldap_password", config.getValue(ADMIN_LDAP_PASSWORD)));
                    add(createLocalReplaceCommand("$haproxy_statistic_pass", config.getValue(HAPROXY_STATISTIC_PASS)));
                    add(createLocalReplaceCommand("$mysql_root_password", config.getValue(MYSQL_ROOT_PASSWORD)));
                    add(createLocalReplaceCommand("$zabbix_db_pass", config.getValue(ZABBIX_DB_PASS)));
                    add(createLocalReplaceCommand("$zabbix_time_zone", config.getValue(ZABBIX_TIME_ZONE)));
                    add(createLocalReplaceCommand("$zabbix_admin_email", config.getValue(ZABBIX_ADMIN_EMAIL)));
                    add(createLocalReplaceCommand("$zabbix_admin_password", config.getValue(ZABBIX_ADMIN_PASSWORD)));
                    add(createLocalReplaceCommand("$jmx_username", config.getValue(JMX_USERNAME)));
                    add(createLocalReplaceCommand("$jmx_password", config.getValue(JMX_PASSWORD)));
                    add(createLocalReplaceCommand("$codenvy_server_xmx", config.getValue(CODENVY_SERVER_XMX)));
                    add(createLocalReplaceCommand("$google_client_id", config.getValue(GOOGLE_CLIENT_ID)));
                    add(createLocalReplaceCommand("$google_secret", config.getValue(GOOGLE_SECRET)));
                    add(createLocalReplaceCommand("$github_client_id", config.getValue(GITHUB_CLIENT_ID)));
                    add(createLocalReplaceCommand("$github_secret", config.getValue(GITHUB_SECRET)));
                    add(createLocalReplaceCommand("$bitbucket_client_id", config.getValue(BITBUCKET_CLIENT_ID)));
                    add(createLocalReplaceCommand("$bitbucket_secret", config.getValue(BITBUCKET_SECRET)));
                    add(createLocalReplaceCommand("$wso2_client_id", config.getValue(WSO2_CLIENT_ID)));
                    add(createLocalReplaceCommand("$wso2_secret", config.getValue(WSO2_SECRET)));
                    add(createLocalReplaceCommand("$projectlocker_client_id", config.getValue(PROJECTLOCKER_CLIENT_ID)));
                    add(createLocalReplaceCommand("$projectlocker_secret", config.getValue(PROJECTLOCKER_SECRET)));
                }}, "Configure puppet master");

            case 4:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("sudo chkconfig --add puppetmaster"));
                    add(createLocalCommand("sudo chkconfig puppetmaster on"));
                    add(createLocalCommand("sudo service puppetmaster start"));
                }}, "Launch puppet master");

            case 5:
                return createLocalCommand(format("sudo yum install %s -y", config.getValue(PUPPET_AGENT_VERSION)));

            case 6:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalRestoreOrBackupCommand("/etc/puppet/puppet.conf"));
                    add(createLocalCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                  "  server = %s\\n" +
                                                  "  runinterval = 300\\n" +
                                                  "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getValue(AIO_HOST_URL))));
                    add(createLocalCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                  "  show_diff = true\\n" +
                                                  "  pluginsync = true\\n" +
                                                  "  report = true\\n" +
                                                  "  default_schedules = false\\n" +
                                                  "  certname = %s\\n/g' /etc/puppet/puppet.conf", config.getValue(AIO_HOST_URL))));
                }}, "Configure puppet agent");


            case 7:
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("sleep 30"));
                    add(createLocalCommand("sudo chkconfig --add puppet"));
                    add(createLocalCommand("sudo chkconfig puppet on"));
                    add(createLocalCommand("sudo service puppet start"));
                }}, "Launch puppet agent");

            case 8:
//                TODO [AB] improve
                return new MacroCommand(new ArrayList<Command>() {{
                    add(createLocalCommand("doneState='Starting'; testFile=/home/codenvy/codenvy-tomcat/logs/catalina.out; " +
                                           "while [ \"${doneState}\" != \"Started\" ]; do " +
                                           "    if grep -Fq \"Exception\" ${testFile}; then >&2 echo \"Tomcat starting up failed\"; exit 1; fi; " +
                                           "    if grep -Fq \"Server startup\" ${testFile}; then doneState=Started; fi; " +
                                           "    sleep 60; " +
                                           "done"));
                }}, "Boot Codenvy");


            default:
                throw new IllegalArgumentException(format("Step number %d is out of range", step));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }

    private Command createLocalReplaceCommand(String property, String value) throws ConfigException {
        return createLocalCommand(
                format("sudo sed -i 's/%1$s = .*/%1$s = \"%2$s\"/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                       property,
                       value.replace("/", "\\/")));
    }

    private Command createLocalRestoreOrBackupCommand(final String file) throws ConfigException {
        final String backupFile = file + ".back";
        return createLocalCommand(
                format("if [ ! -f %1$s ]; then sudo cp %2$s %1$s; else sudo cp %1$s %2$s; fi",
                       backupFile,
                       file));
    }
}
