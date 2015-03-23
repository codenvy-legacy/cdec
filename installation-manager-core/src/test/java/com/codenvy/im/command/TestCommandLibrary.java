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
import com.codenvy.im.agent.LocalAgent;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static com.codenvy.im.command.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.command.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.command.CommandLibrary.createReplaceCommand;
import static com.codenvy.im.command.CommandLibrary.createFileRestoreOrBackupCommand;
import static com.codenvy.im.command.MacroCommand.createCommand;
import static com.codenvy.im.command.CommandLibrary.createFileRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandLibrary.getFileRestoreOrBackupCommand;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestCommandLibrary {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    @Test
    public void testCreateLocalPropertyReplaceCommand() {
        Command testCommand = createPropertyReplaceCommand("testFile", "property", "newValue");
        assertEquals(testCommand.toString(), "{'command'='sudo sed -i 's/property = .*/property = \"newValue\"/g' testFile', " +
                                             "'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateLocalReplaceCommand() {
        Command testCommand = createReplaceCommand("testFile", "old", "new");
        assertEquals(testCommand.toString(), "{'command'='sudo sed -i 's/old/new/g' testFile', " +
                                             "'agent'='LocalAgent'}");
    }

    @Test
    public void testGetRestoreOrBackupCommand() {
        String testCommand = getFileRestoreOrBackupCommand("testFile");
        assertEquals(testCommand, "if sudo test -f testFile; then" +
                                  "     if ! sudo test -f testFile.back; then" +
                                  "         sudo cp testFile testFile.back;" +
                                  "     else" +
                                  "         sudo cp testFile.back testFile;" +
                                  "     fi fi");
    }

    @Test
    public void testCreateLocalFileRestoreOrBackupCommand() throws AgentException {
        Command testCommand = createFileRestoreOrBackupCommand("testFile");
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='if sudo test -f testFile; then" +
                                             "     if ! sudo test -f testFile.back; then" +
                                             "         sudo cp testFile testFile.back;" +
                                             "     else" +
                                             "         sudo cp testFile.back testFile;" +
                                             "     fi fi', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateShellAgentFileRestoreOrBackupCommandForNode() throws AgentException {
        String expectedCommandString = format("[" +
                                              "{'command'='if sudo test -f testFile; then" +
                                              "     if ! sudo test -f testFile.back; then" +
                                              "         sudo cp testFile testFile.back;" +
                                              "     else" +
                                              "         sudo cp testFile.back testFile;" +
                                              "     fi fi', " +
                                              "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}" +
                                              "]",
                                              SYSTEM_USER_NAME);

        NodeConfig node = new NodeConfig(NodeConfig.NodeType.API, "localhost");

        Command testCommand = createFileRestoreOrBackupCommand("testFile", node);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateShellFileRestoreOrBackupCommand() throws AgentException {
        String expectedCommandString = format("[" +
                                              "{'command'='if sudo test -f testFile; then" +
                                              "     if ! sudo test -f testFile.back; then" +
                                              "         sudo cp testFile testFile.back;" +
                                              "     else" +
                                              "         sudo cp testFile.back testFile;" +
                                              "     fi fi', " +
                                              "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}, " +
                                              "{'command'='if sudo test -f testFile; then " +
                                              "    if ! sudo test -f testFile.back; then" +
                                              "         sudo cp testFile testFile.back;" +
                                              "     else" +
                                              "         sudo cp testFile.back testFile;" +
                                              "     fi fi', " +
                                              "'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}" +
                                              "]",
                                              SYSTEM_USER_NAME);

        List<NodeConfig> nodes = ImmutableList.of(
            new NodeConfig(NodeConfig.NodeType.API, "localhost"),
            new NodeConfig(NodeConfig.NodeType.DATA, "127.0.0.1")
        );

        Command testCommand = CommandLibrary.createFileRestoreOrBackupCommand("testFile", nodes);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateLocalAgentFileBackupCommand() {
        Command result = createFileBackupCommand("test_file");
        assertEquals(result.toString(), "{'command'='sudo cp test_file test_file.back', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreatePatchCommand() throws Exception {
        Path patchDir = Paths.get("target/patches");
        createDirectories(patchDir);
        createDirectories(patchDir.resolve("1.0.1"));
        createDirectories(patchDir.resolve("1.0.2"));

        FileUtils.write(patchDir.resolve("1.0.1").resolve("patch.sh").toFile(), "echo -n \"1.0.1\"");
        FileUtils.write(patchDir.resolve("1.0.2").resolve("patch.sh").toFile(), "echo -n \"1.0.2\"");

        Command command = CommandLibrary.createPatchCommand(patchDir, Version.valueOf("1.0.0"), Version.valueOf("1.0.2"));
        assertTrue(command instanceof MacroCommand);

        String batch = command.toString();
        batch = batch.substring(1, batch.length() - 1);
        String[] s = batch.split(", ");

        assertEquals(Arrays.toString(s), "[" +
                                         "{'command'='sudo bash target/patches/1.0.1/patch.sh', 'agent'='LocalAgent'}, " +
                                         "{'command'='sudo bash target/patches/1.0.2/patch.sh', 'agent'='LocalAgent'}" +
                                         "]");

        String patchCommandWithoutSudo1 = s[0].substring(17, 51);
        String patchCommandWithoutSudo2 = s[2].substring(17, 51);

        command = new MacroCommand(ImmutableList.<Command>of(new SimpleCommand(patchCommandWithoutSudo1, new LocalAgent(), null),
                                                             new SimpleCommand(patchCommandWithoutSudo2, new LocalAgent(), null)),
                                   null);

        String output = command.execute();
        assertEquals(output, "1.0.1\n" +
                             "1.0.2\n");
    }

    @Test
    public void testCreateLocalStopServiceCommand() {
        // TODO [ndp]
    }
}
