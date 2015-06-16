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
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.commands.SimpleCommand;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.report.ErrorReport;
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
    public static Path PUPPET_LOG_FILE_PATH = Paths.get("/var/log/messages");
    protected static final int READ_LOG_TIMEOUT_MILLIS = 100;
    public static final int SELECTION_LINE_NUMBER = 5;

    private Thread monitorThread;
    private boolean stop;

    private String result;
    private CommandException commandException;
    private RuntimeException runtimeException;

    private final Command command;
    private final List<NodeConfig> nodes;

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
                lookingForPuppetError(null);

            } else {
                for (NodeConfig node : nodes) {
                    lookingForPuppetError(node);
                }
            }
        }
    }

    private void lookingForPuppetError(NodeConfig node) throws CommandException {
        List<String> lastLines;
        try {
            lastLines = readNLines(node);
        } catch (AgentException | CommandException e) {
            throw new RuntimeException(getRuntimeErrorMessage(node, e), e);
        }

        try {
            if (lastLines == null) {
                Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
            } else {
                for (String line : lastLines) {
                    if (checkPuppetError(line)) {
                        String errorMessage = getPuppetErrorMessage(node, line);

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

    protected boolean checkPuppetError(String line) {
        for (Pattern errorPattern : errorPatterns) {
            Matcher m = errorPattern.matcher(line);
            if (m.find()) {
                return true;
            }
        }

        return false;
    }

    private List<String> readNLines(NodeConfig node) throws CommandException, AgentException {
        Command readFileCommand = createReadFileCommand(node);

        String allLines = readFileCommand.execute();

        if (allLines == null) {
            Collections.singletonList(String.class);
        }

        String[] lines = allLines.split("[\n]+");
        return Arrays.asList(lines);
    }

    protected Command createReadFileCommand(NodeConfig node) throws AgentException {
        if (node == null) {
            return CommandLibrary.createTailCommand(PUPPET_LOG_FILE_PATH, SELECTION_LINE_NUMBER, true);
        } else {
            return CommandLibrary.createTailCommand(PUPPET_LOG_FILE_PATH, SELECTION_LINE_NUMBER, node, true);
        }
    }

    private String getPuppetErrorMessage(NodeConfig node, String line) {
        if (node == null) {
            String message = format("Puppet error: '%s'", line);

            Path errorReport = new ErrorReport().create();
            if (errorReport != null) {
                message += format(". Error report: %s", errorReport);
            }

            return message;
        }

        String message = format("Puppet error at the %s node '%s': '%s'", node.getType(), node.getHost(), line);
        Path errorReport = new ErrorReport().create(node);
        if (errorReport != null) {
            message += format(". Error report: %s", errorReport);
        }

        return message;
    }

    private String getRuntimeErrorMessage(NodeConfig node, IOException e) {
        if (node == null) {
            return format("It is impossible to read puppet log locally: %s", e.getMessage());
        }

        return format("It is impossible to read puppet log at the node '%s': %s", node.getHost(), e.getMessage());
    }

    @Override
    public String getDescription() {
        return "Puppet error interrupter";
    }

    @Override
    public String toString() {
        if (nodes == null) {
            return format("PuppetErrorInterrupter{ %s }; looking on errors in file %s locally", command.toString(), PUPPET_LOG_FILE_PATH);
        }

        return format("PuppetErrorInterrupter{ %s }; looking on errors in file %s of nodes: %s", command.toString(), PUPPET_LOG_FILE_PATH, nodes.toString());
    }
}
