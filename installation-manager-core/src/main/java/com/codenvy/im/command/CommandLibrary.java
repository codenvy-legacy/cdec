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

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

import static com.codenvy.im.command.SimpleCommand.createCommand;
import static com.codenvy.im.utils.Commons.getVersionsList;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

/** @author Dmytro Nochevnov */
public class CommandLibrary {

    private static final String STOP     = "stop";
    private static final String START    = "start";
    private static final String ACTIVE   = "active";
    private static final String INACTIVE = "inactive";

    private CommandLibrary() {
    }

    public static Command createPropertyReplaceCommand(String file, String property, String value) {
        String replacingToken = format("%s = .*", property);
        String replacement = format("%s = \"%s\"", property, value);
        return createReplaceCommand(file, replacingToken, replacement);
    }

    public static Command createReplaceCommand(String file, String replacingToken, String replacement) {
        return createCommand(format("sudo sed -i 's/%s/%s/g' %s",
                                    replacingToken.replace("/", "\\/"),
                                    replacement.replace("/", "\\/"),
                                    file));
    }

    public static Command createFileRestoreOrBackupCommand(final String file) {
        return createCommand(getFileRestoreOrBackupCommand(file));
    }

    public static Command createFileRestoreOrBackupCommand(final String file, List<NodeConfig> nodes) throws AgentException {
        return MacroCommand.createCommand(getFileRestoreOrBackupCommand(file), null, nodes);
    }

    public static Command createFileRestoreOrBackupCommand(final String file, NodeConfig node) throws AgentException {
        return MacroCommand.createCommand(getFileRestoreOrBackupCommand(file), null, ImmutableList.of(node));
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

    public static Command createFileBackupCommand(String file, NodeConfig node) throws AgentException {
        return createCommand(getFileBackupCommand(file), node);
    }

    public static Command createFileBackupCommand(final String file) {
        return createCommand(getFileBackupCommand(file));
    }

    protected static String getFileBackupCommand(final String file) {
        final String backupFile = file + ".back";
        return format("sudo cp %s %s",
                      file,
                      backupFile);
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
                commands.add(createCommand(format("sudo bash %s", patchFile)));
            }
        }

        return new MacroCommand(commands, "Patch resources");
    }

    public static Command createStopServiceCommand(String serviceName) {
        return createCommand(getServiceManagementCommand(serviceName, STOP));
    }

    public static Command createStopServiceCommand(String serviceName, NodeConfig node) throws AgentException {
        return createCommand(getServiceManagementCommand(serviceName, STOP), node);
    }

    public static Command createStartServiceCommand(String serviceName) {
        return createCommand(getServiceManagementCommand(serviceName, START));
    }

    public static Command createStartServiceCommand(String serviceName, NodeConfig node) throws AgentException {
        return createCommand(getServiceManagementCommand(serviceName, START), node);
    }

    protected static String getServiceManagementCommand(String serviceName, String action) {
        switch (action) {
            case STOP:
                return format("sudo service %1$s status | grep 'Active: active (running)'; "
                              + "if [ $? -eq 0 ]; then "
                              + "  sudo service %1$s stop; "
                              + "fi; ",
                              serviceName);

            case START:
                return format("sudo service %1$s status | grep 'Active: active (running)'; "
                              + "if [ $? -ne 0 ]; then "
                              + "  sudo service %1$s start; "
                              + "fi; ",
                              serviceName);
            default:
                throw new IllegalArgumentException(format("Service action '%s' isn't supported", action));
        }
    }

    /**
     * @return command "sudo tar -C {fromDir} -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     *
     * Don't gzip to be able to update pack in future.
     */
    public static Command createPackCommand(Path fromDir, Path packFile, String pathWithinThePack) {
        return createCommand(getPackCommand(fromDir, packFile, pathWithinThePack));
    }

    /**
     * @return for certain node, the command "sudo tar -C {fromDir} -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     *
     * Don't gzip to be able to update pack in future.
     */
    public static Command createPackCommand(Path fromDir, Path packFile, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createCommand(getPackCommand(fromDir, packFile, pathWithinThePack), node);
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

    public static Command createUnpackCommand(Path packFile, Path toDir, String pathWithinThePack) {
        return createCommand(getUnpackCommand(packFile, toDir, pathWithinThePack));
    }

    public static Command createUnpackCommand(Path packFile, Path toDir) {
        return createCommand(getUnpackCommand(packFile, toDir, null));
    }

    public static Command createUnpackCommand(Path packFile, Path toDir, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createCommand(getUnpackCommand(packFile, toDir, pathWithinThePack), node);
    }

    private static String getUnpackCommand(Path packFile, Path toDir, @Nullable String pathWithinThePack) {
        if (pathWithinThePack == null) {
            return format("sudo tar -xf %s -C %s",
                          packFile,
                          toDir);
        } else {
            return format("sudo tar -xf %s -C %s %s",
                          packFile,
                          toDir,
                          pathWithinThePack);
        }
    }

    public static Command createCopyFromRemoteToLocalCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String fromRemotePath = format("%s:%s", remote.getHost(), fromPath);
        return createCommand(getScpCommand(fromRemotePath, toPath.toString()));
    }

    public static Command createCopyFromLocalToRemoteCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String toRemotePath = format("%s:%s", remote.getHost(), toPath);
        return createCommand(getScpCommand(fromPath.toString(), toRemotePath));
    }

    private static String getScpCommand(String fromPath, String toPath) {
        return format("scp -r -q -o LogLevel=QUIET -o StrictHostKeyChecking=no %s %s", fromPath, toPath);
    }

    public static Command createWaitServiceActiveCommand(String service) {
        return createCommand(getWaitServiceStatusCommand(service, ACTIVE));
    }

    public static Command createWaitServiceActiveCommand(String service, NodeConfig node) throws AgentException {
        return createCommand(getWaitServiceStatusCommand(service, ACTIVE), node);
    }

    protected static String getWaitServiceStatusCommand(String service, String status) {
        String operator;
        switch (status) {
            case ACTIVE:
                operator = "-eq";
                break;

            case INACTIVE:
                operator = "-ne";
                break;

            default:
                throw new IllegalArgumentException(format("Service status '%s' isn't supported", status));
        }

        return format("doneState=\"Checking\"; " +
                      "while [ \"${doneState}\" != \"Done\" ]; do " +
                      "    sudo service %s status | grep 'Active: active (running)'; " +
                      "    if [ $? %s 0 ]; then doneState=\"Done\"; " +
                      "    else sleep 5; " +
                      "    fi; " +
                      "done", service, operator);
    }
}
