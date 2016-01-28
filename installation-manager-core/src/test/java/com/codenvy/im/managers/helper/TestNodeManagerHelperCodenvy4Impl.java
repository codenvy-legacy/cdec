/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.im.managers.helper;

import com.codenvy.im.BaseTest;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandLibrary;
import com.codenvy.im.commands.MacroCommand;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.NodeException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestNodeManagerHelperCodenvy4Impl extends BaseTest {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    @Mock
    private ConfigManager               mockConfigManager;
    @Mock
    private AdditionalNodesConfigHelper mockNodesConfigHelper;
    @Mock
    private Command                     mockCommand;

    private static final String              TEST_NODE_DNS  = "node1.hostname";
    private static final NodeConfig.NodeType TEST_NODE_TYPE = NodeConfig.NodeType.MACHINE;
    private static final NodeConfig          TEST_NODE      = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS);

    private static final String ADDITIONAL_NODES_PROPERTY_NAME = Config.SWARM_NODES;
    private static final String TEST_VALUE_WITH_NODE           = TEST_NODE_DNS + ":2375";

    private NodeManagerHelperCodenvy4Impl spyHelperCodenvy4;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyHelperCodenvy4 = spy(new NodeManagerHelperCodenvy4Impl(mockConfigManager));

        doReturn(ImmutableList.of(Paths.get("/etc/puppet/" + Config.SINGLE_SERVER_4_0_PROPERTIES)).iterator())
            .when(mockConfigManager).getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);

        doReturn(mockNodesConfigHelper).when(spyHelperCodenvy4).getNodesConfigHelper(any(Config.class));

        initConfigs();
    }

    private void initConfigs() throws IOException {
        doReturn(new Config(ImmutableMap.of("host_url", "hostname",
                                            Config.VERSION, "4.0.0",
                                            Config.SWARM_NODES, "$host_url:2375"))).when(mockConfigManager).loadInstalledCodenvyConfig();
        doReturn(InstallType.SINGLE_SERVER).when(mockConfigManager).detectInstallationType();
        doReturn(TEST_VALUE_WITH_NODE).when(mockNodesConfigHelper).getValueWithNode(TEST_NODE);
    }

    @Test
    public void testGetAddNodeCommand() throws Exception {
        Command result = spyHelperCodenvy4.getAddNodeCommand(TEST_NODE, ADDITIONAL_NODES_PROPERTY_NAME);
        assertNotNull(result);
        assertTrue(result instanceof MacroCommand);

        List<Command> commands = ((MacroCommand) result).getCommands();
        assertEquals(commands.size(), 15);

        assertEquals(commands.get(0).toString(), "{'command'='if [[ \"$(grep \"node1.hostname\" /etc/puppet/autosign.conf)\" == \"\" ]]; then sudo sh -c \"echo -e 'node1.hostname' >> /etc/puppet/autosign.conf\"; fi', "
                                                 + "'agent'='LocalAgent'}");
        assertEquals(commands.get(1).toString(), "{'command'='yum clean all', 'agent'='LocalAgent'}");
        assertEquals(commands.get(2).toString(), format("{'command'='if [[ \"$(yum list installed | grep puppetlabs-release)\" == \"\" ]]; then   sudo yum -y -q install https://yum.puppetlabs.com/el/7/products/x86_64/puppetlabs-release-7-11.noarch.rpm; fi', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(3).toString(), format("{'command'='sudo yum -y -q install puppet-3.5.1-1.el7.noarch', "
                                                        + "'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(4).toString(), format("{'command'='sudo systemctl enable puppet', "
                                                        + "'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertTrue(commands.get(5).toString().matches(".*'command'='sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back ; "
                                                      + "sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back.[0-9]+.*"), "Actual result: " + commands.get(5).toString());
        assertEquals(commands.get(6).toString(), format("{'command'='if [[ \"$(grep \"server = hostname\" /etc/puppet/puppet.conf)\" == \"\" ]]; then   sudo sed -i 's/\\[main\\]/\\[main\\]\\n    server = hostname\\n    runinterval = 420\\n    configtimeout = 600\\n/g' /etc/puppet/puppet.conf;   sudo sed -i 's/\\[agent\\]/\\[agent\\]\\n    show_diff = true\\n    pluginsync = true\\n    report = true\\n    default_schedules = false\\n    certname = node1.hostname\\n/g' /etc/puppet/puppet.conf; fi', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(7).toString(), format("{'command'='if [[ \"$(grep \"PUPPET_EXTRA_OPTS=--logdest /var/log/puppet/puppet-agent.log\" /etc/sysconfig/puppetagent)\" == \"\" ]]; then   sudo sh -c 'echo -e \"\\nPUPPET_EXTRA_OPTS=--logdest /var/log/puppet/puppet-agent.log\" >> /etc/sysconfig/puppetagent'; fi', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(8).toString(), format("{'command'='sudo systemctl start puppet', "
                                                        + "'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(9).toString(), format("{'command'='sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay --logdest=/var/log/puppet/puppet-agent.log; exit 0;', "
                                                        + "'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(commands.get(10).toString(), format("PuppetErrorInterrupter{ {'command'='doneState=\"Checking\"; while [ \"${doneState}\" != \"Done\" ]; do     sudo service docker status | grep 'Active: active (running)';     if [ $? -eq 0 ]; then doneState=\"Done\";     else sleep 5;     fi; done', " +
                                                         "'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'} }; looking on errors in file /var/log/puppet/puppet-agent.log locally and at the nodes: " + 
                                                         "[{'host':'node1.hostname', 'port':'22', 'privateKeyFile':'~/.ssh/id_rsa', 'type':'MACHINE'}]", SYSTEM_USER_NAME));
        assertTrue(commands.get(11).toString().matches("\\{'command'='sudo cp /etc/puppet/manifests/nodes/codenvy/codenvy.pp /etc/puppet/manifests/nodes/codenvy/codenvy.pp.back ; "
                                                       + "sudo cp /etc/puppet/manifests/nodes/codenvy/codenvy.pp /etc/puppet/manifests/nodes/codenvy/codenvy.pp.back.[0-9]+ ; ', "
                                                       + "'agent'='LocalAgent'\\}"), "Actual result: " + commands.get(11).toString());
        assertEquals(commands.get(12).toString(), "{'command'='sudo cat /etc/puppet/manifests/nodes/codenvy/codenvy.pp | sed ':a;N;$!ba;s/\\n/~n/g' | sed 's|$swarm_nodes *= *\"[^\"]*\"|$swarm_nodes = \"node1.hostname:2375\"|g' | sed 's|~n|\\n|g' > tmp.tmp && sudo mv tmp.tmp /etc/puppet/manifests/nodes/codenvy/codenvy.pp', "
                                                  + "'agent'='LocalAgent'}");
        assertEquals(commands.get(13).toString(), "{'command'='sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay --logdest=/var/log/puppet/puppet-agent.log; exit 0;', 'agent'='LocalAgent'}");
        assertEquals(commands.get(14).toString(), "{'command'='doneState=\"Checking\"; while [ \"${doneState}\" != \"Done\" ]; do     curl http://hostname:23750/info | grep 'node1.hostname';     if [ $? -eq 0 ]; then doneState=\"Done\";     else sleep 5;     fi; done', "
                                                  + "'agent'='LocalAgent'}");
    }

    @Test
    public void testGetUpdatePuppetConfigCommandWhenNodeListIsEmpty() throws Exception {
        prepareSingleNodeEnv(mockConfigManager);
        Command result = spyHelperCodenvy4.getUpdatePuppetConfigCommand("hostname", "new.hostname");
        assertNotNull(result);
        assertEquals(result.getDescription(), CommandLibrary.EMPTY_COMMAND.getDescription());
    }

    @Test
    public void testGetUpdatePuppetConfigCommand() throws Exception {
        final String oldHostName = "hostname";
        final String newHostName = "new.hostname";
        prepareSingleNodeEnv(mockConfigManager);

        Map<String, String> properties = new HashMap<String, String>() {{
            put("host_url", "hostname");
            put(Config.VERSION, TEST_VERSION_STR);
            put(Config.SWARM_NODES, format("$host_url:2375\n"
                                           + "node1.%1$s:2375\n"
                                           + "node2.%1$s:2375", oldHostName));
        }};
        doReturn(new Config(properties)).when(mockConfigManager).loadInstalledCodenvyConfig();

        doReturn(ImmutableMap.of(Config.SWARM_NODES, ImmutableList.of("node1.hostname", "node2.hostname")))
            .when(mockNodesConfigHelper).extractAdditionalNodesDns(NodeConfig.NodeType.MACHINE);

        doReturn(Config.SWARM_NODES).when(mockNodesConfigHelper).getPropertyNameBy(NodeConfig.NodeType.MACHINE);

        Command result = spyHelperCodenvy4.getUpdatePuppetConfigCommand(oldHostName, newHostName);
        assertNotNull(result);
        List<Command> commands = ((MacroCommand) result).getCommands();
        assertEquals(commands.size(), 4);

        List<Command> updateNode1Commands = ((MacroCommand) commands.get(0)).getCommands();
        assertEquals(updateNode1Commands.size(), 4);
        assertTrue(updateNode1Commands.get(0).toString()
                           .matches(format(
                               "\\{'command'='sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back ; sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back.[0-9]+ ; ', 'agent'='\\{'host'='node1.hostname', 'port'='22', 'user'='%1$s', 'identity'='\\[~/.ssh/id_rsa\\]'\\}'\\}",
                               SYSTEM_USER_NAME)),
                   "Actual command: " + updateNode1Commands.get(0).toString());

        assertEquals(updateNode1Commands.get(1).toString(), format("{'command'='sudo sed -i 's/certname = hostname/certname = new.hostname/g' /etc/puppet/puppet.conf', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(updateNode1Commands.get(2).toString(), format("{'command'='sudo sed -i 's/server = hostname/server = new.hostname/g' /etc/puppet/puppet.conf', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(updateNode1Commands.get(3).toString(), format("{'command'='sudo grep \"dns_alt_names = .*,new.hostname.*\" /etc/puppet/puppet.conf; if [ $? -ne 0 ]; then sudo sed -i 's/dns_alt_names = .*/&,new.hostname/' /etc/puppet/puppet.conf; fi', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));

        assertEquals(commands.get(1).toString(), format("{'command'='sudo systemctl restart puppet', 'agent'='{'host'='node1.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));

        List<Command> updateNode2Commands = ((MacroCommand) commands.get(2)).getCommands();
        assertEquals(updateNode2Commands.size(), 4);
        assertTrue(updateNode2Commands.get(0).toString()
                                      .matches(format(
                                          "\\{'command'='sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back ; sudo cp /etc/puppet/puppet.conf /etc/puppet/puppet.conf.back.[0-9]+ ; ', 'agent'='\\{'host'='node2.hostname', 'port'='22', 'user'='%1$s', 'identity'='\\[~/.ssh/id_rsa\\]'\\}'\\}",
                                          SYSTEM_USER_NAME)),
                   "Actual command: " + updateNode2Commands.get(0).toString());

        assertEquals(updateNode2Commands.get(1).toString(), format("{'command'='sudo sed -i 's/certname = hostname/certname = new.hostname/g' /etc/puppet/puppet.conf', 'agent'='{'host'='node2.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(updateNode2Commands.get(2).toString(), format("{'command'='sudo sed -i 's/server = hostname/server = new.hostname/g' /etc/puppet/puppet.conf', 'agent'='{'host'='node2.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        assertEquals(updateNode2Commands.get(3).toString(), format("{'command'='sudo grep \"dns_alt_names = .*,new.hostname.*\" /etc/puppet/puppet.conf; if [ $? -ne 0 ]; then sudo sed -i 's/dns_alt_names = .*/&,new.hostname/' /etc/puppet/puppet.conf; fi', 'agent'='{'host'='node2.hostname', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
        
        assertEquals(commands.get(3).toString(), format("{'command'='sudo systemctl restart puppet', 'agent'='{'host'='node2.hostname', 'port'='22', 'user'='%s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testGetAddNodeCommandNodeException() throws Exception {
        doThrow(new RuntimeException("error")).when(mockConfigManager).getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        spyHelperCodenvy4.getAddNodeCommand(TEST_NODE, ADDITIONAL_NODES_PROPERTY_NAME);
    }

    @Test
    public void testGetRemoveNodeCommand() throws Exception {
        Command removeNodeCommand = spyHelperCodenvy4.getRemoveNodeCommand(TEST_NODE, ADDITIONAL_NODES_PROPERTY_NAME);

        List<Command> commands = ((MacroCommand) removeNodeCommand).getCommands();
        assertEquals(commands.size(), 4);

        assertTrue(commands.get(0).toString().matches("\\{'command'='sudo cp /etc/puppet/manifests/nodes/codenvy/codenvy.pp /etc/puppet/manifests/nodes/codenvy/codenvy.pp.back ; "
                                                      + "sudo cp /etc/puppet/manifests/nodes/codenvy/codenvy.pp /etc/puppet/manifests/nodes/codenvy/codenvy.pp.back.[0-9]+ ; ', "
                                                      + "'agent'='LocalAgent'\\}"), "Actual command: " + commands.get(0).toString());
        assertEquals(commands.get(1).toString(), "{'command'='sudo cat /etc/puppet/manifests/nodes/codenvy/codenvy.pp | "
                                                 + "sed ':a;N;$!ba;s/\\n/~n/g' | sed 's|$swarm_nodes *= *\"[^\"]*\"|$swarm_nodes = \"null\"|g' | "
                                                 + "sed 's|~n|\\n|g' > tmp.tmp && sudo mv tmp.tmp /etc/puppet/manifests/nodes/codenvy/codenvy.pp', "
                                                 + "'agent'='LocalAgent'}");
        assertEquals(commands.get(2).toString(), "{'command'='sudo puppet agent --onetime --ignorecache --no-daemonize --no-usecacheonfailure --no-splay --logdest=/var/log/puppet/puppet-agent.log; exit 0;', 'agent'='LocalAgent'}");
        assertEquals(commands.get(3).toString(), "{'command'='testFile=\"/usr/local/swarm/node_list\"; while true; do     if ! sudo grep \"node1.hostname\" ${testFile}; then break; fi;     sleep 5; done; ', 'agent'='LocalAgent'}");
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testGetRemoveNodeCommandNodeException() throws Exception {
        doThrow(new RuntimeException("error")).when(mockConfigManager).getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);
        spyHelperCodenvy4.getRemoveNodeCommand(TEST_NODE, ADDITIONAL_NODES_PROPERTY_NAME);
    }

    @Test
    public void shouldBeOkWhenInstallTypeIsMultiServerCodenvy() throws Exception {
        spyHelperCodenvy4.checkInstallType();
    }

    @Test
    public void shouldThrowExceptionWhenInstallTypeIsSingleServerCodenvy() throws Exception {
        spyHelperCodenvy4.checkInstallType();
    }

    @Test
    public void testRecognizeNodeTypeFromConfigBy() throws Exception {
        Config testConfig = new Config(ImmutableMap.of("host_url", "hostname",
                                                        Config.SWARM_NODES, format("$host_url:2375\n%s:2375", TEST_NODE_DNS)));
        doReturn(testConfig).when(mockConfigManager).loadInstalledCodenvyConfig();

        assertEquals(spyHelperCodenvy4.recognizeNodeTypeFromConfigBy(TEST_NODE_DNS), NodeConfig.NodeType.MACHINE);
    }

    @Test
    public void testGetPropertyNameBy() throws Exception {
        doReturn(ADDITIONAL_NODES_PROPERTY_NAME).when(mockNodesConfigHelper).getPropertyNameBy(NodeConfig.NodeType.RUNNER);
        assertEquals(spyHelperCodenvy4.getPropertyNameBy(NodeConfig.NodeType.RUNNER), ADDITIONAL_NODES_PROPERTY_NAME);
    }

    @Test
    public void testRecognizeNodeConfigFromDns() throws Exception {
        doReturn(TEST_NODE).when(mockNodesConfigHelper).recognizeNodeConfigFromDns(TEST_NODE_DNS);
        assertEquals(spyHelperCodenvy4.recognizeNodeConfigFromDns(TEST_NODE_DNS), TEST_NODE);
    }

    @Test
    public void testNodesConfigHelper() throws Exception {
        AdditionalNodesConfigHelper helper = spyHelperCodenvy4.getNodesConfigHelper(new Config(Collections.EMPTY_MAP));
        assertNotNull(helper);
    }
}
