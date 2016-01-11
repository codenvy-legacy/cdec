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
package com.codenvy.im.managers;

import com.codenvy.im.BaseTest;
import com.codenvy.im.agent.AgentException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.UnsupportedArtifactVersionException;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.managers.helper.NodeManagerHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestNodeManager extends BaseTest {
    public static final String SYSTEM_USER_NAME = System.getProperty("user.name");

    @Mock
    private ConfigManager     mockConfigManager;
    @Mock
    private CDECArtifact      mockCdecArtifact;
    @Mock
    private Command           mockCommand;
    @Mock
    private NodeManagerHelper mockHelperCodenvy;

    private static final String              TEST_NODE_DNS       = "localhost";
    private static final NodeConfig.NodeType TEST_NODE_TYPE      = NodeConfig.NodeType.RUNNER;
    private static final NodeConfig          TEST_NODE           = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS, null);

    private static final String ADDITIONAL_RUNNERS_PROPERTY_NAME = "additional_runners";

    private NodeManager spyManager;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyManager = spy(new NodeManager(mockConfigManager, mockCdecArtifact));

        doReturn(mockHelperCodenvy).when(spyManager).getHelper();

        doReturn(ImmutableList.of(Paths.get("/etc/puppet/" + Config.MULTI_SERVER_CUSTOM_CONFIG_PP),
                                  Paths.get("/etc/puppet/" + Config.MULTI_SERVER_BASE_CONFIG_PP)).iterator())
            .when(mockConfigManager).getCodenvyPropertiesFiles(InstallType.MULTI_SERVER);
        doReturn(ImmutableList.of(Paths.get("/etc/puppet/" + Config.SINGLE_SERVER_BASE_CONFIG_PP),
                                  Paths.get("/etc/puppet/" + Config.SINGLE_SERVER_PP)).iterator())
            .when(mockConfigManager).getCodenvyPropertiesFiles(InstallType.SINGLE_SERVER);

        initConfigs();
    }

    private void initConfigs() throws IOException {
        doReturn(ADDITIONAL_RUNNERS_PROPERTY_NAME).when(mockHelperCodenvy).getPropertyNameBy(TEST_NODE_TYPE);
    }

    @Test
    public void testAddNode() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        
        doNothing().when(spyManager).validate(TEST_NODE);
        doReturn(TEST_NODE).when(mockHelperCodenvy).recognizeNodeConfigFromDns(TEST_NODE_DNS);
        doReturn(mockCommand).when(mockHelperCodenvy)
                             .getAddNodeCommand(TEST_NODE, ADDITIONAL_RUNNERS_PROPERTY_NAME);

        assertEquals(spyManager.add(TEST_NODE_DNS), TEST_NODE);
        verify(spyManager).validate(TEST_NODE);
        verify(mockCommand).execute();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "This type of node isn't supported")
    public void testAddNodeWhichIsNotSupported() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        
        doReturn(TEST_NODE).when(mockHelperCodenvy).recognizeNodeConfigFromDns(TEST_NODE_DNS);
        doReturn(null).when(mockHelperCodenvy).getPropertyNameBy(TEST_NODE.getType());

        spyManager.add(TEST_NODE_DNS);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "error")
    public void testAddNodeWhenWrongInstallTypeException() throws Exception {
        prepareSingleNodeEnv(mockConfigManager);
        doThrow(new IllegalStateException("error")).when(mockHelperCodenvy).checkInstallType();
        spyManager.add(TEST_NODE_DNS);
    }

    @Test
    public void testRemoveNode() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        
        doReturn(TEST_NODE_TYPE).when(mockHelperCodenvy).recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);
        doReturn(mockCommand).when(mockHelperCodenvy)
                             .getRemoveNodeCommand(TEST_NODE, ADDITIONAL_RUNNERS_PROPERTY_NAME);
        doReturn(TEST_NODE).when(mockHelperCodenvy).recognizeNodeConfigFromDns(TEST_NODE_DNS);

        assertEquals(spyManager.remove(TEST_NODE_DNS), TEST_NODE);
        verify(mockCommand).execute();
    }

    @Test(expectedExceptions = NodeException.class,
          expectedExceptionsMessageRegExp = "Node 'localhost' is not found in Codenvy configuration")
    public void testRemoveNonExistsNodeError() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        doReturn(null).when(mockHelperCodenvy)
                             .recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);

        spyManager.remove(TEST_NODE_DNS);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "error")
    public void testRemoveNodeWhenWrongInstallTypeException() throws Exception {
        prepareSingleNodeEnv(mockConfigManager);
        doThrow(new IllegalStateException("error")).when(mockHelperCodenvy).checkInstallType();
        spyManager.remove(TEST_NODE_DNS);
    }

    @Test
    public void testValidateNode() throws AgentException, CommandException, NodeException {
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doReturn("").when(mockCommand).execute();
        spyManager.validate(TEST_NODE);
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testAddNodeWhenValidationCommandFailed() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        
        doReturn(ADDITIONAL_RUNNERS_PROPERTY_NAME).when(mockHelperCodenvy).getPropertyNameBy(TEST_NODE.getType());
        doReturn(TEST_NODE).when(mockHelperCodenvy).recognizeNodeConfigFromDns(TEST_NODE_DNS);

        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doThrow(new CommandException("error", null)).when(mockCommand).execute();

        spyManager.add(TEST_NODE_DNS);
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testValidateNodeCommandException() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        doReturn(mockCommand).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        doThrow(new CommandException("error", null)).when(mockCommand).execute();
        spyManager.validate(TEST_NODE);
    }

    @Test(expectedExceptions = NodeException.class, expectedExceptionsMessageRegExp = "error")
    public void testValidateNodeAgentException() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        doThrow(new AgentException("error")).when(spyManager).getShellAgentCommand("sudo ls", TEST_NODE);
        spyManager.validate(TEST_NODE);
    }

    @Test
    public void testShellAgentCommand() throws Exception {
        prepareMultiNodeEnv(mockConfigManager);
        Command command = spyManager.getShellAgentCommand("test", TEST_NODE);
        assertEquals(command.toString(), format("{'command'='test', " +
                                                "'agent'='{'host'='localhost', 'port'='22', 'user'='%1$s', 'identity'='[~/.ssh/id_rsa]'}'}", SYSTEM_USER_NAME));
    }

    @Test(expectedExceptions = UnsupportedArtifactVersionException.class,
        expectedExceptionsMessageRegExp = "Version '1.0.0' of artifact 'codenvy' is not supported")
    public void shouldThrowUnsupportedArtifactVersionExceptionWhenAdd() throws Exception {
        NodeManager manager = new NodeManager(mockConfigManager, mockCdecArtifact);
        doReturn(new Config(ImmutableMap.of(Config.VERSION, UNSUPPORTED_VERSION)))
            .when(mockConfigManager).loadInstalledCodenvyConfig();
        doReturn("codenvy").when(mockCdecArtifact).getName();

        manager.add(TEST_NODE_DNS);
    }

    @Test(expectedExceptions = UnsupportedArtifactVersionException.class,
        expectedExceptionsMessageRegExp = "Version '1.0.0' of artifact 'codenvy' is not supported")
    public void shouldThrowUnsupportedArtifactVersionExceptionWhenRemove() throws Exception {
        NodeManager manager = new NodeManager(mockConfigManager, mockCdecArtifact);
        doReturn(new Config(ImmutableMap.of(Config.VERSION, UNSUPPORTED_VERSION)))
            .when(mockConfigManager).loadInstalledCodenvyConfig();
        doReturn("codenvy").when(mockCdecArtifact).getName();

        manager.remove(TEST_NODE_DNS);
    }
}
