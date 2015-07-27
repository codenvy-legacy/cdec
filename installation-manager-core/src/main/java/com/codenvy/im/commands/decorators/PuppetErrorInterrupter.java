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
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class PuppetErrorInterrupter implements Command {

    // TODO [ndp] Roman is going to change puppet to log into file '/var/log/puppet/puppet-agent.log'
    public static Path PUPPET_LOG_FILE = Paths.get("/var/log/messages");

    public static final int READ_LOG_TIMEOUT_MILLIS = 200;
    public static final int SELECTION_LINE_NUMBER   = 20;


    private final Command          command;
    private final List<NodeConfig> nodes;
    private final ConfigManager    configManager;

    private static final Logger LOG = Logger.getLogger(PuppetErrorInterrupter.class.getSimpleName());

    private FutureTask<String> task;

    protected static boolean useSudo = true;  // for testing propose

    public PuppetErrorInterrupter(Command command, ConfigManager configManager) {
        this(command, null, configManager);
    }

    public PuppetErrorInterrupter(Command command, List<NodeConfig> nodes, ConfigManager configManager) {
        this.command = command;
        this.nodes = nodes;
        this.configManager = configManager;
    }

    @Override
    public String execute() throws CommandException {
        LOG.info(toString());

        task = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return command.execute();
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(task);

        try {
            for (; ; ) {
                if (task.isDone()) {
                    try {
                        return task.get();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e.getMessage(), e);

                    } catch (ExecutionException e) {
                        Exception originException = (Exception)e.getCause();

                        if (originException instanceof CommandException) {
                            throw ((CommandException)originException);

                        } else if (originException instanceof RuntimeException) {
                            throw ((RuntimeException)originException);
                        }

                        throw new RuntimeException(originException.getMessage(), e);
                    }
                }

                listenToPuppetError();
            }
        } finally {
            executor.shutdown();
        }
    }

    private void listenToPuppetError() throws CommandException {
        if (nodes == null) {
            lookingForPuppetError(null);

        } else {
            for (NodeConfig node : nodes) {
                lookingForPuppetError(node);
            }
        }
    }

    private void lookingForPuppetError(NodeConfig node) throws CommandException {
        List<String> lastLines = null;
        try {
            lastLines = readNLines(node);
        } catch (AgentException | CommandException e) {
            LOG.log(Level.SEVERE, getRuntimeErrorMessage(node, e), e);    // ignore to don't interrupt installation process
        }

        try {
            if (lastLines == null) {
                Thread.sleep(READ_LOG_TIMEOUT_MILLIS);
            } else {
                for (String line : lastLines) {
                    PuppetError error = checkPuppetError(line);
                    if (error != null) {
                        task.cancel(false);

                        String errorMessage = getPuppetErrorMessageForLog(node, line);

                        LOG.log(Level.SEVERE, errorMessage);

                        Path errorReport = PuppetErrorReport.create(node);

                        String logMessage = error.getLineToDisplay(line);

                        throw new PuppetErrorException(getPuppetErrorMessageForOutput(node, logMessage, errorReport));
                    }
                }
            }
        } catch (PuppetErrorException pe) {
            throw pe;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    protected PuppetError checkPuppetError(String line) {
        for (PuppetError error : PuppetError.values()) {
            Boolean match = error.match(line);
            if (match != null && match) {
                return error;
            }
        }

        return null;
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

    /**
     * If node != null : create command to read from certain node of multi-node codenvy
     * If node == null : create command to read from single-node codenvy
     */
    protected Command createReadFileCommand(@Nullable NodeConfig node) throws AgentException {
        if (node == null) {
            return CommandLibrary.createTailCommand(PUPPET_LOG_FILE, SELECTION_LINE_NUMBER, useSudo);
        } else {
            return CommandLibrary.createTailCommand(PUPPET_LOG_FILE, SELECTION_LINE_NUMBER, node, useSudo);
        }
    }

    private String getPuppetErrorMessageForLog(@Nullable NodeConfig node, String line) {
        return (node == null) ? format("Puppet error: '%s'", line) :
               format("Puppet error at the %s node '%s': '%s'", node.getType(), node.getHost(), line);
    }


    private String getPuppetErrorMessageForOutput(@Nullable NodeConfig node, String lineOfLog, Path errorReport) throws IOException {
        String puppetErrorMessage = (node == null) ? format("Puppet error: '%s'", lineOfLog) :
                                    format("Puppet error at the %s node '%s': '%s'", node.getType(), node.getHost(), lineOfLog);

        Config codenvyConfig = configManager.loadInstalledCodenvyConfig();
        String hostUrl = codenvyConfig.getHostUrl();
        String systemAdminName = codenvyConfig.getValue(Config.ADMIN_LDAP_USER_NAME);
        char[] systemAdminPassword = codenvyConfig.getValue(Config.SYSTEM_LDAP_PASSWORD).toCharArray();
        InstallType installType = configManager.detectInstallationType();
        String docsUrlToken = installType == InstallType.SINGLE_SERVER ? "single" : "multi";

        return puppetErrorMessage + format(". At the time puppet is continue Codenvy installation in background and is trying to fix this issue."
                                           +
                                           " Check administrator dashboard page http://%s/admin to verify installation success (credentials: %s/%s)."
                                           + " If the installation eventually fails, contact support with error report %s."
                                           +
                                           " Installation & Troubleshooting Docs: http://docs.codenvy" +
                                           ".com/onpremises/installation-%s-node/#install-troubleshooting.",
                                           hostUrl,
                                           systemAdminName,
                                           String.valueOf(systemAdminPassword),
                                           errorReport.toString(),
                                           docsUrlToken
                                          );
    }

    private String getRuntimeErrorMessage(@Nullable NodeConfig node, IOException e) {
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
            return format("PuppetErrorInterrupter{ %s }; looking on errors in file %s locally", command.toString(), PUPPET_LOG_FILE);
        }

        return format("PuppetErrorInterrupter{ %s }; looking on errors in file %s of nodes: %s", command.toString(), PUPPET_LOG_FILE,
                      nodes.toString());
    }
}