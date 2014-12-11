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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.response.Response;
import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;

import static com.codenvy.im.response.Response.isError;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

/** @author Dmytro Nochevnov */
public class Console {
    private final boolean interactive;

    public Console(boolean interactive) {
        this.interactive = interactive;
    }

    void printError(String message) {
        print(ansi().fg(RED).a(message).newline().reset(), false);
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

    void cleanCurrentLine() {
        System.out.print(ansi().eraseLine(Ansi.Erase.ALL));
        System.out.flush();
    }

    void cleanLineAbove() {
        System.out.print(ansi().a(ansi().cursorUp(1).eraseLine(Ansi.Erase.ALL)));
        System.out.flush();
    }

    void printLn(String message) {
        print(message);
        System.out.println();
    }

    void print(String message, boolean suppressCodenvyPrompt) {
        if (!interactive && !suppressCodenvyPrompt) {
            printCodenvyPrompt();
        }
        System.out.print(message);
        System.out.flush();
    }

    void print(String message) {
        if (!interactive) {
            printCodenvyPrompt();
        }
        System.out.print(message);
        System.out.flush();
    }

    private void print(Ansi ansi, boolean suppressCodenvyPrompt) {
        if (!interactive && !suppressCodenvyPrompt) {
            printCodenvyPrompt();
        }
        System.out.print(ansi);
        System.out.flush();
    }

    private void printCodenvyPrompt() {
        final String lightBlue = '\u001b' + "[94m";
        System.out.print(ansi().a(lightBlue + "[CODENVY] ").reset()); // light blue
    }

    void printSuccess(String message, boolean suppressCodenvyPrompt) {
        print(ansi().fg(GREEN).a(message).newline().reset(), suppressCodenvyPrompt);
    }

    void printSuccess(String message) {
        print(ansi().fg(GREEN).a(message).newline().reset(), false);
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

    private String doReadLine(@Nullable Character mask) throws IOException {
        return new ConsoleReader().readLine(mask);
    }

    void pressAnyKey(String prompt) throws IOException {
        print(prompt);
        new ConsoleReader().readCharacter();
    }

    void printError(Exception ex) {
        if (isConnectionException(ex)) {
            printError("It is impossible to connect to Installation Manager Service. It might be stopped or it is starting up right now, " +
                       "please retry a bit later.");
        } else {
            printError(Response.valueOf(ex).toJson());
        }
    }

    protected boolean isConnectionException(Exception e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getClass().getCanonicalName().equals(ConnectException.class.getCanonicalName());
    }

    protected void printResponse(String response, AbstractIMCommand abstractIMCommand) throws JsonParseException {
        if (isError(response)) {
            printErrorEndExit(response, abstractIMCommand);
        } else {
            printLn(response);
        }
    }

    /**
     * Print error message and exit with status = 1 if the command is executing in interactive mode.
     */
    protected void printErrorEndExit(String message, AbstractIMCommand abstractIMCommand) {
        printError(message);

        if (!abstractIMCommand.isInteractive()) {
            exit(1);
        }
    }

    protected void exit(int status) {
        System.exit(status);
    }
}
