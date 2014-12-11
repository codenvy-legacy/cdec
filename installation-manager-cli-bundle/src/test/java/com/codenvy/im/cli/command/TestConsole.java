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

package com.codenvy.im.cli.command;

import org.fusesource.jansi.AnsiOutputStream;
import org.restlet.resource.ResourceException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/** @author Dmytro Nochevnov */
public class TestConsole {
    Console spyConsole;

    private InputStream inputStream;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;

    InputStream originIn  = System.in;
    PrintStream originOut = System.out;
    PrintStream originErr = System.out;

    @BeforeMethod
    public void setUp() throws Exception {
        this.outputStream = new ByteArrayOutputStream();
        this.errorStream = new ByteArrayOutputStream();

        System.setIn(this.inputStream);
        System.setOut(new PrintStream(this.outputStream));
        System.setErr(new PrintStream(this.errorStream));

        spyConsole = spy(new Console(true));
    }

    @AfterClass
    public void restoreSystemStreams() {
        System.setIn(originIn);
        System.setOut(originOut);
        System.setErr(originErr);
    }

    @Test
    public void testPrintErrorAndExit() throws Exception {
        spyConsole = spy(new Console(false));
        doNothing().when(spyConsole).exit(anyInt());  // avoid error "The forked VM terminated without properly saying goodbye. VM crash or System.exit called?"

        spyConsole.printErrorAndExit("error");
        assertEquals(removeAnsi(getOutputContent(true)), "[CODENVY] error\n");
        verify(spyConsole).exit(1);
    }

    @Test
    public void testPrintErrorAndExitIfNotInteractive() throws Exception {
        Exception exceptionWithConnectException = new ResourceException(new ConnectException());
        spyConsole.printErrorAndExit(exceptionWithConnectException);

        assertEquals(getOutputContent(true), "It is impossible to connect to Installation Manager Service. It might be stopped or it is starting up right now, "
                                         + "please retry a bit later.\n");
    }

    @Test
    public void testCheckConnectionException() {
        assertTrue(spyConsole.isConnectionException(new ResourceException(new ConnectException())));
        assertFalse(spyConsole.isConnectionException(new RuntimeException()));
    }

    @Test
    public void testPrintProgress() throws Exception {

    }

    @Test
    public void testPrintProgress1() throws Exception {

    }

    @Test
    public void testCleanCurrentLine() throws Exception {

    }

    @Test
    public void testCleanLineAbove() throws Exception {

    }

    @Test
    public void testPrintLn() throws Exception {

    }

    @Test
    public void testPrint() throws Exception {

    }

    @Test
    public void testPrintResponse() throws Exception {

    }

    @Test
    public void testPrintSuccess() throws Exception {

    }

    @Test
    public void testAskUser() throws Exception {

    }

    @Test
    public void testReadLine() throws Exception {

    }

    @Test
    public void testReadPassword() throws Exception {

    }

    @Test
    public void testPressAnyKey() throws Exception {

    }

    private String getOutputContent(boolean removeAnsi) {
        if (removeAnsi) {
            return removeAnsi(outputStream.toString());
        } else {
            return outputStream.toString();
        }
    }

    public void setSystemIn(String lines) {
        this.inputStream = new ByteArrayInputStream(lines.getBytes());
    }

    private String removeAnsi(final String content) {
        if (content == null) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AnsiOutputStream aos = new AnsiOutputStream(baos);
            aos.write(content.getBytes());
            aos.flush();
            return baos.toString();
        } catch (IOException e) {
            return content;
        }
    }
}
