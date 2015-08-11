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
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.codenvy.im.commands.CommandLibrary.createFileBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createFileRestoreOrBackupCommand;
import static com.codenvy.im.commands.CommandLibrary.createPropertyReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.createReplaceCommand;
import static com.codenvy.im.commands.CommandLibrary.getFileRestoreOrBackupCommand;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.write;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestCommandLibrary {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");
    public static NodeConfig testApiNode    = new NodeConfig(NodeConfig.NodeType.API, "localhost", null);
    public static NodeConfig testDataNode = new NodeConfig(NodeConfig.NodeType.DATA, "127.0.0.1", null);

    @BeforeMethod
    public void setup() {
        testApiNode = new NodeConfig(NodeConfig.NodeType.API, "localhost", null);
        testDataNode = new NodeConfig(NodeConfig.NodeType.DATA, "127.0.0.1", null);
    }

    @Test
    public void testCreateLocalPropertyReplaceCommand() {
        Command testCommand = createPropertyReplaceCommand("testFile", "property", "newValue");
        assertEquals(testCommand.toString(), "{'command'='sudo cat testFile " +
                                             "| sed ':a;N;$!ba;s/\\n/~n/g' " +
                                             "| sed 's|property *= *\"[^\"]*\"|property = \"newValue\"|g' " +
                                             "| sed 's|~n|\\n|g' > tmp.tmp " +
                                             "&& sudo mv tmp.tmp testFile', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateLocalPropertyReplaceMultiplyLineCommand() throws IOException {
        Path testFile = Paths.get("target/testFile");
        write(testFile.toFile(), "$property=\"a\n" +
                                 "b\n" +
                                 "c\n" +
                                 "\"\n");

        Command testCommand = createPropertyReplaceCommand(testFile.toString(), "$property", "1\n" +
                                                                                             "2\n" +
                                                                                             "3\n", false);
        assertEquals(testCommand.toString(), "{'command'='cat target/testFile " +
                                             "| sed ':a;N;$!ba;s/\\n/~n/g' " +
                                             "| sed 's|$property *= *\"[^\"]*\"|$property = \"1\\n2\\n3\\n\"|g' " +
                                             "| sed 's|~n|\\n|g' > tmp.tmp " +
                                             "&& mv tmp.tmp target/testFile', " +
                                             "'agent'='LocalAgent'}");

        testCommand.execute();

        String content = readFileToString(testFile.toFile());
        assertEquals(content, "$property = \"1\n" +
                              "2\n" +
                              "3\n" +
                              "\"\n");
    }

    @Test
    public void testCreateLocalReplaceCommand() throws IOException {
        Path testFile = Paths.get("target/testFile");
        write(testFile.toFile(), "old\n");

        Command testCommand = createReplaceCommand(testFile.toString(), "old", "\\$new", false);
        assertEquals(testCommand.toString(), "{'command'='cat target/testFile " +
                                             "| sed ':a;N;$!ba;s/\\n/~n/g' " +
                                             "| sed 's|old|\\\\$new|g' " +
                                             "| sed 's|~n|\\n|g' > tmp.tmp " +
                                             "&& mv tmp.tmp target/testFile', 'agent'='LocalAgent'}");
        testCommand.execute();

        String content = readFileToString(testFile.toFile());
        assertEquals(content, "\\$new\n");
    }

    @Test
    public void testCreateLocalReplaceMultiplyLineCommand() throws IOException {
        Path testFile = Paths.get("target/testFile");
        write(testFile.toFile(), "old\n");

        Command testCommand = createReplaceCommand(testFile.toString(), "old", "new\nnew\nnew\n", false);
        assertEquals(testCommand.toString(), "{'command'='cat target/testFile " +
                                             "| sed ':a;N;$!ba;s/\\n/~n/g' " +
                                             "| sed 's|old|new\\nnew\\nnew\\n|g' " +
                                             "| sed 's|~n|\\n|g' > tmp.tmp " +
                                             "&& mv tmp.tmp target/testFile', 'agent'='LocalAgent'}");
        testCommand.execute();

        String content = readFileToString(testFile.toFile());
        assertEquals(content, "new\n" +
                              "new\n" +
                              "new\n" +
                              "\n");
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

        NodeConfig node = new NodeConfig(NodeConfig.NodeType.API, "localhost", null);

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
            testApiNode,
            testDataNode
        );

        Command testCommand = CommandLibrary.createFileRestoreOrBackupCommand("testFile", nodes);
        assertEquals(testCommand.toString(), expectedCommandString);
    }

    @Test
    public void testCreateLocalAgentFileBackupCommand() {
        Command result = createFileBackupCommand("test_file");
        assertTrue(result.toString().matches(
                "\\{'command'='sudo cp test_file test_file.back ; sudo cp test_file test_file.back.[0-9]+ ; ', 'agent'='LocalAgent'\\}"),
                   result.toString());
    }

    @Test
    public void testCreatePatchBeforeUpdateMultiServerCommand() throws Exception {
        ImmutableMap<String, String> configProperties = ImmutableMap.of("test_property1", "property1");
        InstallOptions installOptions = new InstallOptions().setInstallType(InstallType.MULTI_SERVER)
                                                            .setConfigProperties(configProperties);

        Path patchDir = Paths.get("target/patches");
        createDirectories(patchDir);
        FileUtils.writeStringToFile(patchDir.resolve(InstallType.MULTI_SERVER.toString().toLowerCase()).resolve("patch_before_update.sh").toFile(), "echo -n \"$test_property1\"");

        Command command = CommandLibrary.createPatchCommand(patchDir, CommandLibrary.PatchType.BEFORE_UPDATE, installOptions);
        assertEquals(command.toString(), "[{'command'='sudo cat target/patches/multi_server/patch_before_update.sh " +
                                         "| sed ':a;N;$!ba;s/\\n/~n/g' " +
                                         "| sed 's|$test_property1|property1|g' " +
                                         "| sed 's|~n|\\n|g' > tmp.tmp " +
                                         "&& sudo mv tmp.tmp target/patches/multi_server/patch_before_update.sh', 'agent'='LocalAgent'}, " +
                                         "{'command'='bash target/patches/multi_server/patch_before_update.sh', 'agent'='LocalAgent'}]");
    }

//    @Test
//    public void testCreatePatchAfterUpdateSingleServerCommand() throws Exception {
//        ImmutableMap<String, String> configProperties = ImmutableMap.of("test_property1", "property1",
//                                                                        "test_property2", "property2");
//        InstallOptions installOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER)
//                                                            .setConfigProperties(configProperties);
//
//        Path patchDir = Paths.get("target/patches");
//        createDirectories(patchDir);
//        createDirectories(patchDir.resolve("1.0.1"));
//        createDirectories(patchDir.resolve("1.0.2"));
//
//        FileUtils.write(patchDir.resolve("1.0.1").resolve(InstallType.SINGLE_SERVER.toString().toLowerCase()).resolve("patch_after_update.sh").toFile(), "echo -n \"$test_property1\"");
//        FileUtils.write(patchDir.resolve("1.0.2").resolve(InstallType.SINGLE_SERVER.toString().toLowerCase()).resolve("patch_after_update.sh").toFile(), "echo -n \"1.0.2\"");
//
//        Command command = CommandLibrary.createPatchCommand(patchDir, CommandLibrary.PatchType.AFTER_UPDATE, installOptions);
//        assertEquals(command.toString(), "[" +
//                                         "{'command'='sudo sed -i 's/$test_property1/property1/g' target/patches/1.0.1/single_server/patch_after_update.sh', 'agent'='LocalAgent'}, " +
//                                         "{'command'='sudo sed -i 's/$test_property2/property2/g' target/patches/1.0.1/single_server/patch_after_update.sh', 'agent'='LocalAgent'}, " +
//                                         "{'command'='sudo bash target/patches/1.0.1/single_server/patch_after_update.sh', 'agent'='LocalAgent'}, " +
//                                         "{'command'='sudo sed -i 's/$test_property1/property1/g' target/patches/1.0.2/single_server/patch_after_update.sh', 'agent'='LocalAgent'}, " +
//                                         "{'command'='sudo sed -i 's/$test_property2/property2/g' target/patches/1.0.2/single_server/patch_after_update.sh', 'agent'='LocalAgent'}, " +
//                                         "{'command'='sudo bash target/patches/1.0.2/single_server/patch_after_update.sh', 'agent'='LocalAgent'}]");
//    }


    @Test
    public void testCreateStopServiceCommand() throws AgentException {
        Command testCommand = CommandLibrary.createStopServiceCommand("test-service");
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='sudo service test-service status | grep 'Active: active (running)'; " +
                                             "if [ $? -eq 0 ]; then" +
                                             "   sudo service test-service stop; " +
                                             "fi; ', " +
                                             "'agent'='LocalAgent'" +
                                             "}");

        testCommand = CommandLibrary.createStopServiceCommand("test-service", testApiNode);
        assertEquals(testCommand.toString(), format("{" +
                                                    "'command'='sudo service test-service status | grep 'Active: active (running)'; " +
                                                    "if [ $? -eq 0 ]; then" +
                                                    "   sudo service test-service stop; " +
                                                    "fi; ', " +
                                                    "'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'" +
                                                    "}", SYSTEM_USER_NAME));
    }

    @Test
    public void testCreateStartServiceCommand() throws AgentException {
        Command testCommand = CommandLibrary.createStartServiceCommand("test-service");
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='sudo service test-service status | grep 'Active: active (running)'; " +
                                             "if [ $? -ne 0 ]; then" +
                                             "   sudo service test-service start; " +
                                             "fi; ', " +
                                             "'agent'='LocalAgent'" +
                                             "}");

        testCommand = CommandLibrary.createStartServiceCommand("test-service", testApiNode);
        assertEquals(testCommand.toString(), format("{" +
                                                    "'command'='sudo service test-service status | grep 'Active: active (running)'; " +
                                                    "if [ $? -ne 0 ]; then" +
                                                    "   sudo service test-service start; " +
                                                    "fi; ', " +
                                                    "'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'" +
                                                    "}", SYSTEM_USER_NAME));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Service action 'unknown-action' isn't supported")
    public void testGetServiceManagementCommandException() {
        CommandLibrary.getServiceManagementCommand("test-service", "unknown-action");
    }

    @Test
    public void testCreateCopyFromLocalToRemoteCommand() {
        Command testCommand = CommandLibrary.createCopyFromLocalToRemoteCommand(Paths.get("local/path"), Paths.get("remote/path"),
                                                                                testApiNode.setUser(SYSTEM_USER_NAME));
        assertEquals(testCommand.toString(), format("{" +
                                                    "'command'='scp -r -q -o StrictHostKeyChecking=no local/path %s@localhost:remote/path', " +
                                                    "'agent'='LocalAgent'" +
                                                    "}", SYSTEM_USER_NAME));

        testCommand = CommandLibrary.createCopyFromLocalToRemoteCommand(Paths.get("local/path"), Paths.get("remote/path"), testDataNode.setUser(null));
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='scp -r -q -o StrictHostKeyChecking=no local/path 127.0.0.1:remote/path', " +
                                             "'agent'='LocalAgent'" +
                                             "}");

        testCommand = CommandLibrary.createCopyFromLocalToRemoteCommand(Paths.get("local/path"), Paths.get("remote/path"), testDataNode.setUser(""));
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='scp -r -q -o StrictHostKeyChecking=no local/path 127.0.0.1:remote/path', " +
                                             "'agent'='LocalAgent'" +
                                             "}");
    }

    @Test
    public void testCreateCopyFromRemoteToLocalCommand() {
        Command testCommand = CommandLibrary.createCopyFromRemoteToLocalCommand(Paths.get("remote/path"), Paths.get("local/path"), testApiNode.setUser(SYSTEM_USER_NAME));
        assertEquals(testCommand.toString(), format("{" +
                                                    "'command'='scp -r -q -o StrictHostKeyChecking=no %s@localhost:remote/path local/path', " +
                                                    "'agent'='LocalAgent'" +
                                                    "}", SYSTEM_USER_NAME));

        testCommand = CommandLibrary.createCopyFromRemoteToLocalCommand(Paths.get("remote/path"), Paths.get("local/path"), testDataNode.setUser(null));
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='scp -r -q -o StrictHostKeyChecking=no 127.0.0.1:remote/path local/path', " +
                                             "'agent'='LocalAgent'" +
                                             "}");

        testCommand = CommandLibrary.createCopyFromRemoteToLocalCommand(Paths.get("remote/path"), Paths.get("local/path"), testDataNode.setUser(""));
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='scp -r -q -o StrictHostKeyChecking=no 127.0.0.1:remote/path local/path', " +
                                             "'agent'='LocalAgent'" +
                                             "}");
    }

    @Test
    public void testCreateWaitServiceActiveCommand() throws AgentException {
        Command testCommand = CommandLibrary.createWaitServiceActiveCommand("test-service");
        assertEquals(testCommand.toString(), "{" +
                                             "'command'='doneState=\"Checking\"; " +
                                             "while [ \"${doneState}\" != \"Done\" ]; do" +
                                             "     sudo service test-service status | grep 'Active: active (running)';" +
                                             "     if [ $? -eq 0 ]; then doneState=\"Done\";" +
                                             "     else sleep 5;" +
                                             "     fi; " +
                                             "done', " +
                                             "'agent'='LocalAgent'" +
                                             "}");

        testCommand = CommandLibrary.createWaitServiceActiveCommand("test-service", testApiNode);
        assertEquals(testCommand.toString(), format("{" +
                                             "'command'='doneState=\"Checking\"; " +
                                             "while [ \"${doneState}\" != \"Done\" ]; do" +
                                             "     sudo service test-service status | grep 'Active: active (running)';" +
                                             "     if [ $? -eq 0 ]; then doneState=\"Done\";" +
                                             "     else sleep 5;" +
                                             "     fi; " +
                                             "done', " +
                                             "'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'" +
                                             "}", SYSTEM_USER_NAME));
    }

    @Test
    public void testGetWaitServiceInactiveStatusCommand() {
        String testCommand = CommandLibrary.getWaitServiceStatusCommand("test-service", "inactive");
        assertEquals(testCommand, "doneState=\"Checking\"; " +
                                  "while [ \"${doneState}\" != \"Done\" ]; do" +
                                  "     sudo service test-service status | grep 'Active: active (running)';" +
                                  "     if [ $? -ne 0 ]; then doneState=\"Done\";" +
                                  "     else sleep 5;" +
                                  "     fi; " +
                                  "done");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Service status 'unknown' isn't supported")
    public void testGetWaitServiceStatusCommandException() {
        CommandLibrary.getWaitServiceStatusCommand("test-service", "unknown");
    }

    @Test
    public void testCreateForcePuppetAgentCommand() throws AgentException {
        Command testCommand = CommandLibrary.createForcePuppetAgentCommand(testApiNode);
        assertEquals(testCommand.toString(), format("{" +
                                             "'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then" +
                                             "    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; " +
                                             "fi;', " +
                                             "'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
    }

    @Test
    public void testCreateUnpackCommand() throws AgentException {
        Path packFile = Paths.get("packFile");
        Path toDir = Paths.get("toDir");
        String pathWithinThePack = "pathWithinThePack";

        Command testCommand = CommandLibrary.createUnpackCommand(packFile, toDir);
        assertEquals(testCommand.toString(), "{'command'='tar  -xf packFile -C toDir', 'agent'='LocalAgent'}");

        testCommand = CommandLibrary.createUnpackCommand(packFile, toDir, pathWithinThePack);
        assertEquals(testCommand.toString(), "{'command'='tar  -xf packFile -C toDir pathWithinThePack', 'agent'='LocalAgent'}");

        testCommand = CommandLibrary.createUnpackCommand(packFile, toDir, pathWithinThePack, true);
        assertEquals(testCommand.toString(), "{'command'='sudo tar  -xf packFile -C toDir pathWithinThePack', 'agent'='LocalAgent'}");

        testCommand = CommandLibrary.createUnpackCommand(packFile, toDir, pathWithinThePack, testApiNode);
        assertEquals(testCommand.toString(),
                     format("{'command'='sudo tar  -xf packFile -C toDir pathWithinThePack', 'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}",
                            SYSTEM_USER_NAME));
    }

    @Test
    public void testCreateCompressCommand() throws AgentException {
        Path fromDir = Paths.get("fromDir");
        Path packFile = Paths.get("packFile");
        String pathWithinThePack = "pathWithinThePack";

        Command testCommand = CommandLibrary.createCompressCommand(fromDir, packFile, pathWithinThePack, testApiNode);
        assertEquals(testCommand.toString(),
                     format("{'command'='if sudo test -f packFile; then    sudo tar -C fromDir -z -rf packFile pathWithinThePack;else    sudo tar -C fromDir -z -cf packFile pathWithinThePack;fi;', 'agent'='{'host'='localhost', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}",
                            SYSTEM_USER_NAME));
    }

    @Test
    public void testCreateUncompressCommand() throws AgentException {
        Path packFile = Paths.get("packFile");
        Path toDir = Paths.get("toDir");

        Command testCommand = CommandLibrary.createUncompressCommand(packFile, toDir);
        assertEquals(testCommand.toString(), "{'command'='tar -z -xf packFile -C toDir', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateReadFileCommand() throws AgentException {
        Path fileToRead = Paths.get("messages");
        Command command = CommandLibrary.createTailCommand(fileToRead, 5, false);
        assertEquals(command.toString(), "{'command'='tail -n 5 messages', 'agent'='LocalAgent'}");

        command = CommandLibrary.createTailCommand(fileToRead, 6, true);
        assertEquals(command.toString(), "{'command'='sudo tail -n 6 messages', 'agent'='LocalAgent'}");

        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.API, "host", "user");
        command = CommandLibrary.createTailCommand(fileToRead, 5, testNode, false);
        assertEquals(command.toString(), "{'command'='tail -n 5 messages', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");

        command = CommandLibrary.createTailCommand(fileToRead, 6, testNode, true);
        assertEquals(command.toString(),
                     "{'command'='sudo tail -n 6 messages', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");
    }

    @Test
    public void testCreateCopyCommand() throws AgentException {
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.API, "host", "user");
        Command command = CommandLibrary.createCopyCommand(Paths.get("from"), Paths.get("to"), testNode, true);
        assertEquals(command.toString(), "{'command'='sudo cp from to', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");

        command = CommandLibrary.createCopyCommand(Paths.get("from"), Paths.get("to"), testNode, false);
        assertEquals(command.toString(), "{'command'='cp from to', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");

        command = CommandLibrary.createCopyCommand(Paths.get("from"), Paths.get("to"));
        assertEquals(command.toString(), "{'command'='cp from to', 'agent'='LocalAgent'}");

        command = CommandLibrary.createCopyCommand(Paths.get("from"), Paths.get("to"), true);
        assertEquals(command.toString(), "{'command'='sudo cp from to', 'agent'='LocalAgent'}");

        command = CommandLibrary.createCopyCommand(Paths.get("from"), Paths.get("to"), false);
        assertEquals(command.toString(), "{'command'='cp from to', 'agent'='LocalAgent'}");
    }

    @Test
    public void testCreateChmodCommand() throws AgentException {
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.API, "host", "user");
        Command command = CommandLibrary.createChmodCommand("007", Paths.get("file"), testNode, true);
        assertEquals(command.toString(), "{'command'='sudo chmod 007 file', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");

        command = CommandLibrary.createChmodCommand("007", Paths.get("file"), testNode, false);
        assertEquals(command.toString(), "{'command'='chmod 007 file', 'agent'='{'host'='host', 'user'='user', 'identity'='[~/.ssh/id_rsa]'}'}");

        command = CommandLibrary.createChmodCommand("007", Paths.get("file"), true);
        assertEquals(command.toString(), "{'command'='sudo chmod 007 file', 'agent'='LocalAgent'}");

        command = CommandLibrary.createChmodCommand("007", Paths.get("file"), false);
        assertEquals(command.toString(), "{'command'='chmod 007 file', 'agent'='LocalAgent'}");
    }
}
