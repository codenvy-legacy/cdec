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
import com.codenvy.im.config.Config;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.command.SimpleCommand.createLocalCommand;
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
    public Version getInstalledVersion() throws IOException {
        String response;
        try {
            response = transport.doOption(combinePaths(apiNodeUrl, "api/"), null);
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


    /** {@inheritDoc} */
    @Override
    public List<String> getUpdateInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of("Unzip Codenvy binaries to /tmp/cdec",
                                "Configure Codenvy",
                                "Patch resources",
                                "Move Codenvy binaries to /etc/puppet",
                                "Restart Codenvy",
                                "Patch resources");
    }

    /** {@inheritDoc} */
    @Override
    public Command getUpdateCommand(Version version, Path pathToBinaries, InstallOptions installOptions) throws IOException {
        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

        switch (step) {
            case 0:
                return createLocalCommand(format("rm -rf /tmp/cdec; " +
                                                 "mkdir /tmp/cdec/; " +
                                                 "unzip -o %s -d /tmp/cdec", pathToBinaries.toString()));

            case 1:
                List<Command> commands = new ArrayList<>();
                commands.add(createLocalCommand(format("sudo sed -i 's/%s/%s/g' /etc/puppet/manifests/nodes/single_server/single_server.pp",
                                                       "YOUR_DNS_NAME", config.getHostUrl())));
                for (Map.Entry<String, String> e : config.getProperties().entrySet()) {
                    String property = e.getKey();
                    String value = e.getValue();

                    commands.add(createLocalReplaceCommand(Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalReplaceCommand(Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

            case 2:
                String patchFile = "/tmp/cdec/patches/" + version.toString() + "/before-" + getInstalledVersion() + ".sh";
                return createLocalCommand(format("if [ -f %1$s ]; then bash %1$s; fi", patchFile));

            case 3:
                return createLocalCommand("rm -rf /etc/puppet/files; " +
                                          "rm -rf /etc/puppet/modules; " +
                                          "rm -rf /etc/puppet/manifests; " +
                                          "sudo mv /tmp/cdec/* /etc/puppet");

            case 4:
                return new MacroCommand(ImmutableList.<Command>of(
                        createLocalCommand("doneState=\"Stopping\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Stopped\" ]; do " +
                                           "    sleep 5; " +
                                           "    if ! sudo test -f ${testFile}; then doneState=\"Stopped\"; fi; " +
                                           "    if ! sudo grep -Fq \"Server startup\" ${testFile}; then doneState=\"Stopped\"; fi; " +
                                           "done"),
                        createLocalCommand("doneState=\"Starting\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Started\" ]; do " +
                                           "    sleep 5; " +
                                           "    if sudo grep -Fq \"Exception\" ${testFile}; then >&2 echo \"Tomcat starting up failed\"; exit 1; " +
                                           "fi; " +
                                           "    if sudo grep -Fq \"Server startup\" ${testFile}; then doneState=\"Started\"; fi; " +
                                           "done")),
                                        "Restart Codenvy");

            case 5:
                patchFile = "/tmp/cdec/patches/" + version.toString() + "/after-" + getInstalledVersion() + ".sh";
                return createLocalCommand(format("if [ -f %1$s ]; then bash %1$s; fi", patchFile));

            default:
                throw new IllegalArgumentException(format("Step number %d is out of update range", step));
        }
    }

    @Override
    public List<String> getInstallInfo(InstallOptions installOptions) throws IOException {
        return ImmutableList.of("Disable SELinux",
                                "Install puppet server",
                                "Unzip Codenvy binaries",
                                "Configure puppet master",
                                "Launch puppet master",
                                "Install puppet agent",
                                "Configure puppet agent",
                                "Launch puppet agent",
                                "Install Codenvy (~25 min)",
                                "Boot Codenvy");
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(final Version version,
                                     final Path pathToBinaries,
                                     final InstallOptions installOptions) throws IOException {

        final Config config = new Config(installOptions.getConfigProperties());
        final int step = installOptions.getStep();

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
                            + format("then sudo yum install %s -y", config.getValue(Config.PUPPET_RESOURCE_URL))
                            + "; fi"));
                    add(createLocalCommand(
                            format("sudo yum install %s -y", config.getValue(Config.PUPPET_SERVER_VERSION))));
                }}, "Install puppet server");

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

                    commands.add(createLocalReplaceCommand(Config.SINGLE_SERVER_PROPERTIES, "$" + property, value));
                    commands.add(createLocalReplaceCommand(Config.SINGLE_SERVER_BASE_PROPERTIES, "$" + property, value));
                }

                return new MacroCommand(commands, "Configure puppet master");

            case 4:
                return new MacroCommand(ImmutableList.<Command>of(
                        createLocalCommand("sudo chkconfig --add puppetmaster"),
                        createLocalCommand("sudo chkconfig puppetmaster on"),
                        createLocalCommand("sudo service puppetmaster start")),
                                        "Launch puppet master");

            case 5:
                return createLocalCommand(format("sudo yum install %s -y", config.getValue(Config.PUPPET_AGENT_VERSION)));

            case 6:
                return new MacroCommand(ImmutableList.of(
                        createLocalRestoreOrBackupCommand("/etc/puppet/puppet.conf"),
                        createLocalCommand(format("sudo sed -i 's/\\[main\\]/\\[main\\]\\n" +
                                                  "  server = %s\\n" +
                                                  "  runinterval = 300\\n" +
                                                  "  configtimeout = 600\\n/g' /etc/puppet/puppet.conf", config.getHostUrl())),
                        createLocalCommand(format("sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n" +
                                                  "  show_diff = true\\n" +
                                                  "  pluginsync = true\\n" +
                                                  "  report = true\\n" +
                                                  "  default_schedules = false\\n" +
                                                  "  certname = %s\\n/g' /etc/puppet/puppet.conf", config.getHostUrl()))),
                                        "Configure puppet agent");


            case 7:
                return new MacroCommand(ImmutableList.<Command>of(
                        createLocalCommand("sleep 30"),
                        createLocalCommand("sudo chkconfig --add puppet"),
                        createLocalCommand("sudo chkconfig puppet on"),
                        createLocalCommand("sudo service puppet start")),
                                        "Launch puppet agent");

            case 8:
                return createLocalCommand("doneState=\"Installing\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Installed\" ]; do " +
                                           "    sleep 30; " +
                                           "    if sudo test -f ${testFile}; then doneState=\"Installed\"; fi; " +
                                           "done");

            case 9:
                return createLocalCommand("doneState=\"Booting\"; " +
                                           "testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; " +
                                           "while [ \"${doneState}\" != \"Booted\" ]; do " +
                                           "    sleep 5; " +
                                           "    if sudo grep -Fq \"Exception\" ${testFile}; then >&2 echo \"Tomcat starting up failed\"; exit 1; " +
                                           "fi; " +
                                           "    if sudo grep -Fq \"Server startup\" ${testFile}; then doneState=\"Booted\"; fi; " +
                                           "done");

            default:
                throw new IllegalArgumentException(format("Step number %d is out of install range", step));
        }
    }

    private Command createLocalReplaceCommand(String file, String property, String value) throws ConfigException {
        return createLocalCommand(
                format("sudo sed -i 's/%1$s = .*/%1$s = \"%2$s\"/g' %3$s",
                       property,
                       value.replace("/", "\\/"),
                       file));
    }

    private Command createLocalRestoreOrBackupCommand(final String file) throws ConfigException {
        final String backupFile = file + ".back";
        return createLocalCommand(
                format("if [ ! -f %1$s ]; then sudo cp %2$s %1$s; else sudo cp %1$s %2$s; fi",
                       backupFile,
                       file));
    }
}
