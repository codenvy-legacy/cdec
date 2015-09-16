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

import com.codenvy.im.SshServerFactory;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.SshServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
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

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public class TestPuppetErrorInterrupterOnNode extends BaseTestPuppetErrorInterrupter {
    SshServer sshd;

    NodeConfig testNode;

    @BeforeClass
    private void startSshServers() throws InterruptedException, IOException {
        sshd = SshServerFactory.createSshd();
        sshd.start();

        testNode = new NodeConfig(NodeConfig.NodeType.API, SshServerFactory.TEST_SSH_HOST, SshServerFactory.TEST_SSH_USER);
        testNode.setPort(sshd.getPort());
        testNode.setPrivateKeyFile(SshServerFactory.TEST_SSH_AUTH_PRIVATE_KEY);
    }

    @Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testInterruptWhenAddError() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        final String puppetErrorMessage = "2015-07-29 16:01:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n"
                                          + "2015-07-29 16:02:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n"
                                          + "2015-07-29 16:03:00 +0100 Puppet (err): Could not retrieve catalog from remote server: No route to host - connect(2)\n";

        doAnswer(invocationOnMock -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                failMessage[0] = "mockCommand should be interrupted by spyInterrupter, but wasn't";
                return null;
            } catch (InterruptedException e) {
                // it's okay here
                return null;
            }
        }).when(mockCommand).execute();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS / 2);
                FileUtils.writeStringToFile(spyInterrupter.getPuppetLogFile().toFile(), puppetErrorMessage, true);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });

        try {
            spyInterrupter.execute();
        } catch (Exception e) {
            assertEquals(e.getClass(), PuppetErrorException.class);

            String errorMessage = e.getMessage();

            Pattern errorMessagePattern = Pattern.compile(
                "Puppet error at the API node '127.0.0.1': 'Could not retrieve catalog from remote server: No route to host - connect[(]2[)]'. "
                + "At the time puppet is continue Codenvy installation in background and is trying to fix this issue. "
                + "Check administrator dashboard page http://localhost/admin to verify installation success [(]credentials: admin/password[)]. "
                + "If the installation eventually fails, contact support with error report target/reports/error_report_.*.tar.gz. "
                + "Installation & Troubleshooting Docs: http://docs.codenvy.com/onpremises/installation-multi-node/#install-troubleshooting.");

            assertTrue("Actual errorMessage: " + errorMessage, errorMessagePattern.matcher(errorMessage).find());

            assertErrorReport(errorMessage, logWithoutErrorMessages + puppetErrorMessage, testNode);
            return;
        }

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }

        fail("spyInterrupter.execute() should throw PuppetErrorException");
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
            TEST_TMP_DIRECTORY.resolve(testNode.getType().toString().toLowerCase()).resolve(spyInterrupter.getPuppetLogFile().getFileName());
        assertTrue(Files.exists(puppetLogFile));

        String logFileContent = FileUtils.readFileToString(puppetLogFile.toFile());
        assertEquals(logFileContent, expectedContentOfLogFile);
    }

    @Test(timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testNotInterruptWhenAddNoError() throws InterruptedException, IOException {
        final String[] failMessage = {null};
        final String expectedResult = "okay";

        doAnswer(invocationOnMock -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                return expectedResult;
            } catch (InterruptedException e) {
                failMessage[0] = "mockCommand should not be interrupted by spyInterrupter, but was.";
                return null;
            }
        }).when(mockCommand).execute();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS / 2);

                // append non-error message into puppet log file
                String errorMessage = "Jun  8 15:56:59 test puppet-agent[10240]: dummy message";

                FileUtils.write(spyInterrupter.getPuppetLogFile().toFile(), errorMessage, true);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });

        String result = spyInterrupter.execute();
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

        doAnswer(invocationOnMock -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                throw new CommandException("error");
            } catch (InterruptedException e) {
                failMessage[0] = "mockCommand should not be interrupted by spyInterrupter, but was.";
                return null;
            }
        }).when(mockCommand).execute();

        spyInterrupter.execute();

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }
    }

    @Test(expectedExceptions = RuntimeException.class,
          expectedExceptionsMessageRegExp = "error",
          timeOut = MOCK_COMMAND_TIMEOUT_MILLIS * 10)
    public void testRethrowRuntimeExceptionByInterrupter() throws InterruptedException, IOException {
        final String[] failMessage = {null};

        doAnswer(invocationOnMock -> {
            try {
                Thread.sleep(MOCK_COMMAND_TIMEOUT_MILLIS);
                throw new RuntimeException("error");
            } catch (InterruptedException e) {
                failMessage[0] = "mockCommand should not be interrupted by spyInterrupter, but was.";
                return null;
            }
        }).when(mockCommand).execute();

        spyInterrupter.execute();

        if (failMessage[0] != null) {
            fail(failMessage[0]);
        }
    }

    @Test(dataProvider = "dataForCheckPuppetError")
    public void testCheckPuppetError(String puppetLog, PuppetError expectedError) {
        List<String> lines = Arrays.asList(puppetLog.split("\n"));

        PuppetError error = null;
        for (String line : lines) {
            error = spyInterrupter.checkPuppetError(testNode, line);
        }

        assertEquals(error, expectedError);
    }

    @AfterMethod
    public void tearDown() throws InterruptedException, IOException {
        PuppetErrorReport.BASE_TMP_DIRECTORY = ORIGIN_BASE_TMP_DIRECTORY;

        deleteDirectory(BASE_TMP_DIRECTORY.toFile());
    }

    @Override
    public PuppetError getTestPuppetError() {
        return new PuppetError(testNode, "Dependency Package[openldap] has failures: true");
    }

    @AfterClass
    public void stopSshServers() throws InterruptedException {
        sshd.stop();
    }

    public InstallType getInstallType() {
        return InstallType.MULTI_SERVER;
    }

    public PuppetErrorInterrupter getSpyInterrupter() {
        return spy(new PuppetErrorInterrupter(mockCommand, Collections.singletonList(testNode), mockConfigManager));
    }
}
