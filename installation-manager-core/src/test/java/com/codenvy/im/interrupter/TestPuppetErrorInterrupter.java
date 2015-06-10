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
package com.codenvy.im.interrupter;

import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.SimpleCommand;
import org.apache.commons.io.FileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestPuppetErrorInterrupter {
    public static final int WAIT_INTERRUPTER_MILLIS = PuppetErrorInterrupter.READ_LOG_TIMEOUT_MILLIS * 2;
    @Mock
    Interruptable testInterruptable;

    PuppetErrorInterrupter testInterrupter;

    Path   testPuppetLog           = Paths.get("target/test-classes/messages");
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

        testInterrupter = spy(new PuppetErrorInterrupter(testInterruptable));

        doReturn(testPuppetLog).when(testInterrupter).getPuppetLog();

        Command readPuppetLogCommandWithoutSudo = testInterrupter.getReadPuppetLogCommand(testPuppetLog, PuppetErrorInterrupter.SELECTION_LINE_NUMBER, false);
        doReturn(readPuppetLogCommandWithoutSudo).when(testInterrupter).getReadPuppetLogCommand(testPuppetLog, PuppetErrorInterrupter.SELECTION_LINE_NUMBER, true);

        // create log file
        FileUtils.write(testPuppetLog.toFile(), logWithoutErrorMessages);
    }

    @Test
    public void testGetReadPuppetLogCommand() {
        Command command = testInterrupter.getReadPuppetLogCommand(testPuppetLog, PuppetErrorInterrupter.SELECTION_LINE_NUMBER, false);
        assertEquals(command.toString(), "{'command'='tail -n 5 target/test-classes/messages', 'agent'='LocalAgent'}");

        command = testInterrupter.getReadPuppetLogCommand(testPuppetLog, PuppetErrorInterrupter.SELECTION_LINE_NUMBER + 1, true);
        assertEquals(command.toString(), "{'command'='sudo tail -n 6 target/test-classes/messages', 'agent'='LocalAgent'}");
    }

    @Test
    public void testInterruptWhenAddError() throws InterruptedException, IOException {
        testInterrupter.start();
        Thread.sleep(WAIT_INTERRUPTER_MILLIS);
        assertFalse(testInterrupter.hasInterrupted());
        assertNull(testInterrupter.getContext().getMessage());
        verify(testInterruptable, never()).interrupt(any(Context.class));

        // append error message into puppet log file
        String errorMessage = "Jun  8 15:56:59 test puppet-agent[10240]: Could not retrieve catalog from remote server: " +
                              "Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy";
        FileUtils.write(testPuppetLog.toFile(), errorMessage, true);
        Thread.sleep(WAIT_INTERRUPTER_MILLIS);

        assertTrue(testInterrupter.hasInterrupted());
        String expectedErrorMessage = format("Puppet error: '%s'", errorMessage);
        assertEquals(testInterrupter.getContext().getMessage(), expectedErrorMessage);

        PuppetErrorInterrupter.PuppetErrorContext expectedContext = new PuppetErrorInterrupter.PuppetErrorContext(expectedErrorMessage);
        verify(testInterruptable).interrupt(expectedContext);
    }

    @Test
    public void testNotInterruptWhenAddNoError() throws InterruptedException, IOException {
        testInterrupter.start();
        Thread.sleep(WAIT_INTERRUPTER_MILLIS);

        // append error message into puppet log file
        String errorMessage = "Jun  8 15:56:59 test puppet-agent[10240]: dummy message";
        FileUtils.write(testPuppetLog.toFile(), errorMessage, true);
        Thread.sleep(WAIT_INTERRUPTER_MILLIS);

        assertFalse(testInterrupter.hasInterrupted());
        assertNull(testInterrupter.getContext().getMessage());
        verify(testInterruptable, never()).interrupt(any(Context.class));
    }

    @AfterMethod
    public void tearDown() throws InterruptedException {
        testInterrupter.stop();
        Thread.sleep(WAIT_INTERRUPTER_MILLIS);
    }
}
