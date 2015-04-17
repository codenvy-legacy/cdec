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
package com.codenvy.im.agent;

import com.codenvy.im.console.Console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public class LocalAgent extends AbstractAgent {
    private static final Logger LOG = Logger.getLogger(LocalAgent.class.getSimpleName());

    protected final static String READ_PASSWORD_PROMPT              = String.format("[sudo] password for %s: ", System.getProperty("user.name"));
    protected final static String NEED_PASSWORD_STATUS              = "need_password_status";
    protected static final String CHECK_PASSWORD_NECESSITY_COMMAND =
            String.format("if ! sudo -n true 2>/dev/null; then echo \"%s\"; fi", NEED_PASSWORD_STATUS);
    protected static final String CHECK_IS_PASSWORD_CORRECT_COMMAND = "sudo -S true";
    protected static final String PASSWORD_INCORRECT_STATUS         = "Sorry, try again";

    private static char[] pwdCache = null;

    public LocalAgent() {
    }

    /** {@inheritDoc} */
    @Override
    public String execute(String command) throws AgentException {
        if (isPasswordInputRequired(command)) {
            return executeWithPassword(command);
        } else {
            return executeWithoutPassword(command);
        }
    }

    private String executeWithPassword(String command) throws AgentException {
        if (pwdCache == null) {
            pwdCache = obtainPassword();
        }

        try {
            String commandWhichIsReadPasswordFromInput =
                    command.replace("sudo ", "sudo -S ");  // make sudo be able to read password from input stream
            Process process = getProcess(commandWhichIsReadPasswordFromInput);
            passPasswordToProcess(process, pwdCache);
            int exitStatus = process.waitFor();

            InputStream in = process.getInputStream();
            InputStream err = process.getErrorStream();

            return processOutput(exitStatus, in, err);
        } catch (Exception e) {
            String errMessage = String.format("Can't execute command '%s'.", command);
            throw makeAgentException(errMessage, e);
        }
    }

    private String executeWithoutPassword(String command) throws AgentException {
        try {
            Process process = getProcess(command);

            int exitStatus = process.waitFor();
            InputStream in = process.getInputStream();
            InputStream err = process.getErrorStream();

            return processOutput(exitStatus, in, err);
        } catch (Exception e) {
            String errMessage = String.format("Can't execute command '%s'.", command);
            throw makeAgentException(errMessage, e);
        }
    }

    protected void passPasswordToProcess(Process process, char[] pwd) {
        final PrintWriter stdIn = new PrintWriter(process.getOutputStream());

        for (char ch : pwd) {
            stdIn.append(ch);
        }

        stdIn.append('\n');
        stdIn.flush();
    }

    protected boolean isPasswordInputRequired(String command) {
        if (!command.contains("sudo ")) {
            return false;
        }

        if (!isConsoleAccessible()) {
            return false;
        }

        try {
            String result = executeWithoutPassword(CHECK_PASSWORD_NECESSITY_COMMAND);
            return (result != null) && result.contains(NEED_PASSWORD_STATUS);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Can't verify requirement of sudo password", e);
            return false;
        }
    }

    protected char[] obtainPassword() throws AgentException, IllegalStateException {
        Console console = getConsole();

        try {
            console.hideProgressor();
            console.println();

            for (int i = 0; i < 3; i++) {
                console.print(READ_PASSWORD_PROMPT);
                Object answer = console.readPassword();
                if (answer == null) {
                    console.println("Sorry, try again.");
                    continue;
                }

                char[] answerChar = ((String)answer).toCharArray();
                if (isPasswordCorrect(answerChar)) {
                    console.restoreProgressor();
                    return answerChar;
                }

                console.println("Sorry, try again.");
            }

            console.println("sudo: 3 incorrect password attempts");
            throw new RuntimeException();
        } catch (Exception e) {
            throw new AgentException("Can't obtain correct password", e);
        } finally {
            console.showProgressor();
        }
    }

    protected Process getProcess(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        return pb.start();
    }

    protected Console getConsole() throws AgentException {
        Console console;
        try {
            console = Console.getInstance();
        } catch (Exception e) {
            throw new AgentException("Can't obtain correct password", e);
        }
        return console;
    }

    protected boolean isPasswordCorrect(char[] checkingPwd) throws Exception {
        Process process = getProcess(CHECK_IS_PASSWORD_CORRECT_COMMAND);
        passPasswordToProcess(process, checkingPwd);
        InputStream errorStream = process.getErrorStream();

        // Don't use process.waitFor() because the process is hang up in case of incorrect password.
        // Read error stream using errorStream.read(), because IOUtils.toString(bufferedReader) hang up in case of incorrect password.
        String errorMessage = "";
        for (; ; ) {
            int c = errorStream.read();
            if (c == -1) {    // check end of stream and exit here if there is no login error
                return true;
            }

            errorMessage += (char)c;
            if (errorMessage.contains(PASSWORD_INCORRECT_STATUS)) {
                process.destroy();
                return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * @return true if org/fusesource/jansi/Ansi is found, or false otherwise
     */
    protected boolean isConsoleAccessible() {
        try {
            getConsole();
        } catch(AgentException e) {
            return false;
        }

        return true;
    }
}
