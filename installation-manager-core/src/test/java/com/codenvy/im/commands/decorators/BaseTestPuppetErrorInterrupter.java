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
package com.codenvy.im.commands.decorators;

import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.commands.decorators.PuppetErrorInterrupter.READ_LOG_TIMEOUT_MILLIS;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public abstract class BaseTestPuppetErrorInterrupter {
    static final int MOCK_COMMAND_TIMEOUT_MILLIS = READ_LOG_TIMEOUT_MILLIS * 16;

    static final Path BASE_TMP_DIRECTORY   = Paths.get("target/tmp").toAbsolutePath();
    static final Path REPORT_TMP_DIRECTORY = Paths.get("target/tmp/report").toAbsolutePath();
    static final Path TEST_TMP_DIRECTORY   = Paths.get("target/tmp/test").toAbsolutePath();
    static final Path LOG_TMP_DIRECTORY    = Paths.get("target/tmp/log").toAbsolutePath();

    static final Path ORIGIN_BASE_TMP_DIRECTORY                  = PuppetErrorReport.BASE_TMP_DIRECTORY;
    static final Path ORIGIN_CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG;

    @Mock
    Command mockCommand;

    @Mock
    ConfigManager mockConfigManager;

    PuppetErrorInterrupter spyInterrupter;

    String logWithoutErrorMessages =
        "Jun  8 14:53:53 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.13 seconds\n"
        + "Jun  8 14:53:55 test puppet-agent[22276]: Finished catalog run in 1.98 seconds\n"
        + "Jun  8 15:17:31 test systemd[1]: Time has been changed\n"
        + "Jun  8 15:17:40 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.13 seconds\n"
        + "Jun  8 15:17:42 test puppet-agent[22754]: Finished catalog run in 1.83 seconds\n"
        + "Jun  8 15:22:40 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.23 seconds\n"
        + "Jun  8 15:22:42 test puppet-agent[23240]: Finished catalog run in 1.95 seconds\n"
        + "Jun  8 15:27:40 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.12 seconds\n"
        + "Jun  8 15:27:42 test puppet-agent[23713]: Finished catalog run in 2.01 seconds\n"
        + "Jun  8 15:51:51 test systemd[1]: Time has been changed\n"
        + "Jun  8 15:51:57 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.13 seconds\n"
        + "Jun  8 15:52:00 test puppet-agent[24198]: Finished catalog run in 2.04 seconds\n"
        + "Jun  8 15:56:57 test puppet-master[5409]: Compiled catalog for test.com in environment production in 0.13 seconds\n"
        + "Jun  8 15:56:59 test puppet-agent[24672]: Finished catalog run in 1.67 seconds\n";

    @BeforeMethod
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        deleteIfExists(BASE_TMP_DIRECTORY);

        createDirectory(BASE_TMP_DIRECTORY);
        createDirectory(REPORT_TMP_DIRECTORY);
        createDirectory(LOG_TMP_DIRECTORY);
        createDirectory(TEST_TMP_DIRECTORY);

        spyInterrupter = getSpyInterrupter();
        when(spyInterrupter.useSudo()).thenReturn(false);   // prevents asking sudo password when running the tests locally

        // create local puppet log file
        Path puppetLogFile = LOG_TMP_DIRECTORY.resolve(spyInterrupter.getPuppetLogFile().getFileName());
        FileUtils.write(puppetLogFile.toFile(), logWithoutErrorMessages);
        when(spyInterrupter.getPuppetLogFile()).thenReturn(puppetLogFile);

        // create IM CLI client log
        Path imLogFile = TEST_TMP_DIRECTORY.resolve(PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG.getFileName());
        FileUtils.write(imLogFile.toFile(), "");

        PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = imLogFile;
        PuppetErrorReport.BASE_TMP_DIRECTORY = REPORT_TMP_DIRECTORY;
        PuppetErrorReport.useSudo = false;   // prevents asking sudo password when running the tests locally

        // prepare Codenvy Config
        doReturn(getInstallType()).when(mockConfigManager).detectInstallationType();
        doReturn(new Config(ImmutableMap.of(
            Config.HOST_URL, "localhost",
            Config.ADMIN_LDAP_USER_NAME, "admin",
            Config.SYSTEM_LDAP_PASSWORD, "password"
        )))
            .when(mockConfigManager).loadInstalledCodenvyConfig();
    }

    abstract public PuppetErrorInterrupter getSpyInterrupter();

    abstract public InstallType getInstallType();

    abstract public PuppetError getTestPuppetError();

    @AfterMethod
    public void tearDown() throws InterruptedException, IOException {
        PuppetErrorReport.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
        PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = ORIGIN_CLI_CLIENT_NON_INTERACTIVE_MODE_LOG;
        PuppetErrorReport.useSudo = true;

        deleteDirectory(BASE_TMP_DIRECTORY.toFile());
    }

    @DataProvider
    public Object[][] getDataToCheckPuppetError() {
        return new Object[][] {
            {// only 1 error message
             "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Failed to generate additional resources using 'eval_generate': getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Could not evaluate: Could not retrieve file metadata for puppet://puppet/plugins: getaddrinfo: Name or service not known\n"
             + "Wrapped exception:\n"
             + "getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:35 +0100 Puppet (notice): Using cached catalog\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not retrieve catalog; skipping run\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not send report: getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:18:34 +0100 Puppet (warning): Unable to fetch my node definition, but the agent run will continue:\n"
             + "2015-07-30 13:18:34 +0100 Puppet (warning): getaddrinfo: Name or service not known",
             null},

            // 3 (= min_error_events_to_interrupt_im) the equal error messages
            {"2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Failed to generate additional resources using 'eval_generate': getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Could not evaluate: Could not retrieve file metadata for puppet://puppet/plugins: getaddrinfo: Name or service not known\n"
             + "Wrapped exception:\n"
             + "getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n",
             null},

            // 3 different error messages
            {"2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Failed to generate additional resources using 'eval_generate': getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Could not evaluate: Could not retrieve file metadata for puppet://puppet/plugins: getaddrinfo: Name or service not known\n"
             + "Wrapped exception:\n"
             + "getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:12:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n"
             + "2015-07-30 16:14:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:15:52 +0100 /Stage[main]/Third_party::another (notice): Dependency Package[another] has failures: true\n"
             + "2015-07-29 16:16:52 +0100 /Stage[main]/Third_party::yet-another (notice): Dependency Package[yet-another] has failures: true\n",
             null},

            // 2 similar error messages
            {"2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Failed to generate additional resources using 'eval_generate': getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Could not evaluate: Could not retrieve file metadata for puppet://puppet/plugins: getaddrinfo: Name or service not known\n"
             + "Wrapped exception:\n"
             + "getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:12:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n"
             + "2015-07-30 16:14:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:15:52 +0100 /Stage[main]/Third_party::another (notice): Dependency Package[another] has failures: true\n"
             + "2015-07-29 16:16:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n",
             null},

            // 3 similar error messages
            {"2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Failed to generate additional resources using 'eval_generate': getaddrinfo: Name or service not known\n"
             + "2015-07-30 13:13:34 +0100 /File[/var/lib/puppet/lib] (err): Could not evaluate: Could not retrieve file metadata for puppet://puppet/plugins: getaddrinfo: Name or service not known\n"
             + "Wrapped exception:\n"
             + "getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:12:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n"
             + "2015-07-30 16:14:35 +0100 Puppet (err): Could not retrieve catalog from remote server: getaddrinfo: Name or service not known\n"
             + "2015-07-29 16:15:52 +0100 /Stage[main]/Third_party::another (notice): Dependency Package[another] has failures: true\n"
             + "2015-07-29 16:16:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n"
             + "2015-07-29 16:17:52 +0100 /Stage[main]/Third_party::another (notice): Dependency Package[another] has failures: true\n"
             + "2015-07-29 16:18:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n",
             getTestPuppetError()}
        };
    }

    protected void assertLocalErrorReport(String errorMessage, String expectedContentOfLogFile) throws IOException, InterruptedException {
        Pattern errorReportInfoPattern = Pattern.compile("target/reports/error_report_.*.tar.gz");
        Matcher pathToReportMatcher = errorReportInfoPattern.matcher(errorMessage);
        assertTrue(pathToReportMatcher.find());

        Path report = Paths.get(pathToReportMatcher.group());
        assertNotNull(report);
        assertTrue(exists(report));

        CommandLibrary.createUnpackCommand(report, TEST_TMP_DIRECTORY).execute();
        Path puppetLogFile = TEST_TMP_DIRECTORY.resolve(spyInterrupter.getPuppetLogFile().getFileName());
        assertTrue(exists(puppetLogFile));
        String puppetLogFileContent = FileUtils.readFileToString(puppetLogFile.toFile());
        assertEquals(puppetLogFileContent, expectedContentOfLogFile);

        Path imLogfile = TEST_TMP_DIRECTORY.resolve(PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG.getFileName());
        assertTrue(exists(imLogfile));
    }


    protected void assertNodeErrorReport(String errorMessage, String expectedContentOfLogFile, NodeConfig testNode)
        throws IOException, InterruptedException {
        Pattern errorReportInfoPattern = Pattern.compile("target/reports/error_report_.*.tar.gz");
        Matcher pathToReportMatcher = errorReportInfoPattern.matcher(errorMessage);
        assertTrue(pathToReportMatcher.find());

        Path report = Paths.get(pathToReportMatcher.group());
        assertNotNull(report);
        assertTrue(exists(report));

        CommandLibrary.createUnpackCommand(report, TEST_TMP_DIRECTORY).execute();
        Path puppetLogFile =
            TEST_TMP_DIRECTORY.resolve(testNode.getType().toString().toLowerCase()).resolve(spyInterrupter.getPuppetLogFile().getFileName());
        assertTrue(exists(puppetLogFile));

        String logFileContent = FileUtils.readFileToString(puppetLogFile.toFile());
        assertEquals(logFileContent, expectedContentOfLogFile);
    }
}
