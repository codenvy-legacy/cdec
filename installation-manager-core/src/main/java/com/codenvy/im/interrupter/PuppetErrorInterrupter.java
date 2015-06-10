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
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class PuppetErrorInterrupter implements Interrupter {
    private static final Path PUPPET_LOG_FILE_PATH = Paths.get("/var/log/messages");
    protected static final int READ_LOG_TIMEOUT_MILLIS = 100;
    public static final int SELECTION_LINE_NUMBER = 5;

    private boolean interruptHappened;
    private boolean stop;
    private Context context;
    private Thread  monitorThread;

    private final Interruptable interruptable;

    private List<Pattern> errorPatterns = ImmutableList.of(
        Pattern.compile("puppet-agent[^:]*: Could not retrieve catalog from remote server")
    );


    public PuppetErrorInterrupter(Interruptable interruptable) {
        this.interruptable = interruptable;
    }

    public void start() {
        interruptHappened = false;
        stop = false;
        context = new NullContext();

        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Path puppetLog = getPuppetLog();
                for (; ; ) {
                    if (stop) {
                        return;
                    }

                    try {
                        List<String> lastLines = readNLines(puppetLog, SELECTION_LINE_NUMBER);

                        if (lastLines == null) {
                            Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
                        } else {
                            for (String line : lastLines) {
                                if (checkPuppetError(line)) {
                                    String errorMessage = format("Puppet error: '%s'", line);
                                    context = new PuppetErrorContext(errorMessage);
                                    interruptHappened = true;
                                    interruptable.interrupt(context);
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
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

    private List<String> readNLines(Path file, int lineNumber) throws IOException {
        Command readFileCommand = getReadPuppetLogCommand(file, lineNumber, true);
        String allLines = readFileCommand.execute();

        if (allLines == null) {
            Collections.singletonList(String.class);
        }

        String[] lines = allLines.split("[\n]+");
        return Arrays.asList(lines);
    }

    protected Command getReadPuppetLogCommand(Path file, int lineNumber, boolean needSudo) {
        return SimpleCommand.createCommand(getReadPuppetBashCommand(file, lineNumber, needSudo));
    }

    private String getReadPuppetBashCommand(Path file, int lineNumber, boolean needSudo) {
        String command = format("tail -n %s %s", lineNumber, file);
        if (needSudo) {
            command = "sudo " + command;
        }

        return command;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public boolean hasInterrupted() {
        return interruptHappened;
    }

    @Override
    public Context getContext() {
        return context;
    }

    static class PuppetErrorContext implements Context {
        private final String message;

        PuppetErrorContext(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PuppetErrorContext that = (PuppetErrorContext)o;

            if (message != null ? !message.equals(that.message) : that.message != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return message != null ? message.hashCode() : 0;
        }
    }
}
