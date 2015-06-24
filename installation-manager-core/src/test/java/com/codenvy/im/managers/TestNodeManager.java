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
package com.codenvy.im.managers;

import com.codenvy.im.BaseTest;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.utils.Version;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static java.lang.String.format;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestNodeManager extends BaseTest {

    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    @Mock
    private ConfigManager             configManager;
    @Mock
    private Config                    config;
    @Mock
    private AdditionalNodesConfigUtil mockNodesConfigUtil;
    @Mock
    private CDECArtifact              mockCdecArtifact;
    @Mock
    private Command                   mockCommand;

    private static final String              TEST_NODE_DNS  = "localhost";
    private static final NodeConfig.NodeType TEST_NODE_TYPE = NodeConfig.NodeType.RUNNER;
    private static final NodeConfig          TEST_NODE      = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS, null);

    private static final Version TEST_VERSION                     = Version.valueOf("1.0.0");
    private static final String  TEST_RUNNER_NODE_URL             = "test_runner_node_url";
    private static final String  ADDITIONAL_RUNNERS_PROPERTY_NAME = "additional_runners";

    private NodeManager spyManager;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        doReturn(TEST_VERSION).when(mockCdecArtifact).getInstalledVersion();

        initConfigs();

        spyManager = spy(new NodeManager(configManager, mockCdecArtifact));
        doReturn(config).when(spyManager).getCodenvyConfig(configManager);
        doReturn(mockNodesConfigUtil).when(spyManager).getNodesConfigUtil(config);
    }

    private void initConfigs() {
        doReturn(ADDITIONAL_RUNNERS_PROPERTY_NAME).when(mockNodesConfigUtil).getPropertyNameBy(TEST_NODE_TYPE);
        doReturn(TEST_RUNNER_NODE_URL).when(mockNodesConfigUtil).getValueWithNode(TEST_NODE);

        doReturn("127.0.0.1").when(config).getValue(NodeConfig.NodeType.API.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
    }

    @Test
    public void testAddNode() throws Exception {
        prepareMultiNodeEnv(configManager);
        doNothing().when(spyManager).validate(TEST_NODE);
        doReturn(TEST_NODE).when(mockNodesConfigUtil).recognizeNodeConfigFromDns(TEST_NODE_DNS);
        doReturn(mockCommand).when(spyManager)
                             .getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, config);

        assertEquals(spyManager.add(TEST_NODE_DNS), TEST_NODE);
        verify(mockCommand).execute();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "This type of node isn't supported")
    public void testAddNodeWhichIsNotSupported() throws Exception {
        prepareMultiNodeEnv(configManager);
        doNothing().when(spyManager).validate(TEST_NODE);
        doReturn(TEST_NODE).when(mockNodesConfigUtil).recognizeNodeConfigFromDns(TEST_NODE_DNS);
        doReturn(null).when(mockNodesConfigUtil).getPropertyNameBy(TEST_NODE.getType());

        spyManager.add(TEST_NODE_DNS);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "You can add node to Multi-Server Codenvy only")
    public void testAddNodeToSingleServerCodenvyException() throws Exception {
        prepareSingleNodeEnv(configManager);
        spyManager.add(TEST_NODE_DNS);
    }

    @Test
    public void testGetAddNodeCommand() throws Exception {
        prepareMultiNodeEnv(configManager);
        Command result = spyManager.getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, config);
        assertNotNull(result);
        assertTrue(result instanceof MacroCommand);

        List<Command> commands = ((MacroCommand) result).getCommands();
        assertEquals(commands.size(), 15);

        assertTrue(commands.get(0).toString().matches("\\{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back ; sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back.[0-9]+ ; ', 'agent'='LocalAgent'\\}"),
                   commands.get(0).toString());

        assertEquals(commands.get(1).toString(), "{'command'='sudo sed -i 's|$additional_runners = .*|$additional_runners = \"test_runner_node_url\"|g' /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp', 'agent'='LocalAgent'}");
        assertEquals(commands.get(2).toString(), "{'command'='sudo sh -c \"echo -e 'localhost' >> /etc/puppet/autosign.conf\"', 'agent'='LocalAgent'}");
        assertEquals(commands.get(3).toString(), format("{'command'='yum list installed | grep puppetlabs-release.noarch; if [ $? -ne 0 ]; then sudo yum -y -q install null; fi', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(4).toString(), format("{'command'='sudo yum -y -q install null', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(5).toString(), format("{'command'='if [ ! -f /etc/systemd/system/multi-user.target.wants/puppet.service ]; then sudo ln -s '/usr/lib/systemd/system/puppet.service' '/etc/systemd/system/multi-user.target.wants/puppet.service'; fi', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(6).toString(), format("{'command'='sudo systemctl enable puppet', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));

        assertTrue(commands.get(7).toString().matches(format("\\{'command'='sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back ; sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back.[0-9]+ ; ', 'agent'='\\{'host'='localhost', 'user'='%1$s', 'identity'='\\[~/.ssh/id_rsa\\]'\\}'\\}", SYSTEM_USER_NAME)),
                   commands.get(7).toString());

        assertEquals(commands.get(8).toString(), format("{'command'='sudo sed -i 's/\\[main\\]/\\[main\\]\\n  server = null\\n  runinterval = 420\\n  configtimeout = 600\\n/g' /etc/puppet/puppet.conf', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(9).toString(), format("{'command'='sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n  show_diff = true\\n  pluginsync = true\\n  report = true\\n  default_schedules = false\\n  certname = localhost\\n/g' /etc/puppet/puppet.conf', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(10).toString(), format("{'command'='sudo systemctl start puppet', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(11).toString(), format("{'command'='doneState=\"Installing\"; testFile=\"/home/codenvy/codenvy-tomcat/logs/catalina.out\"; while [ \"${doneState}\" != \"Installed\" ]; do     if sudo test -f ${testFile}; then doneState=\"Installed\"; fi;     sleep 30; done', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(12).toString(), format("{'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; fi;', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(13).toString(), format("{'command'='testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; while true; do     if sudo grep \"test_runner_node_url$\" ${testFile}; then break; fi;     sleep 5; done; sleep 15; # delay to involve into start of rebooting api server', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(14).toString(), "Expected to be installed 'mockCdecArtifact' of the version '1.0.0'");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testGetAddNodeCommandWhenGetValueWithNodeException() throws Exception {
        prepareMultiNodeEnv(configManager);
        doThrow(new IllegalArgumentException("error")).when(mockNodesConfigUtil).getValueWithNode(TEST_NODE);

        spyManager.getAddNodeCommand(TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME, mockNodesConfigUtil, TEST_NODE, config);
    }

    @Test
    public void testRemoveNode() throws Exception {
        prepareMultiNodeEnv(configManager);
        doReturn(TEST_NODE_TYPE).when(mockNodesConfigUtil).recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);
        doReturn(mockCommand).when(spyManager)
                             .getRemoveNodeCommand(TEST_NODE, config, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
        doReturn(TEST_NODE).when(mockNodesConfigUtil).recognizeNodeConfigFromDns(TEST_NODE_DNS);

        assertEquals(spyManager.remove(TEST_NODE_DNS), TEST_NODE);
        verify(mockCommand).execute();
    }

    @Test(expectedExceptions = NodeException.class,
            expectedExceptionsMessageRegExp = "Node 'localhost' is not found in Codenvy configuration among additional nodes")
    public void testRemoveNonExistsNodeError() throws Exception {
        prepareMultiNodeEnv(configManager);
        doReturn(mockCommand).when(spyManager)
                             .getRemoveNodeCommand(TEST_NODE, config, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);

        assertEquals(spyManager.remove(TEST_NODE_DNS), TEST_NODE);
        verify(mockCommand).execute();
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "You can remove node from Multi-Server Codenvy only")
    public void testRemoveNodeFromSingleServerCodenvyException() throws Exception {
        prepareSingleNodeEnv(configManager);
        spyManager.remove(TEST_NODE_DNS);
    }

    @Test
    public void testGetRemoveNodeCommand() throws Exception {
        prepareMultiNodeEnv(configManager);
        Command removeNodeCommand =
                spyManager.getRemoveNodeCommand(TEST_NODE, config, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);

        List<Command> commands = ((MacroCommand) removeNodeCommand).getCommands();
        assertEquals(commands.size(), 9);

        assertTrue(commands.get(0).toString().matches(
            "\\{'command'='sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back ; sudo cp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp.back.[0-9]+ ; ', 'agent'='LocalAgent'\\}"),
                   commands.get(0).toString());

        assertEquals(commands.get(1).toString(),
                     "{'command'='sudo sed -i 's|$additional_runners = .*|$additional_runners = \"null\"|g' /etc/puppet/manifests/nodes/multi_server/custom_configurations.pp', 'agent'='LocalAgent'}");
        assertEquals(commands.get(2).toString(), format("{'command'='if ! sudo test -f /var/lib/puppet/state/agent_catalog_run.lock; then    sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay; fi;', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(3).toString(), format("{'command'='testFile=\"/home/codenvy/codenvy-data/conf/general.properties\"; while true; do     if ! sudo grep \"localhost\" ${testFile}; then break; fi;     sleep 5; done; sleep 15; # delay to involve into start of rebooting api server', 'agent'='{'host'='127.0.0.1', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(4).toString(), "Expected to be installed 'mockCdecArtifact' of the version '1.0.0'");
        assertEquals(commands.get(5).toString(), "{'command'='sudo puppet cert clean localhost', 'agent'='LocalAgent'}");
        assertEquals(commands.get(6).toString(), "{'command'='sudo service puppetmaster restart', 'agent'='LocalAgent'}");
        assertEquals(commands.get(7).toString(), format("{'command'='sudo service puppet stop', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(8).toString(), format("{'command'='sudo rm -rf /var/lib/puppet/ssl', 'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
    }

    @Test(expectedExceptions = NodeException.class,
            expectedExceptionsMessageRegExp = "error")
    public void testGetRemoveNodeCommandWhenGetValueWithNodeException() throws Exception {
        prepareMultiNodeEnv(configManager);
        doThrow(new IllegalArgumentException("error")).when(config)
                                                      .getValue(NodeConfig.NodeType.API.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);

        spyManager.getRemoveNodeCommand(TEST_NODE, config, mockNodesConfigUtil, TEST_VERSION, ADDITIONAL_RUNNERS_PROPERTY_NAME);
    }

    @Test(dataProvider = "PuppetAgentStates")
    public void testIsPuppetAgentActive(String state, boolean expectedResult) throws Exception {
        prepareMultiNodeEnv(configManager);
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
    public void testIsPuppetAgentActiveWhenCommandError() throws Exception {
        prepareMultiNodeEnv(configManager);
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
    public void testValidateNodeCommandException() throws Exception {
        prepareMultiNodeEnv(configManager);
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doThrow(new CommandException("error", null)).when(mockCommand).execute();
        spyManager.validate(TEST_NODE);
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testValidateNodeAgentException() throws Exception {
        prepareMultiNodeEnv(configManager);
        doThrow(new AgentException("error")).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        spyManager.validate(TEST_NODE);
    }

    @Test
    public void testGetCodenvyConfig() throws Exception {
        prepareMultiNodeEnv(configManager);
        doReturn(config).when(configManager).loadInstalledCodenvyConfig(InstallType.MULTI_SERVER);

        NodeManager manager = new NodeManager(configManager, mockCdecArtifact);
        Config config = manager.getCodenvyConfig(configManager);
        assertEquals(config, this.config);
    }

    @Test
    public void testNodesConfigUtil() throws Exception {
        prepareMultiNodeEnv(configManager);
        NodeManager manager = new NodeManager(configManager, mockCdecArtifact);
        AdditionalNodesConfigUtil config = manager.getNodesConfigUtil(this.config);
        assertNotNull(config);
    }

    @Test
    public void testShellAgentCommand() throws Exception {
        prepareMultiNodeEnv(configManager);
        Command command = spyManager.getShellAgentCommand("test", TEST_NODE);
        assertEquals(command.toString(), format("{'command'='test', " +
                                                "'agent'='{'host'='localhost', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
    }
}
