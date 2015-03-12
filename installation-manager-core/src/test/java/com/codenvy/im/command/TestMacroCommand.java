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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestMacroCommand {
    @Test
    public void testDescription() {
        MacroCommand testCommand = new MacroCommand(Collections.<Command>emptyList(), "macro");
        assertEquals(testCommand.getDescription(), "macro");
    }

    @Test
    public void testToString() {
        List<Command> commands = new ArrayList<Command>() {{
            add(new DummyCommand("command1"));
            add(new DummyCommand("command2"));
        }};

        MacroCommand testCommand = new MacroCommand(commands, "macro");
        assertEquals(testCommand.toString(), "[command1, command2]");
    }

    @Test
    public void testExecution() throws CommandException {
        List<Command> commands = new ArrayList<Command>() {{
            add(new DummyCommand("command1"));
            add(new DummyCommand("command2"));
        }};

        MacroCommand testCommand = new MacroCommand(commands, "macro");

        String result = testCommand.execute();
        assertEquals(result, "command1\ncommand2\n");
    }

    @Test(expectedExceptions = CommandException.class, expectedExceptionsMessageRegExp = "exception")
    public void testInnerCommandException() throws CommandException {
        final DummyCommand spyDummyCommand = spy(new DummyCommand("command1"));
        doThrow(new CommandException("exception", new IOException())).when(spyDummyCommand).execute();

        MacroCommand testCommand = new MacroCommand(new ArrayList<Command>() {{
            add(spyDummyCommand);
        }}, "macro");

        testCommand.execute();
    }

    @Test
    public void testCreateShellCommandsForEachNode() throws AgentException {
        String command = "command";
        String description = "description";
        List<NodeConfig> nodes = ImmutableList.of(
            new NodeConfig(NodeConfig.NodeType.API, "localhost"),
            new NodeConfig(NodeConfig.NodeType.DATA, "127.0.0.1")
        );

        String user = System.getProperty("user.name");
        String expectedCommand = format("[" +
                                        "{'command'='command', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}, " +
                                        "{'command'='command', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}" +
                                        "]", user);

        Command testMacroCommand = MacroCommand.createShellAgentCommand(command, description, nodes);
        assertEquals(testMacroCommand.toString(), expectedCommand);
    }

    @Test
    public void testCreatePatchCommand() throws Exception {
        Path patchDir = Paths.get("target/patches");
        createDirectories(patchDir);
        createDirectories(patchDir.resolve("1.0.1"));
        createDirectories(patchDir.resolve("1.0.2"));

        FileUtils.write(patchDir.resolve("1.0.1").resolve("patch.sh").toFile(), "echo -n \"1.0.1\"");
        FileUtils.write(patchDir.resolve("1.0.2").resolve("patch.sh").toFile(), "echo -n \"1.0.2\"");

        Command command = MacroCommand.createPatchCommand(patchDir, Version.valueOf("1.0.0"), Version.valueOf("1.0.2"));
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

    class DummyCommand implements Command {
        private String description;

        public DummyCommand(String description) {
            this.description = description;
        }

        @Override
        public String execute() throws CommandException {
            return description;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
