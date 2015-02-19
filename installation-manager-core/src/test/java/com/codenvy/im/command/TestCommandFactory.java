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
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.List;

import static com.codenvy.im.command.CommandFactory.createLocalAgentPropertyReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentReplaceCommand;
import static com.codenvy.im.command.CommandFactory.createLocalAgentRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentCommand;
import static com.codenvy.im.command.CommandFactory.createShellAgentRestoreOrBackupCommand;
import static com.codenvy.im.command.CommandFactory.getRestoreOrBackupCommand;
import static java.lang.String.format;
import static org.testng.Assert.*;

/** @author Dmytro Nochevnov */
public class TestCommandFactory {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    @Test
    public void testCreateLocalPropertyReplaceCommand() {
        Command testCommand = createLocalAgentPropertyReplaceCommand("testFile", "property", "newValue");
        assertEquals(testCommand.toString(), "{'command'='sudo sed -i 's/property = .*/property = \"newValue\"/g' testFile', " +
                                             "'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateLocalReplaceCommand() {
        Command testCommand = createLocalAgentReplaceCommand("testFile", "old", "new");
        assertEquals(testCommand.toString(), "{'command'='sudo sed -i 's/old/new/g' testFile', " +
                                             "'agent'='LocalAgent'}");
    }

    @Test
    public void testGetRestoreOrBackupCommand() {
        String testCommand = getRestoreOrBackupCommand("testFile");
        assertEquals(testCommand, "if sudo test -f testFile; then" +
                                  "     if ! sudo test -f testFile.back; then" +
                                  "         sudo cp testFile testFile.back;" +
                                  "     else" +
                                  "         sudo cp testFile.back testFile;" +
                                  "     fi fi");
    }

    @Test
    public void testCreateShellCommandForNodeList() throws AgentException {
        String expectedCommandString = format("[" +
                                              "{'command'='testCommand', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}, " +
                                              "{'command'='testCommand', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}" +
                                              "]",
                                              SYSTEM_USER_NAME);

        List<NodeConfig> nodes = ImmutableList.of(
            new NodeConfig(NodeConfig.NodeType.API, "localhost"),
            new NodeConfig(NodeConfig.NodeType.DATA, "127.0.0.1")
        );

        Command testCommand = createShellAgentCommand("testCommand", nodes);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateShellCommandForNode() throws AgentException {
        String expectedCommandString = format("[{'command'='testCommand', 'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}]",
                                              SYSTEM_USER_NAME);

        NodeConfig node = new NodeConfig(NodeConfig.NodeType.DATA, "localhost");
        Command testCommand = createShellAgentCommand("testCommand", node);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateLocalRestoreOrBackupCommand() throws AgentException {
        Command testCommand = createLocalAgentRestoreOrBackupCommand("testFile");
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='if sudo test -f testFile; then" +
                                             "     if ! sudo test -f testFile.back; then" +
                                             "         sudo cp testFile testFile.back;" +
                                             "     else" +
                                             "         sudo cp testFile.back testFile;" +
                                             "     fi fi', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateShellRestoreOrBackupCommand() throws AgentException {
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

        Command testCommand = createShellAgentRestoreOrBackupCommand("testFile", nodes);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateShellAgentRestoreOrBackupCommandForOneNode() {
        // TODO [ndp]
    }

    @Test
    public void testCreateLocalAgentBackupCommand() {
        // TODO [ndp]
    }

    @Test
    public void testCreateShellAgentBackupCommandOnNode() {
        // TODO [ndp]
    }

    @Test
    public void testGetBackupCommand() {
        // TODO [ndp]
    }
}
