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
package com.codenvy.im.commands;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static java.lang.String.format;
import static java.nio.file.Files.exists;

/** @author Dmytro Nochevnov */
public class CommandLibrary {

    private static final String STOP     = "stop";
    private static final String START    = "start";
    private static final String ACTIVE   = "active";
    private static final String INACTIVE = "inactive";

    public enum PatchType {
        BEFORE_UPDATE, AFTER_UPDATE
    }

    private CommandLibrary() {
    }

    public static Command createPropertyReplaceCommand(String file, String property, String value) {
        String replacingToken = format("%s = .*", property);
        String replacement = format("%s = \"%s\"", property, value);
        return createReplaceCommand(file, replacingToken, replacement);
    }

    public static Command createReplaceCommand(String file, String replacingToken, String replacement) {
        return createCommand(format("sudo sed -i 's|%s|%s|g' %s",
                                    replacingToken.replace("\n", "\\n"),
                                    replacement.replace("\n", "\\n"),
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
        return format("sudo cp %1$s %1$s.back ; " +
                      "sudo cp %1$s %1$s.back.%2$s ; ",
                      file,
                      new Date().getTime());
    }

    public static Command createPatchCommand(Path patchDir, PatchType patchType, InstallOptions installOptions) throws IOException {
        List<Command> commands = new ArrayList<>();

        Path relativePatchFilePath = getRelativePatchFilePath(patchType, installOptions.getInstallType());
        Path patchFile = patchDir.resolve(relativePatchFilePath);
        if (exists(patchFile)) {
            for (Map.Entry<String, String> e : installOptions.getConfigProperties().entrySet()) {
                String property = e.getKey();
                String value = e.getValue();

                commands.add(createReplaceCommand(patchFile.toString(), "$" + property, value));
            }
            commands.add(createCommand(format("bash %s", patchFile)));
        }

        return new MacroCommand(commands, "Patch resources");
    }

    /**
     * Return relative path to patch file, for example:  "single_server/patch_before_update.sh"
     */
    private static Path getRelativePatchFilePath(PatchType patchType, InstallType installType) {
        String pathFilename = format("patch_%s.sh", patchType.toString().toLowerCase());
        return Paths.get(installType.toString().toLowerCase())
                    .resolve(pathFilename);
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
    public static Command createPackCommand(Path fromDir, Path packFile, String pathWithinThePack, boolean needSudo) {
        return createCommand(getPackCommand(fromDir, packFile, pathWithinThePack, needSudo, false));
    }

    /**
     * @return for certain node, the command "sudo tar -C {fromDir} -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     *
     * Don't gzip to be able to update pack in future.
     */
    public static Command createPackCommand(Path fromDir, Path packFile, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createCommand(getPackCommand(fromDir, packFile, pathWithinThePack, true, false), node);
    }

    /**
     * @return for certain node, the command "sudo tar -C {fromDir} -z -rf {packFile} {pathWithinThePack}", if packFile exists, or
     * command "sudo tar -C {fromDir} -z -cf {packFile} {pathWithinThePack}", if packFile doesn't exists.
     */
    public static Command createCompressCommand(Path fromDir, Path packFile, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createCommand(getPackCommand(fromDir, packFile, pathWithinThePack, true, true), node);
    }

    private static String getPackCommand(Path fromDir, Path packFile, String pathWithinThePack, boolean needSudo, boolean useCompression) {
        String compressionOption = useCompression ? "-z" : "";

        if (needSudo) {
            return format("if sudo test -f %2$s; then " +
                          "   sudo tar -C %1$s %4$s -rf %2$s %3$s;" +
                          "else " +
                          "   sudo tar -C %1$s %4$s -cf %2$s %3$s;" +
                          "fi;",
                          fromDir,
                          packFile,
                          pathWithinThePack,
                          compressionOption);
        } else {
            return format("if test -f %2$s; then " +
                          "   tar -C %1$s %4$s -rf %2$s %3$s;" +
                          "else " +
                          "   tar -C %1$s %4$s -cf %2$s %3$s;" +
                          "fi;",
                          fromDir,
                          packFile,
                          pathWithinThePack,
                          compressionOption);
        }
    }

    public static Command createUnpackCommand(Path packFile, Path toDir, String pathWithinThePack, boolean needSudo) {
        return createCommand(getUnpackCommand(packFile, toDir, pathWithinThePack, needSudo, false));
    }

    public static Command createUnpackCommand(Path packFile, Path toDir, String pathWithinThePack) {
        return createCommand(getUnpackCommand(packFile, toDir, pathWithinThePack, false, false));
    }

    public static Command createUnpackCommand(Path packFile, Path toDir) {
        return createCommand(getUnpackCommand(packFile, toDir, null, false, false));
    }

    public static Command createUncompressCommand(Path packFile, Path toDir) {
        return createCommand(getUnpackCommand(packFile, toDir, null, false, true));
    }

    public static Command createUnpackCommand(Path packFile, Path toDir, String pathWithinThePack, NodeConfig node) throws AgentException {
        return createCommand(getUnpackCommand(packFile, toDir, pathWithinThePack, true, false), node);
    }

    private static String getUnpackCommand(Path packFile, Path toDir, @Nullable String pathWithinThePack, boolean needSudo, boolean useCompression) {
        String command;
        String compressionOptions = useCompression ? "-z" : "";

        if (pathWithinThePack == null) {
            command = format("tar %s -xf %s -C %s",
                             compressionOptions,
                             packFile,
                             toDir);
        } else {
            command = format("tar %s -xf %s -C %s %s",
                             compressionOptions,
                             packFile,
                             toDir,
                             pathWithinThePack);
        }

        if (needSudo) {
            command = "sudo " + command;
        }

        return command;
    }

    public static Command createCopyFromRemoteToLocalCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String userNamePrefix = "";
        if (remote.getUser() != null
            && !remote.getUser().isEmpty()) {
            userNamePrefix = format("%s@", remote.getUser());
        }

        String fromRemotePath = format("%s%s:%s", userNamePrefix, remote.getHost(), fromPath);
        return createCommand(getScpCommand(fromRemotePath, toPath.toString()));
    }

    public static Command createCopyFromLocalToRemoteCommand(Path fromPath, Path toPath, NodeConfig remote) {
        String userNamePrefix = "";
        if (remote.getUser() != null
            && !remote.getUser().isEmpty()) {
            userNamePrefix = format("%s@", remote.getUser());
        }

        String toRemotePath = format("%s%s:%s", userNamePrefix, remote.getHost(), toPath);
        return createCommand(getScpCommand(fromPath.toString(), toRemotePath));
    }

    private static String getScpCommand(String fromPath, String toPath) {
        return format("scp -r -q -o StrictHostKeyChecking=no %s %s", fromPath, toPath);
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

    /**
     * Creates command to force running puppet agent if no lock file exists.
     */
    public static Command createForcePuppetAgentCommand(NodeConfig node) throws AgentException {
        return createCommand(getForcePuppetAgentCommand(), node);
    }


    /**
     * Creates command to force running puppet agent if no lock file exists.
     */
    public static Command createForcePuppetAgentCommand() throws AgentException {
        return createCommand(getForcePuppetAgentCommand());
    }

    /**
     * Creates command to force running puppet agent if no lock file exists.
     * Lock file is placed at /var/lib/puppet/state/agent_catalog_run.lock
     */
    private static String getForcePuppetAgentCommand() {
        return "if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then " +
               "   sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; " +  // make sure there is no "--detailed-exitcodes" option
               "fi;";
    }

    public static Command createReadFileCommand(Path file, int lineNumber, boolean needSudo) {
        return SimpleCommand.createCommandWithoutLogging(getReadFileCommand(file, lineNumber, needSudo));
    }

    public static Command createReadFileCommand(Path file, int lineNumber, NodeConfig node, boolean needSudo) throws AgentException {
//        return SimpleCommand.createCommandWithoutLogging(getReadFileCommand(file, lineNumber, needSudo), node);
        return SimpleCommand.createCommand(getReadFileCommand(file, lineNumber, needSudo), node);
    }

    private static String getReadFileCommand(Path file, int lineNumber, boolean needSudo) {
        String command = format("tail -n %s %s", lineNumber, file);
        if (needSudo) {
            command = "sudo " + command;
        }

        return command;
    }
}
