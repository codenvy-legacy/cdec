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

import jline.console.ConsoleReader;

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.response.Response;

import org.fusesource.jansi.Ansi;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.response.Response.isError;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/** @author Dmytro Nochevnov */
public class Console {
    public static final Ansi   ERASE_LINE_ABOVE   = ansi().a(ansi().cursorUp(1).eraseLine(Ansi.Erase.ALL));
    public static final Ansi   ERASE_CURRENT_LINE = ansi().eraseLine(Ansi.Erase.ALL);
    public static final String CODENVY_PREFIX     = "[CODENVY] ";
    private static final Logger LOG               = Logger.getLogger(Console.class.getName());

    private final boolean       interactive;
    protected ConsoleReader consoleReader;

    Console(boolean interactive) throws IOException {
        this.interactive = interactive;
        this.consoleReader = new ConsoleReader();
    }

    void printError(String message) {
        printError(message, false);
    }

    void printError(String message, boolean suppressCodenvyPrompt) {
        print(ansi().fg(RED).a(message).newline().reset(), suppressCodenvyPrompt);
    }

    void printProgress(int percents) {
        printProgress(createProgressBar(percents));
    }

    void printProgress(String message) {
        System.out.print(ansi().saveCursorPosition().a(message).restorCursorPosition());
        System.out.flush();
    }

    void cleanCurrentLine() {
        System.out.print(ERASE_CURRENT_LINE);
        System.out.flush();
    }

    void cleanLineAbove() {
        System.out.print(ERASE_LINE_ABOVE);
        System.out.flush();
    }

    void println(String message) {
        print(message);
        System.out.println();
    }

    void print(String message) {
        print((Object)message, false);
    }

    void print(String message, boolean suppressCodenvyPrompt) {
        print((Object)message, suppressCodenvyPrompt);
    }

    void printSuccess(String message) {
        printSuccess(message, false);
    }

    void printSuccess(String message, boolean suppressCodenvyPrompt) {
        print(ansi().fg(GREEN).a(message).newline().reset(), suppressCodenvyPrompt);
    }

    /** @return "true" only if only user typed line equals "y". */
    boolean askUser(String prompt) throws IOException {
        print(prompt + " [y/N] ");
        String userAnswer = readLine();
        return userAnswer != null && userAnswer.equalsIgnoreCase("y");
    }

    /** @return line typed by user */
    String readLine() throws IOException {
        return doReadLine(null);
    }

    String readPassword() throws IOException {
        return doReadLine('*');
    }

    void pressAnyKey(String prompt) throws IOException {
        print(prompt);
        consoleReader.readCharacter();
    }

    void printErrorAndExit(Exception ex) {
        String errorMessage;

        if (isConnectionException(ex)) {
            errorMessage = "It is impossible to connect to Installation Manager Service. It might be stopped or it is starting up right now, " +
                           "please retry a bit later.";
        } else {
            errorMessage = Response.valueOf(ex).toJson();
        }

        LOG.log(Level.SEVERE, ex.getMessage(), ex);

        printError(errorMessage);

        if (!interactive) {
            exit(1);
        }
    }

    void printResponse(String response) throws JsonParseException {
        if (isError(response)) {
            printErrorAndExit(response);
        } else {
            println(response);
        }
    }

    /**
     * Print error message and exit with status = 1 if the command is not executing in interactive mode.
     */
    void printErrorAndExit(String message) {
        LOG.log(Level.SEVERE, message);

        printError(message);

        if (!interactive) {
            exit(1);
        }
    }

    protected void exit(int status) {
        System.exit(status);
    }

    private String createProgressBar(int percent) {
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                bar.append("=");
            } else if (i == (percent / 2)) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }

        bar.append("]   ").append(percent).append("%     ");
        return bar.toString();
    }

    private void print(Object o, boolean suppressCodenvyPrompt) {
        if (!interactive && !suppressCodenvyPrompt) {
            printCodenvyPrompt();
        }

        System.out.print(o);
        System.out.flush();
    }

    private void printCodenvyPrompt() {
        final String lightBlue = '\u001b' + "[94m";
        System.out.print(ansi().a(lightBlue + CODENVY_PREFIX).reset()); // light blue
    }

    private boolean isConnectionException(Exception e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getClass().getCanonicalName().equals(ConnectException.class.getCanonicalName());
    }

    private String doReadLine(@Nullable Character mask) throws IOException {
        return consoleReader.readLine(mask);
    }
}
