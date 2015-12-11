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
package com.codenvy.im.managers.helper;

import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableList;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/** @author Dmytro Nochevnov */
public class TestAdditionalNodesConfigHelperCodenvy3 {

    @Mock
    private Config mockConfig;

    private static final String              TEST_NODE_DNS  = "localhost";
    private static final NodeConfig.NodeType TEST_NODE_TYPE = NodeConfig.NodeType.RUNNER;
    private static final NodeConfig          TEST_NODE      = new NodeConfig(TEST_NODE_TYPE, TEST_NODE_DNS, null);

    private static final String ADDITIONAL_RUNNERS_PROPERTY_NAME = "additional_runners";

    private AdditionalNodesConfigHelperCodenvy3Impl spyConfigUtil;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        spyConfigUtil = spy(new AdditionalNodesConfigHelperCodenvy3Impl(mockConfig));
    }

    @Test
    public void testRecognizeNodeTypeBy() {
        doReturn(TEST_NODE_DNS).when(mockConfig).getValueWithoutSubstitution(ADDITIONAL_RUNNERS_PROPERTY_NAME);
        NodeConfig.NodeType result = spyConfigUtil.recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);

        assertEquals(result, NodeConfig.NodeType.RUNNER);
    }

    @Test
    public void testRecognizeNodeTypeFail() {
        NodeConfig.NodeType result = spyConfigUtil.recognizeNodeTypeFromConfigBy(TEST_NODE_DNS);
        assertNull(result);
    }

    @Test
    public void testGetPropertyNameBy() {
        assertEquals(spyConfigUtil.getPropertyNameBy(NodeConfig.NodeType.RUNNER),
                     ADDITIONAL_RUNNERS_PROPERTY_NAME);
        assertEquals(spyConfigUtil.getPropertyNameBy(NodeConfig.NodeType.BUILDER),
                     "additional_builders");
    }

    @Test(dataProvider = "GetValueWithNode")
    public void testGetValueWithNode(List<String> additionalNodes, String addingNodeDns, String expectedResult) {
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                                String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.RUNNER, addingNodeDns, null);

        String result = spyConfigUtil.getValueWithNode(testNode);
        assertEquals(result, expectedResult);
    }

    @DataProvider(name = "GetValueWithNode")
    public static Object[][] GetValueWithNode() {
        return new Object[][]{
            {new ArrayList(), "test", "http://test:8080/runner/internal/runner"},
            {new ArrayList<>(ImmutableList.of("test1")), "test2", "test1,http://test2:8080/runner/internal/runner"}
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Node '" + TEST_NODE_DNS + "' has been already used")
    public void testGetValueWithNodeWhenNodeExists() {
        List<String> additionalNodes = new ArrayList<>();
        additionalNodes.add(String.format("http://%s:8080/runner/internal/runner", TEST_NODE_DNS));
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                                String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));

        spyConfigUtil.getValueWithNode(TEST_NODE);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Additional nodes property '" + ADDITIONAL_RUNNERS_PROPERTY_NAME + "' isn't found in Codenvy config")
    public void testGetValueWithNodeWithoutAdditionalNodesProperty() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                     String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));
        spyConfigUtil.getValueWithNode(TEST_NODE);
    }

    @Test(dataProvider = "GetValueWithoutNode")
    public void testGetValueWithoutNode(List<String> additionalNodes, String removingNodeDns, String expectedResult) {
        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                                String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));
        NodeConfig testNode = new NodeConfig(NodeConfig.NodeType.RUNNER, removingNodeDns, null);

        String result = spyConfigUtil.getValueWithoutNode(testNode);
        assertEquals(result, expectedResult);
    }

    @DataProvider(name = "GetValueWithoutNode")
    public static Object[][] GetValueWithoutNode() {
        return new Object[][]{
                {new ArrayList<>(ImmutableList.of("http://test:8080/runner/internal/runner")), "test", ""},
                {new ArrayList<>(ImmutableList.of(
                                      "http://test1:8080/runner/internal/runner",
                                      "http://test2:8080/runner/internal/runner",
                                      "http://test3:8080/runner/internal/runner"
                                  )),
                                  "test1",
                                  "http://test2:8080/runner/internal/runner,http://test3:8080/runner/internal/runner"},
                {new ArrayList<>(ImmutableList.of(
                                      "http://test1:8080/runner/internal/runner",
                                      "http://test2:8080/runner/internal/runner",
                                      "http://test3:8080/runner/internal/runner"
                                  )),
                                  "test2",
                                  "http://test1:8080/runner/internal/runner,http://test3:8080/runner/internal/runner"},
                {new ArrayList<>(ImmutableList.of(
                                "http://test1:8080/runner/internal/runner",
                                "http://test2:8080/runner/internal/runner",
                                "http://test3:8080/runner/internal/runner"
                            )),
                             "test3",
                             "http://test1:8080/runner/internal/runner,http://test2:8080/runner/internal/runner"}
        };
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "There is no node '" + TEST_NODE_DNS + "' in the list of additional nodes")
    public void testGetValueWithoutNodeWhenNodeIsNotExists() {
        doReturn(new ArrayList<>()).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                                  String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));

        spyConfigUtil.getValueWithoutNode(TEST_NODE);
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Additional nodes property '" + ADDITIONAL_RUNNERS_PROPERTY_NAME + "' isn't found in Codenvy config")
    public void testGetValueWithoutNodeWithoutAdditionalNodesProperty() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                     String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));
        spyConfigUtil.getValueWithoutNode(TEST_NODE);
    }

    @Test(dataProvider = "GetAdditionalNodeUrl")
    public void testGetAdditionalNodeUrl(NodeConfig node, String expectedResult) {
        assertEquals(spyConfigUtil.getAdditionalNodeUrl(node), expectedResult);
    }

    @DataProvider(name = "GetAdditionalNodeUrl")
    public static Object[][] GetAdditionalNodeUrl() {
        return new Object[][]{
            {new NodeConfig(NodeConfig.NodeType.RUNNER, TEST_NODE_DNS, null), "http://localhost:8080/runner/internal/runner"},
            {new NodeConfig(NodeConfig.NodeType.BUILDER, TEST_NODE_DNS, null), "http://localhost:8080/builder/internal/builder"},
            {new NodeConfig(NodeConfig.NodeType.SITE, TEST_NODE_DNS, null), "http://localhost:8080/site/internal/site"},
        };
    }

    @Test(dataProvider = "RecognizeNodeConfigFromDns")
    public void testRecognizeNodeConfigFromDns(String dns, String baseNodeDomain, NodeConfig expectedResult) {
        doReturn(baseNodeDomain).when(mockConfig).getValue(NodeConfig.NodeType.BUILDER.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
        doReturn(baseNodeDomain).when(mockConfig).getValue(NodeConfig.NodeType.RUNNER.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
        assertEquals(spyConfigUtil.recognizeNodeConfigFromDns(dns), expectedResult);
    }

    @DataProvider(name = "RecognizeNodeConfigFromDns")
    public static Object[][] RecognizeNodeConfigFromDns() {
        return new Object[][]{
            {"runner123.dev.com", "runner1.dev.com", new NodeConfig(NodeConfig.NodeType.RUNNER, "runner123.dev.com", null)},
            {"builder123.com", "builder1.com", new NodeConfig(NodeConfig.NodeType.BUILDER, "builder123.com", null)}
        };
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = "Host name of base node of type 'BUILDER' wasn't found.")
    public void testRecognizeNodeConfigFromDnsWhenBaseNodeDomainUnknown() {
        spyConfigUtil.recognizeNodeConfigFromDns("some");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Illegal DNS name 'runner2.another.com' of additional node. Correct name template is '<prefix><number><base_node_domain>' where supported prefix is one from the list '.*'")
    public void testRecognizeNodeConfigFromDnsWhenDnsDoesNotComplyBaseNodeDomain() {
        doReturn("builder1.some.com").when(mockConfig).getValue(
                NodeConfig.NodeType.BUILDER.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
        doReturn("runner1.some.com").when(mockConfig).getValue(NodeConfig.NodeType.RUNNER.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX);
        spyConfigUtil.recognizeNodeConfigFromDns("runner2.another.com");
    }

    @Test
    public void testExtractAdditionalNodesDns() {
        ArrayList additionalNodes = new ArrayList<>(ImmutableList.of(
            "http://test1.dev.com/runner/internal/runner",
            "http://test-2.dev.com:8080/runner/internal/runner",
            "https://test3.dev.com:8080/runner/internal/runner",
            "wrong_address"
        ));

        doReturn(additionalNodes).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                                String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));

        Map<String, List<String>> result = spyConfigUtil.extractAdditionalNodesDns(NodeConfig.NodeType.RUNNER);
        assertEquals(result.toString(), "{additional_runners=[test1.dev.com, test-2.dev.com, test3.dev.com]}");
    }

    @Test
    public void testExtractAdditionalNodesDnsWhenPropertiesIsAbsent() {
        doReturn(null).when(mockConfig).getAllValues(ADDITIONAL_RUNNERS_PROPERTY_NAME,
                                                     String.valueOf(AdditionalNodesConfigHelperCodenvy3Impl.ADDITIONAL_NODE_DELIMITER));

        Map<String, List<String>> result = spyConfigUtil.extractAdditionalNodesDns(NodeConfig.NodeType.RUNNER);
        assertNull(result);
    }
}
