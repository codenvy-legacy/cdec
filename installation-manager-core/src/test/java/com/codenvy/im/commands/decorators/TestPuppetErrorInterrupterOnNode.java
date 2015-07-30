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
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.commands.decorators.PuppetErrorInterrupter.PUPPET_LOG_FILE;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.deleteIfExists;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public class TestPuppetErrorInterrupterOnNode {
    static final int MOCK_COMMAND_TIMEOUT_MILLIS = PuppetErrorInterrupter.READ_LOG_TIMEOUT_MILLIS * 16;

    static final Path BASE_TMP_DIRECTORY   = Paths.get("target/tmp").toAbsolutePath();
    static final Path REPORT_TMP_DIRECTORY = Paths.get("target/tmp/report").toAbsolutePath();
    static final Path TEST_TMP_DIRECTORY   = Paths.get("target/tmp/test").toAbsolutePath();
    static final Path LOG_TMP_DIRECTORY    = Paths.get("target/tmp/log").toAbsolutePath();

    static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    static final Path ORIGIN_PUPPET_LOG         = PuppetErrorInterrupter.PUPPET_LOG_FILE;
    static final Path ORIGIN_BASE_TMP_DIRECTORY = PuppetErrorReport.BASE_TMP_DIRECTORY;

    @Mock
    Command mockCommand;

    @Mock
    ConfigManager mockConfigManager;

    PuppetErrorInterrupter testInterrupter;

    NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.API, "127.0.0.1", SYSTEM_USER_NAME);

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

        // create puppet log file
        Path puppetLogFile = LOG_TMP_DIRECTORY.resolve(PUPPET_LOG_FILE.getFileName()).toAbsolutePath();  // absolute path is needed to execute ssh commands
        FileUtils.write(puppetLogFile.toFile(), logWithoutErrorMessages);

        testInterrupter = spy(new PuppetErrorInterrupter(mockCommand, Collections.singletonList(testNode), mockConfigManager));
        PuppetErrorInterrupter.PUPPET_LOG_FILE = puppetLogFile;
        PuppetErrorInterrupter.useSudo = false;  // prevents asking sudo password when running the tests locally

        PuppetErrorReport.BASE_TMP_DIRECTORY = REPORT_TMP_DIRECTORY;
        PuppetErrorReport.useSudo = false;  // prevents asking sudo password when running the tests locally

        // prepare Codenvy Config
        doReturn(InstallType.MULTI_SERVER).when(mockConfigManager).detectInstallationType();
        doReturn(new Config(ImmutableMap.of(
                Config.HOST_URL, "localhost",
                Config.ADMIN_LDAP_USER_NAME, "admin",
                Config.SYSTEM_LDAP_PASSWORD, "password"
                                           )))
                .when(mockConfigManager).loadInstalledCodenvyConfig();
    }

    //@Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testInterruptWhenAddError() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        final String puppetErrorMessage = "2015-07-29 16:01:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n"
                                          + "2015-07-29 16:02:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n"
                                          + "2015-07-29 16:03:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                    failMessage[0] = "mockCommand should be interrupted by testInterrupter, but wasn't";
                    return null;
                } catch (InterruptedException e) {
                    // it's okay here
                    return null;
                }
            }
        }).when(mockCommand).execute();

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS / 2);
                    FileUtils.writeStringToFile(PuppetErrorInterrupter.PUPPET_LOG_FILE.toFile(), puppetErrorMessage, true);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }.start();

        try {
            testInterrupter.execute();
        } catch (Exception e) {
            assertEquals(e.getClass(), PuppetErrorException.class);

            String errorMessage = e.getMessage();

            Pattern errorMessagePattern = Pattern.compile(
                "Puppet error at the API node '127.0.0.1': 'Could not retrieve catalog from remote server: No route to host - connect[(]2[)]'. "
                + "At the time puppet is continue Codenvy installation in background and is trying to fix this issue. "
                + "Check administrator dashboard page http://localhost/admin to verify installation success [(]credentials: admin/password[)]. "
                + "If the installation eventually fails, contact support with error report target/reports/error_report_.*.tar.gz. "
                + "Installation & Troubleshooting Docs: http://docs.codenvy.com/onpremises/installation-multi-node/#install-troubleshooting.");

            assertTrue(errorMessage, errorMessagePattern.matcher(errorMessage).find());

            assertErrorReport(errorMessage, logWithoutErrorMessages + puppetErrorMessage, testNode);
            return;
        }

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }

        fail("testInterrupter.execute() should throw PuppetErrorException");
    }

    private void assertErrorReport(String errorMessage, String expectedContentOfLogFile, NodeConfig testNode)
        throws IOException, InterruptedException {
        Pattern errorReportInfoPattern = Pattern.compile("target/reports/error_report_.*.tar.gz");
        Matcher pathToReportMatcher = errorReportInfoPattern.matcher(errorMessage);
        assertTrue(pathToReportMatcher.find());

        Path report = Paths.get(pathToReportMatcher.group());
        assertNotNull(report);
        assertTrue(Files.exists(report));

        CommandLibrary.createUnpackCommand(report, TEST_TMP_DIRECTORY).execute();
        Path puppetLogFile =
            TEST_TMP_DIRECTORY.resolve(testNode.getType().toString().toLowerCase()).resolve(PuppetErrorInterrupter.PUPPET_LOG_FILE.getFileName());
        assertTrue(Files.exists(puppetLogFile));

        String logFileContent = FileUtils.readFileToString(puppetLogFile.toFile());
        assertEquals(logFileContent, expectedContentOfLogFile);
    }

    @Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testNotInterruptWhenAddNoError() throws InterruptedException, IOException {
        final String[] failMessage = {null};
        final String expectedResult = "okay";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                    return expectedResult;
                } catch (InterruptedException e) {
                    failMessage[0] = "mockCommand should not be interrupted by testInterrupter, but was.";
                    return null;
                }
            }
        }).when(mockCommand).execute();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS / 2);

                    // append non-error message into puppet log file
                    String errorMessage = "Jun  8 15:56:59 test puppet-agent[10240]: dummy message";

                    FileUtils.write(PuppetErrorInterrupter.PUPPET_LOG_FILE.toFile(), errorMessage, true);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });

        String result = testInterrupter.execute();
        assertEquals(result, expectedResult);

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }
    }

    @Test(expectedExceptions = CommandException.class,
          expectedExceptionsMessageRegExp = "error",
          timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testRethrowCommandExceptionByInterrupter() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                    throw new CommandException("error");
                } catch (InterruptedException e) {
                    failMessage[0] = "mockCommand should not be interrupted by testInterrupter, but was.";
                    return null;
                }
            }
        }).when(mockCommand).execute();

        testInterrupter.execute();

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }
    }

    @Test(expectedExceptions = RuntimeException.class,
          expectedExceptionsMessageRegExp = "error",
          timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testRethrowRuntimeExceptionByInterrupter() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                    throw new RuntimeException("error");
                } catch (InterruptedException e) {
                    failMessage[0] = "mockCommand should not be interrupted by testInterrupter, but was.";
                    return null;
                }
            }
        }).when(mockCommand).execute();

        testInterrupter.execute();

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }
    }

    @Test(dataProvider = "dataForCheckPuppetError")
    public void testCheckPuppetError(String puppetLog, PuppetError expectedError) {
        List<String> lines = Arrays.asList(puppetLog.split("\n"));

        PuppetError error = null;
        for (String line : lines) {
            error = testInterrupter.checkPuppetError(testNode, line);
        }

        assertEquals(error, expectedError);
    }

    @DataProvider(name = "dataForCheckPuppetError")
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
             new PuppetError(testNode, "Dependency Package[openldap] has failures: true")}
        };
    }

    @AfterMethod
    public void tearDown() throws InterruptedException, IOException {
        PuppetErrorInterrupter.PUPPET_LOG_FILE = ORIGIN_PUPPET_LOG;
        PuppetErrorInterrupter.useSudo = true;

        PuppetErrorReport.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
        PuppetErrorReport.useSudo = true;

        deleteDirectory(BASE_TMP_DIRECTORY.toFile());
    }
}
