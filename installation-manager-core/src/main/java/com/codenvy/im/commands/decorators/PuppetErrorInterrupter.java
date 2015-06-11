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
import com.codenvy.im.commands.SimpleCommand;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class PuppetErrorInterrupter implements Command {
    private static final Path PUPPET_LOG_FILE_PATH = Paths.get("/var/log/messages");
    protected static final int READ_LOG_TIMEOUT_MILLIS = 100;
    public static final int SELECTION_LINE_NUMBER = 5;

    private Thread monitorThread;
    private boolean stop;

    private String result;
    private CommandException commandException;
    private RuntimeException runtimeException;

    private final Command command;

    private List<Pattern> errorPatterns = ImmutableList.of(
        Pattern.compile("puppet-agent[^:]*: Could not retrieve catalog from remote server")
    );

    private static final Logger LOG = Logger.getLogger(SimpleCommand.class.getSimpleName());

    public PuppetErrorInterrupter(Command command) {
        this.command = command;
    }

    @Override
    public String execute() throws CommandException {
        LOG.info(toString());

        stop = false;
        result = null;
        commandException = null;

        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result = command.execute();
                } catch (CommandException ce) {
                    commandException = ce;
                } catch (RuntimeException re) {
                    runtimeException = re;
                } finally {
                    stop = true;
                }
            }
        });

        monitorThread.start();

        listenToPuppetError();

        if (commandException != null) {
            throw commandException;

        } else if (runtimeException != null) {
            throw runtimeException;
        }

        return result;
    }

    private void listenToPuppetError() throws CommandException {
        Path puppetLog = getPuppetLog();
        for (; ; ) {
            if (stop) {
                return;
            }

            List<String> lastLines = readNLines(puppetLog, SELECTION_LINE_NUMBER);

            try {
                if (lastLines == null) {
                    Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
                } else {
                    for (String line : lastLines) {
                        if (checkPuppetError(line)) {
                            String errorMessage = format("Puppet error: '%s'", line);

                            monitorThread.interrupt();
                            monitorThread.join();

                            throw new CommandException(errorMessage);
                        }
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected Path getPuppetLog() {
        return PUPPET_LOG_FILE_PATH;
    }

    private boolean checkPuppetError(String line) {
        for (Pattern errorPattern : errorPatterns) {
            Matcher m = errorPattern.matcher(line);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    private List<String> readNLines(Path file, int lineNumber) throws CommandException {
        Command readFileCommand = getReadPuppetLogCommand(file, lineNumber, true);
        String allLines = readFileCommand.execute();

        if (allLines == null) {
            Collections.singletonList(String.class);
        }

        String[] lines = allLines.split("[\n]+");
        return Arrays.asList(lines);
    }

    protected Command getReadPuppetLogCommand(Path file, int lineNumber, boolean needSudo) {
        return SimpleCommand.createCommandWithoutLogging(getReadPuppetBashCommand(file, lineNumber, needSudo));
    }

    private String getReadPuppetBashCommand(Path file, int lineNumber, boolean needSudo) {
        String command = format("tail -n %s %s", lineNumber, file);
        if (needSudo) {
            command = "sudo " + command;
        }

        return command;
    }

    @Override
    public String getDescription() {
        return "Puppet error interrupter";
    }

    @Override
    public String toString() {
        return format("PuppetErrorInterrupter{ %s }", command.toString());
    }
}
