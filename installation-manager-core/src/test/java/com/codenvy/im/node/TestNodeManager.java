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
package com.codenvy.im.node;

import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.command.Command;
import com.codenvy.im.command.CommandException;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.utils.Version;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static com.codenvy.im.service.InstallationManagerConfig.CONFIG_FILE;
import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/** @author Dmytro Nochevnov */
public class TestNodeManager {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");
    @Mock
    private ConfigUtil mockConfigUtil;
    @Mock
    private Config                    mockConfig;
    @Mock
    private AdditionalNodesConfigUtil mockNodesConfigUtil;
    @Mock
    private CDECArtifact              mockCdecArtifact;
    @Mock
    private Command                   mockCommand;


    private static final String              TEST_NODE_DNS  = "localhost";
    private static final NodeConfig.NodeType TEST_NODE_TYPE = NodeConfig.NodeType.RUNNER;
    private static final NodeConfig          TEST_NODE      = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS);

    private static final Version TEST_VERSION                     = Version.valueOf("1.0.0");
    private static final String  TEST_RUNNER_NODE_URL             = "test_runner_node_url";
    private static final String  ADDITIONAL_RUNNERS_PROPERTY_NAME = "additional_runners";
    private static final String  MOCK_COMMAND_EXECUTE_RESULT      = "mock_command_execute_result";

    private NodeManager spyManager;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_VERSION).when(mockCdecArtifact).getInstalledVersion();

        initConfigs();

        spyManager = spy(new NodeManager(mockConfigUtil, mockCdecArtifact));
        doReturn(mockConfig).when(spyManager).getCodenvyConfig(mockConfigUtil);
        doReturn(mockNodesConfigUtil).when(spyManager).getNodesConfigUtil(mockConfig);

        CONFIG_FILE = Paths.get(this.getClass().getClassLoader().getResource("im.properties").getPath());
    }

    private void initConfigs() {
        doReturn(ADDITIONAL_RUNNERS_PROPERTY_NAME).when(mockNodesConfigUtil).getPropertyNameBy(TEST_NODE_TYPE);
        doReturn(TEST_RUNNER_NODE_URL).when(mockNodesConfigUtil).getValueWithNode(TEST_NODE);

        doReturn("127.0.0.1").when(mockConfig).getValue(NodeConfig.NodeType.API.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
    }

    @Test
    public void testAddNode() throws IOException {
        doNothing().when(spyManager).validate(TEST_NODE);
        doReturn(true).when(spyManager).isPuppetAgentActive(TEST_NODE);

        doReturn(mockCommand).when(spyManager).getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, mockConfig);
        doReturn(MOCK_COMMAND_EXECUTE_RESULT).when(mockCommand).execute();

        assertEquals(spyManager.add(TEST_NODE), MOCK_COMMAND_EXECUTE_RESULT);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "This type of node isn't supported")
    public void testAddNodeWhichIsNotSupported() throws IOException {
        doReturn(ADDITIONAL_RUNNERS_PROPERTY_NAME).when(mockNodesConfigUtil).getPropertyNameBy(TEST_NODE.getType());

        spyManager.add(new NodeConfig(NodeConfig.NodeType.API, "some"));
    }

    @Test
    public void testGetAddNodeCommandWithoutPuppetAgent() throws IOException {
        doReturn(false).when(spyManager).isPuppetAgentActive(TEST_NODE);

        Command result = spyManager.getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, mockConfig);
        assertEquals(result.toString(), format("[" +
                                        "{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back', " +
                                        "'agent'='LocalAgent'}, " +
                                        "{'command'='sudo sed -i 's/$additional_runners = .*/$additional_runners = \"test_runner_node_url\"/g' /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp', " +
                                        "'agent'='LocalAgent'}, " +
                                        "[{'command'='if [ \"`yum list installed | grep puppetlabs-release.noarch`\" == \"\" ]; then sudo yum -y -q install null; fi', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo yum -y -q install null', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target.wants/puppet.service'; fi', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo systemctl enable puppet', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo sed -i 's/\\[main\\]/\\[main\\]\\n  server = master\\n  runinterval = 420\\n  configtimeout = 600\\n/g' /etc/puppet/puppet.conf', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n  show_diff = true\\n  pluginsync = true\\n  report = true\\n  default_schedules = false\\n  certname = localhost\\n/g' /etc/puppet/puppet.conf', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='sudo systemctl start puppet', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='doneState=\"Installing\"; testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; while [ \"${doneState}\" != \"Installed\" ]; do     if sudo test -f ${testFile}; then doneState=\"Installed\"; fi;     sleep 30; done', " +
                                        "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "[{'command'='testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; while true; do     if sudo grep \"test_runner_node_url$\" ${testFile}; then break; fi;     sleep 5; done; sleep 15; # delay to involve into start of rebooting api server', " +
                                        "'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                                        "Expected to be installed 'mockCdecArtifact' of the version '1.0.0']", System.getProperty("user.name")));
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testGetAddNodeCommandWhenGetValueWithNodeException() throws IOException {
        doThrow(new IllegalArgumentException("error")).when(mockNodesConfigUtil).getValueWithNode(TEST_NODE);

        spyManager.getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, mockConfig);
    }

    @Test
    public void testRemoveNode() throws IOException {
        doReturn(TEST_NODE_TYPE).when(mockNodesConfigUtil).recognizeNodeTypeBy(TEST_NODE_DNS);
        doReturn(mockCommand).when(spyManager).getRemoveNodeCommand(TEST_NODE, mockConfig, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
        doReturn(MOCK_COMMAND_EXECUTE_RESULT).when(mockCommand).execute();

        assertEquals(spyManager.remove(TEST_NODE_DNS), MOCK_COMMAND_EXECUTE_RESULT);
    }

    @Test(expectedExceptions = NodeException.class,
          expectedExceptionsMessageRegExp = "Node 'localhost' is not found in Codenvy configuration among additional nodes")
    public void testRemoveNonExistsNodeError() throws IOException {
        doReturn(mockCommand).when(spyManager).getRemoveNodeCommand(TEST_NODE, mockConfig, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
        doReturn(MOCK_COMMAND_EXECUTE_RESULT).when(mockCommand).execute();

        assertEquals(spyManager.remove(TEST_NODE_DNS), MOCK_COMMAND_EXECUTE_RESULT);
    }

    @Test
    public void testGetRemoveNodeCommand() throws IOException {
        Command removeNodeCommand = spyManager.getRemoveNodeCommand(TEST_NODE, mockConfig, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
        assertEquals(removeNodeCommand.toString(),
                     format("[" +
                     "{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back', " +
                     "'agent'='LocalAgent'}, " +
                     "{'command'='sudo sed -i 's/$additional_runners = .*/$additional_runners = \"null\"/g' /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp', " +
                     "'agent'='LocalAgent'}, " +
                     "[{'command'='testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; while true; do     if ! sudo grep \"localhost\" ${testFile}; then break; fi;     sleep 5; done; sleep 15; # delay to involve into start of rebooting api server', " +
                     "'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                     "Expected to be installed 'mockCdecArtifact' of the version '1.0.0', " +
                     "{'command'='sudo puppet cert clean localhost', " +
                     "'agent'='LocalAgent'}, " +
                     "{'command'='sudo service puppetmaster restart', " +
                     "'agent'='LocalAgent'}, " +
                     "[{'command'='sudo service puppet stop', " +
                     "'agent'='{'host'='localhost', 'user'='ndp', 'identity'='[~/.ssh/id_rsa]'}'}], " +
                     "[{'command'='rm -rf /var/lib/puppet/ssl', " +
                     "'agent'='{'host'='localhost', 'user'='ndp', 'identity'='[~/.ssh/id_rsa]'}'}]" +
                     "]", SYSTEM_USER_NAME));
    }

    @Test(expectedExceptions = NodeException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testGetRemoveNodeCommandWhenGetValueWithNodeException() throws IOException {
        doThrow(new IllegalArgumentException("error")).when(mockConfig).getValue(NodeConfig.NodeType.API.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);

        spyManager.getRemoveNodeCommand(TEST_NODE, mockConfig, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
    }

    @Test(dataProvider = "PuppetAgentStates")
    public void testIsPuppetAgentActive(String state, boolean expectedResult) throws AgentException, CommandException {
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo service puppet status", TEST_NODE);
        doReturn(state).when(mockCommand).execute();
        assertEquals(spyManager.isPuppetAgentActive(TEST_NODE), expectedResult);
    }

    @DataProvider(name = "PuppetAgentStates")
    public static Object[][] PuppetAgentStates() {
        return new Object[][]{
            {null, false},
            {"Active: inactive (dead)", false},
            {"Loaded: loaded  Active: active (running)", true},
        };
    }

    @Test
    public void testIsPuppetAgentActiveWhenCommandError() throws AgentException, CommandException {
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo service puppet status", TEST_NODE);
        doThrow(new CommandException("error", null)).when(mockCommand).execute();
        assertFalse(spyManager.isPuppetAgentActive(TEST_NODE));
    }

    @Test
    public void testValidateNode() throws AgentException, CommandException, NodeException {
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doReturn("").when(mockCommand).execute();
        spyManager.validate(TEST_NODE);
    }

    @Test(expectedExceptions = NodeException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testValidateNodeCommandException() throws IOException {
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doThrow(new CommandException("error", null)).when(mockCommand).execute();
        spyManager.validate(TEST_NODE);
    }

    @Test(expectedExceptions = NodeException.class,
          expectedExceptionsMessageRegExp = "error")
    public void testValidateNodeAgentException() throws AgentException, CommandException, NodeException {
        doThrow(new AgentException("error")).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        spyManager.validate(TEST_NODE);
    }

    @Test
    public void testGetCodenvyConfig() throws IOException {
        Config config = spyManager.getCodenvyConfig(mockConfigUtil);
        assertNotNull(config);
    }

    @Test
    public void testNodesConfigUtil() {
        AdditionalNodesConfigUtil config = spyManager.getNodesConfigUtil(mockConfig);
        assertNotNull(config);
    }

    @Test
    public void testShellAgentCommand() throws AgentException {
        Command command = spyManager.getShellAgentCommand("test", TEST_NODE);
        assertEquals(command.toString(), format("[{'command'='test', " +
                                         "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}]", SYSTEM_USER_NAME));
    }
}
