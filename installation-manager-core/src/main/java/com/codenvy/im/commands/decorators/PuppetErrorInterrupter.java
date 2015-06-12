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

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.commands.SimpleCommand;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableList;

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
    private final List<NodeConfig> nodes;

    private Path puppetLog;

    private List<Pattern> errorPatterns = ImmutableList.of(
        Pattern.compile("puppet-agent[^:]*: Could not retrieve catalog from remote server")
    );

    private static final Logger LOG = Logger.getLogger(SimpleCommand.class.getSimpleName());

    public PuppetErrorInterrupter(Command command) {
        this(command, null);
    }

    public PuppetErrorInterrupter(Command command, List<NodeConfig> nodes) {
        this.command = command;
        this.nodes = nodes;
    }

    @Override
    public String execute() throws CommandException {
        LOG.info(toString());

        stop = false;
        result = null;
        commandException = null;
        puppetLog = getPuppetLog();

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
        for (; ; ) {
            if (stop) {
                return;
            }

            if (nodes == null) {
                listenToLocalPuppetError();

            } else {
                for (NodeConfig node : nodes) {
                    listenToRemotePuppetError(node);
                }
            }
        }
    }

    private void listenToRemotePuppetError(NodeConfig node) throws CommandException {
        List<String> lastLines;
        try {
            lastLines = readNLinesRemotely(node);
        } catch (AgentException | CommandException e) {
            throw new RuntimeException(format("It is impossible to read puppet log at the node '%s': %s", node.getHost(), e.getMessage()), e);
        }

        try {
            if (lastLines == null) {
                Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
            } else {
                for (String line : lastLines) {
                    if (checkPuppetError(line)) {
                        String errorMessage = format("Puppet error at the %s node '%s': '%s'", node.getType(), node.getHost(), line);

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

    private List<String> readNLinesRemotely(NodeConfig node) throws CommandException, AgentException {
        Command readFileCommand = createReadFileCommand(puppetLog, SELECTION_LINE_NUMBER, node, true);
        String allLines = readFileCommand.execute();

        if (allLines == null) {
            Collections.singletonList(String.class);
        }

        String[] lines = allLines.split("[\n]+");
        return Arrays.asList(lines);
    }

    private void listenToLocalPuppetError() throws CommandException {
        List<String> lastLines;
        try {
            lastLines = readNLinesLocally();
        } catch (CommandException e) {
            throw new RuntimeException(format("It is impossible to read puppet log locally: %s", e.getMessage()), e);
        }

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

    protected Path getPuppetLog() {
        return PUPPET_LOG_FILE_PATH;
    }

    protected boolean checkPuppetError(String line) {
        for (Pattern errorPattern : errorPatterns) {
            Matcher m = errorPattern.matcher(line);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    private List<String> readNLinesLocally() throws CommandException {
        Command readFileCommand = createReadFileCommand(puppetLog, SELECTION_LINE_NUMBER, true);
        String allLines = readFileCommand.execute();

        if (allLines == null) {
            return Collections.emptyList();
        }

        String[] lines = allLines.split("[\n]+");
        return Arrays.asList(lines);
    }

    protected Command createReadFileCommand(Path file, int lineNumber, boolean needSudo) {
        return SimpleCommand.createCommandWithoutLogging(getReadFileCommand(file, lineNumber, needSudo));
    }

    protected Command createReadFileCommand(Path file, int lineNumber, NodeConfig node, boolean needSudo) throws AgentException {
        return SimpleCommand.createCommandWithoutLogging(getReadFileCommand(file, lineNumber, needSudo), node);
    }

    private String getReadFileCommand(Path file, int lineNumber, boolean needSudo) {
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
