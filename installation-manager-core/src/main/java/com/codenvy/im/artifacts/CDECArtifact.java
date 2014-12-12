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
import com.codenvy.im.command.Command;
import com.codenvy.im.command.MacroCommand;
import com.codenvy.im.config.CodenvySingleServerConfig;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.ConnectException;
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
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of(
            "Disable SELinux",
            "Install puppet server",
            "Unzip Codenvy binaries",
            "Configure puppet master",
            "Launch puppet master",
            "Install puppet agent",
            "Configure puppet agent",
            "Launch puppet agent",
            "Install Codenvy (~25 min)",
            "Boot Codenvy"
        );
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
                return new MacroCommand(ImmutableList.of(
                    createLocalRestoreOrBackupCommand("/etc/selinux/config"),
                    createLocalCommand("if ! grep -Fq \"SELINUX=disabled\" /etc/selinux/config; then " +
                                           "    sudo setenforce 0; " +
                                           "    sudo sed -i s/SELINUX=enforcing/SELINUX=disabled/g /etc/selinux/config; " +
                                           "    sudo sed -i s/SELINUX=permissive/SELINUX=disabled/g /etc/selinux/config; " +
                                           "fi")),
                "Disable SELinux");

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
                return new MacroCommand(ImmutableList.of(
                    createLocalRestoreOrBackupCommand("/etc/puppet/fileserver.conf"),
                    createLocalCommand("sudo sed -i \"\\$a[file]\"                      /etc/puppet/fileserver.conf"),
                    createLocalCommand("sudo sed -i \"\\$a    path /etc/puppet/files\"  /etc/puppet/fileserver.conf"),
                    createLocalCommand("sudo sed -i \"\\$a    allow *\"                 /etc/puppet/fileserver.conf"),
                    createLocalCommand(
                            format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                   "YOUR_DNS_NAME", config.getValue(AIO_HOST_URL))),
                    createLocalReplaceCommand("$host_url", config.getValue(AIO_HOST_URL)),
                    createLocalReplaceCommand("$builder_max_execution_time", config.getValue(BUILDER_MAX_EXECUTION_TIME)),
                    createLocalReplaceCommand("$builder_waiting_time", config.getValue(BUILDER_WAITING_TIME)),
                    createLocalReplaceCommand("$builder_keep_result_time", config.getValue(BUILDER_KEEP_RESULT_TIME)),
                    createLocalReplaceCommand("$builder_queue_size", config.getValue(BUILDER_QUEUE_SIZE)),
                    createLocalReplaceCommand("$runner_default_app_mem_size", config.getValue(RUNNER_DEFAULT_APP_MEM_SIZE)),
                    createLocalReplaceCommand("$runner_workspace_max_memsize", config.getValue(RUNNER_WORKSPACE_MAX_MEMSIZE)),
                    createLocalReplaceCommand("$runner_app_lifetime", config.getValue(RUNNER_APP_LIFETIME)),
                    createLocalReplaceCommand("$runner_waiting_time", config.getValue(RUNNER_WAITING_TIME)),
                    createLocalReplaceCommand(
                            "$workspace_inactive_temporary_stop_time", config.getValue(WORKSPACE_INACTIVE_TEMPORARY_STOP_TIME)),
                    createLocalReplaceCommand(
                            "$workspace_inactive_persistent_stop_time", config.getValue(WORKSPACE_INACTIVE_PERSISTENT_STOP_TIME)),
                    createLocalReplaceCommand("$mongo_admin_pass", config.getValue(MONGO_ADMIN_PASS)),
                    createLocalReplaceCommand("$mongo_user_pass", config.getValue(MONGO_USER_PASS)),
                    createLocalReplaceCommand("$mongo_orgservice_user_pwd", config.getValue(MONGO_ORGSERVICE_USER_PWD)),
                    createLocalReplaceCommand("$user_ldap_password", config.getValue(USER_LDAP_PASSWORD)),
                    createLocalReplaceCommand("$admin_ldap_user_name", config.getValue(ADMIN_LDAP_USER_NAME)),
                    createLocalReplaceCommand("$admin_ldap_password", config.getValue(ADMIN_LDAP_PASSWORD)),
                    createLocalReplaceCommand("$haproxy_statistic_pass", config.getValue(HAPROXY_STATISTIC_PASS)),
                    createLocalReplaceCommand("$mysql_root_password", config.getValue(MYSQL_ROOT_PASSWORD)),
                    createLocalReplaceCommand("$zabbix_db_pass", config.getValue(ZABBIX_DB_PASS)),
                    createLocalReplaceCommand("$zabbix_time_zone", config.getValue(ZABBIX_TIME_ZONE)),
                    createLocalReplaceCommand("$zabbix_admin_email", config.getValue(ZABBIX_ADMIN_EMAIL)),
                    createLocalReplaceCommand("$zabbix_admin_password", config.getValue(ZABBIX_ADMIN_PASSWORD)),
                    createLocalReplaceCommand("$jmx_username", config.getValue(JMX_USERNAME)),
                    createLocalReplaceCommand("$jmx_password", config.getValue(JMX_PASSWORD)),
                    createLocalReplaceCommand("$codenvy_server_xmx", config.getValue(CODENVY_SERVER_XMX)),
                    createLocalReplaceCommand("$google_client_id", config.getValue(GOOGLE_CLIENT_ID)),
                    createLocalReplaceCommand("$google_secret", config.getValue(GOOGLE_SECRET)),
                    createLocalReplaceCommand("$github_client_id", config.getValue(GITHUB_CLIENT_ID)),
                    createLocalReplaceCommand("$github_secret", config.getValue(GITHUB_SECRET)),
                    createLocalReplaceCommand("$bitbucket_client_id", config.getValue(BITBUCKET_CLIENT_ID)),
                    createLocalReplaceCommand("$bitbucket_secret", config.getValue(BITBUCKET_SECRET)),
                    createLocalReplaceCommand("$wso2_client_id", config.getValue(WSO2_CLIENT_ID)),
                    createLocalReplaceCommand("$wso2_secret", config.getValue(WSO2_SECRET)),
                    createLocalReplaceCommand("$projectlocker_client_id", config.getValue(PROJECTLOCKER_CLIENT_ID)),
                    createLocalReplaceCommand("$projectlocker_secret", config.getValue(PROJECTLOCKER_SECRET))),
                "Configure puppet master");

            case 4:
                return new MacroCommand(ImmutableList.<Command>of(
                    createLocalCommand("sudo chkconfig --add puppetmaster"),
                    createLocalCommand("sudo chkconfig puppetmaster on"),
                    createLocalCommand("sudo service puppetmaster start")),
                 "Launch puppet master");

            case 5:
                return createLocalCommand(format("sudo yum install %s -y", config.getValue(PUPPET_AGENT_VERSION)));

            case 6:
                return new MacroCommand(ImmutableList.of(
                    createLocalRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
                    createLocalCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                  "  server = %s\\n" +
                                                  "  runinterval = 300\\n" +
                                                  "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getValue(AIO_HOST_URL))),
                    createLocalCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                  "  show_diff = true\\n" +
                                                  "  pluginsync = true\\n" +
                                                  "  report = true\\n" +
                                                  "  default_schedules = false\\n" +
                                                  "  certname = %s\\n/g' /etc/puppet/puppet.conf", config.getValue(AIO_HOST_URL)))),
                 "Configure puppet agent");


            case 7:
                return new MacroCommand(ImmutableList.<Command>of(
                    createLocalCommand("sleep 30"),
                    createLocalCommand("sudo chkconfig --add puppet"),
                    createLocalCommand("sudo chkconfig puppet on"),
                    createLocalCommand("sudo service puppet start")),
                 "Launch puppet agent");

            case 8:
                return new MacroCommand(ImmutableList.<Command>of(
                        createLocalCommand("doneState=\"Installing\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                           "    sleep 30; " +
                                           "    if [ -f ${testFile} ]; then doneState=\"Installed\"; fi; " +
                                           "done")),
                                        "Install Codenvy");

            case 9:
                return new MacroCommand(ImmutableList.<Command>of(
                        createLocalCommand("doneState=\"Booting\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Booted\" ]; do " +
                                           "    sleep 30; " +
                                           "    if grep -Fq \"Exception\" ${testFile}; then >&2 echo \"Tomcat starting up failed\"; exit 1; fi; " +
                                           "    if grep -Fq \"Server startup\" ${testFile}; then doneState=\"Booted\"; fi; " +
                                           "done")),
                                        "Boot Codenvy");

            default:
                throw new IllegalArgumentException(format("Step number %d is out of range", step));
        }
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
