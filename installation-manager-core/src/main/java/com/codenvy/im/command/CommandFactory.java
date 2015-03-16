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
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

import static com.codenvy.im.command.SimpleCommand.createLocalAgentCommand;
import static com.codenvy.im.utils.Commons.getVersionsList;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

/** @author Dmytro Nochevnov */
public class CommandFactory {

    private static final String STOP = "stop";
    private static final String START = "start";

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

    public static Command createLocalAgentFileRestoreOrBackupCommand(final String file) {
        return createLocalAgentCommand(getFileRestoreOrBackupCommand(file));
    }

    public static Command createShellAgentFileRestoreOrBackupCommand(final String file, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createShellAgentCommand(getFileRestoreOrBackupCommand(file), null, nodes);
    }

    public static Command createShellAgentFileRestoreOrBackupCommand(final String file, NodeConfig node) throws AgentException {
        return MacroCommand.createShellAgentCommand(getFileRestoreOrBackupCommand(file), null, ImmutableList.of(node));
    }

    protected static String getFileRestoreOrBackupCommand(final String file) {
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

    public static Command createShellAgentFileBackupCommand(String file, NodeConfig node) throws AgentException {
        return createShellAgentCommand(getFileBackupCommand(file), node);
    }

    public static Command createLocalAgentFileBackupCommand(final String file) {
        return createLocalAgentCommand(getFileBackupCommand(file));
    }

    protected static String getFileBackupCommand(final String file) {
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


    public static Command createPatchCommand(Path patchDir, Version installedVersion, Version versionToUpdate) throws IOException {
        List<Command> commands;
        commands = new ArrayList<>();

        NavigableSet<Version> versions = getVersionsList(patchDir).subSet(installedVersion, false, versionToUpdate, true);
        Iterator<Version> iter = versions.iterator();
        while (iter.hasNext()) {
            Version v = iter.next();
            Path patchFile = patchDir.resolve(v.toString()).resolve("patch.sh");
            if (exists(patchFile)) {
                commands.add(createLocalAgentCommand(format("sudo bash %s", patchFile)));
            }
        }

        return new MacroCommand(commands, "Patch resources");
    }

    public static Command createLocalStopServiceCommand(String serviceName) {
        return createLocalAgentCommand(getServiceManagementCommand(serviceName, STOP));
    }

    public static Command createRemoteStopServiceCommand(String serviceName, NodeConfig node) throws AgentException {
        return createShellAgentCommand(getServiceManagementCommand(serviceName, STOP), node);
    }

    public static Command createLocalStartServiceCommand(String serviceName) {
        return createLocalAgentCommand(getServiceManagementCommand(serviceName, START));
    }

    public static Command createRemoteStartServiceCommand(String serviceName, NodeConfig node) throws AgentException {
        return createShellAgentCommand(getServiceManagementCommand(serviceName, START), node);
    }

    private static String getServiceManagementCommand(String serviceName, String action) {
        switch (action) {
            case STOP:
                return format("sudo service --status-all | grep %1$s; "
                              + "if [ $? -eq 0 ]; then "
                              + "  sudo service %1$s stop; "
                              + "fi; ",
                              serviceName);

            case START:
                return format("sudo service --status-all | grep %1$s; "
                              + "if [ $? -ne 0 ]; then "
                              + "  sudo service %1$s start; "
                              + "fi; ",
                              serviceName);
            default:
                return "";
        }
    }

    /**
     * @return command "sudo tar -C {fromDir} -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     *
     * Don't gzip to be able to update pack in future.
     */
    public static Command createLocalPackCommand(Path fromDir, Path packFile, String pathWithinThePack) {
        return createLocalAgentCommand(getPackCommand(fromDir, packFile, pathWithinThePack));
    }

    /**
     * @return for certain node, the command "sudo tar -C {fromDir} -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     *
     * Don't gzip to be able to update pack in future.
     */
    public static Command createRemotePackCommand(Path fromDir, Path packFile, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createShellAgentCommand(getPackCommand(fromDir, packFile, pathWithinThePack), node);
    }

    private static String getPackCommand(Path fromDir, Path packFile, String pathWithinThePack) {
        return format("if sudo test -f %2$s; then " +
                      "   sudo tar -C %1$s -rf %2$s %3$s;" +
                      "else " +
                      "   sudo tar -C %1$s -cf %2$s %3$s;" +
                      "fi;",
                      fromDir,
                      packFile,
                      pathWithinThePack);
    }

    public static Command createCopyFromRemoteToLocalCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String fromRemotePath = format("%s:%s", remote.getHost(), fromPath);
        return createLocalAgentCommand(getScpCommand(fromRemotePath, toPath.toString()));
    }

    public static Command createCopyFromLocalToRemoteCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String toRemotePath = format("%s:%s", remote.getHost(), toPath);
        return createLocalAgentCommand(getScpCommand(fromPath.toString(), toRemotePath));
    }

    private static String getScpCommand(String fromPath, String toPath) {
        return format("scp -r -q -o LogLevel=QUIET -o StrictHostKeyChecking=no %s %s", fromPath, toPath);
    }
}
