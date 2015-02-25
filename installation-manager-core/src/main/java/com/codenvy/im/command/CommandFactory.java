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
package com.codenvy.im.command;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.command.SimpleCommand.createLocalAgentCommand;
import static com.codenvy.im.utils.Commons.combinePaths;
import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newInputStream;

/** @author Dmytro Nochevnov */
public class CommandFactory {
    private CommandFactory() {
    }

    public static Command createLocalAgentPropertyReplaceCommand(String file, String property, String value) {
        String replacingToken = format("%s = .*", property);
        String replacement = format("%s = \"%s\"", property, value);
        return createLocalAgentReplaceCommand(file, replacingToken, replacement);
    }

    public static Command createLocalAgentReplaceCommand(String file, String replacingToken, String replacement) {
        return createLocalAgentCommand(format("sudo sed -i 's/%s/%s/g' %s",
                                              replacingToken.replace("/", "\\/"),
                                              replacement.replace("/", "\\/"),
                                              file));
    }

    public static Command createLocalAgentRestoreOrBackupCommand(final String file) {
        return createLocalAgentCommand(getRestoreOrBackupCommand(file));
    }

    public static Command createShellAgentRestoreOrBackupCommand(final String file, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createShellAgentCommand(getRestoreOrBackupCommand(file), null, nodes);
    }

    public static Command createShellAgentRestoreOrBackupCommand(final String file, NodeConfig node) throws AgentException {
        return MacroCommand.createShellAgentCommand(getRestoreOrBackupCommand(file), null, ImmutableList.of(node));
    }

    protected static String getRestoreOrBackupCommand(final String file) {
        final String backupFile = file + ".back";
        return format("if sudo test -f %2$s; then " +
                      "    if ! sudo test -f %1$s; then " +
                      "        sudo cp %2$s %1$s; " +
                      "    else " +
                      "        sudo cp %1$s %2$s; " +
                      "    fi " +
                      "fi",
                      backupFile,
                      file);
    }

    public static Command createShellAgentBackupCommand(String file, NodeConfig node) throws AgentException {
        return createShellAgentCommand(getBackupCommand(file), node);
    }

    public static Command createLocalAgentBackupCommand(final String file) {
        return createLocalAgentCommand(getBackupCommand(file));
    }

    protected static String getBackupCommand(final String file) {
        final String backupFile = file + ".back";
        return format("sudo cp %s %s",
                      file,
                      backupFile);
    }

    public static Command createShellAgentCommand(String command, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createShellAgentCommand(command, null, nodes);
    }

    public static Command createShellAgentCommand(String command, NodeConfig node) throws AgentException {
        return createShellAgentCommand(command, ImmutableList.of(node));
    }
}
