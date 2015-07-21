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
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.commands.decorators.PuppetErrorInterrupter.PUPPET_LOG_FILE;
import static com.codenvy.im.commands.decorators.PuppetErrorInterrupter.READ_LOG_TIMEOUT_MILLIS;
import static com.codenvy.im.commands.decorators.PuppetErrorInterrupter.useSudo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public class TestPuppetErrorInterrupterLocally {
    static final int  MOCK_COMMAND_TIMEOUT_MILLIS = READ_LOG_TIMEOUT_MILLIS * 16;

    static final Path BASE_TMP_DIRECTORY                         = Paths.get("target/tmp");
    static final Path REPORT_TMP_DIRECTORY                       = Paths.get("target/tmp/report");
    static final Path TEST_TMP_DIRECTORY                         = Paths.get("target/tmp/test");
    static final Path LOG_TMP_DIRECTORY                          = Paths.get("target/tmp/log");

    static final Path ORIGIN_PUPPET_LOG                          = PUPPET_LOG_FILE;

    static final Path ORIGIN_BASE_TMP_DIRECTORY                  = PuppetErrorReport.BASE_TMP_DIRECTORY;
    static final Path ORIGIN_CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG;

    @Mock
    Command mockCommand;

    @Mock
    ConfigManager mockConfigManager;

    PuppetErrorInterrupter testInterrupter;

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

        Files.createDirectory(BASE_TMP_DIRECTORY);
        Files.createDirectory(REPORT_TMP_DIRECTORY);
        Files.createDirectory(LOG_TMP_DIRECTORY);
        Files.createDirectory(TEST_TMP_DIRECTORY);

        // create puppet log file
        Path puppetLogFile = LOG_TMP_DIRECTORY.resolve("messages");
        FileUtils.write(puppetLogFile.toFile(), logWithoutErrorMessages);

        testInterrupter = spy(new PuppetErrorInterrupter(mockCommand, mockConfigManager));
        PUPPET_LOG_FILE = puppetLogFile;
        useSudo = false;   // prevents asking sudo password when running the tests locally

        // create IM CLI client log
        Path imLogFile = TEST_TMP_DIRECTORY.resolve(PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG.getFileName());
        FileUtils.write(imLogFile.toFile(), "");

        PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = imLogFile;
        PuppetErrorReport.BASE_TMP_DIRECTORY = REPORT_TMP_DIRECTORY;
        PuppetErrorReport.useSudo = false;   // prevents asking sudo password when running the tests locally

        // prepare Codenvy Config
        doReturn(InstallType.SINGLE_SERVER).when(mockConfigManager).detectInstallationType();
        doReturn(new Config(ImmutableMap.of(
            Config.HOST_URL, "localhost",
            Config.ADMIN_LDAP_USER_NAME, "admin",
            Config.SYSTEM_LDAP_PASSWORD, "password"
        )))
        .when(mockConfigManager).loadInstalledCodenvyConfig();
    }

    @Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testInterruptWhenAddError() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        final String puppetErrorMessage =
            "Jun 17 10:03:40 ns2 puppet-agent[23932]: (/Stage[main]/Third_party::Zabbix::Server_config/Exec[init_zabbix_db]) Dependency Exec[set-mysql-password] has failures: true\r";

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
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

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS / 2);

                    // append error message into puppet log file
                    FileUtils.write(PUPPET_LOG_FILE.toFile(), puppetErrorMessage, true);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        });

        try {
            testInterrupter.execute();
        } catch(Exception e) {
            assertEquals(e.getClass(), PuppetErrorException.class, Arrays.toString(e.getStackTrace()));

            String errorMessage = e.getMessage();

            Pattern errorMessagePattern = Pattern.compile("Puppet error: 'Dependency Exec\\[set-mysql-password\\] has failures: true'. "
                                                          + "At the time puppet is continue Codenvy installation in background and is trying to fix this issue. "
                                                          + "Check administrator dashboard page http://localhost/admin to verify installation success [(]credentials: admin/password[)]. "
                                                          + "If the installation eventually fails, contact support with error report target/reports/error_report_.*.tar.gz. "
                                                          + "Installation & Troubleshooting Docs: http://docs.codenvy.com/onpremises/installation-single-node/#install-troubleshooting.");

            assertTrue(errorMessagePattern.matcher(errorMessage).find());

            assertErrorReport(errorMessage, logWithoutErrorMessages + puppetErrorMessage);
            return;
        }

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }

        fail("testInterrupter.execute() should throw PuppetErrorException");
    }

    private void assertErrorReport(String errorMessage, String expectedContentOfLogFile) throws IOException, InterruptedException {
        Pattern errorReportInfoPattern = Pattern.compile("target/reports/error_report_.*.tar.gz");
        Matcher pathToReportMatcher = errorReportInfoPattern.matcher(errorMessage);
        assertTrue(pathToReportMatcher.find());

        Path report = Paths.get(pathToReportMatcher.group());
        assertNotNull(report);
        assertTrue(Files.exists(report));

        CommandLibrary.createUnpackCommand(report, TEST_TMP_DIRECTORY).execute();
        Path puppetLogFile = TEST_TMP_DIRECTORY.resolve(PUPPET_LOG_FILE.getFileName());
        assertTrue(Files.exists(puppetLogFile));
        String puppetLogFileContent = FileUtils.readFileToString(puppetLogFile.toFile());
        assertEquals(puppetLogFileContent, expectedContentOfLogFile);

        Path imLogfile = TEST_TMP_DIRECTORY.resolve(PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG.getFileName());
        assertTrue(Files.exists(imLogfile));
    }

    @Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testNotInterruptWhenAddNoError() throws InterruptedException, IOException {
        final String[] failMessage = {null};
        final String expectedResult = "okay";

        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
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

                    FileUtils.write(PUPPET_LOG_FILE.toFile(), errorMessage, true);
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
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
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
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
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

    @AfterMethod
    public void tearDown() throws InterruptedException {
        PUPPET_LOG_FILE = ORIGIN_PUPPET_LOG;
        useSudo = true;

        PuppetErrorReport.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;
        PuppetErrorReport.CLI_CLIENT_NON_INTERACTIVE_MODE_LOG = ORIGIN_CLI_CLIENT_NON_INTERACTIVE_MODE_LOG;
        PuppetErrorReport.useSudo = true;

        FileUtils.deleteQuietly(BASE_TMP_DIRECTORY.toFile());
    }
}
