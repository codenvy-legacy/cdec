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

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class AdditionalNodesConfigUtil {

    /** Names of properties of Config.MULTI_SERVER_PROPERTIES config file */
    private static final Map<NodeConfig.NodeType, String> ADDITIONAL_NODES_CODENVY_PROPERTIES = ImmutableMap.of(
            NodeConfig.NodeType.BUILDER, "additional_builders",
            NodeConfig.NodeType.RUNNER, "additional_runners"
                                                                                                               );

    public static final String ADDITIONAL_NODE_URL_TEMPLATE = "http://%1$s:8080/%2$s/internal/%2$s";

    private Config config;

    public AdditionalNodesConfigUtil(Config config) {
        this.config = config;
    }

    /**
     * Read all urls of additional nodes stored from the puppet master config, find out node with certain dns and return type of additional node with
     * certain dns.
     * For example: given:
     * $additional_builders = "http://builder2.example.com:8080/builder/internal/builder,http://builder3.example.com:8080/builder/internal/builder"
     * dns = "builder3.example.com"
     * Result = BUILDER
     */
    @Nullable
    public NodeConfig.NodeType recognizeNodeTypeFromConfigBy(String dns) {
        for (Map.Entry<NodeConfig.NodeType, String> entry : ADDITIONAL_NODES_CODENVY_PROPERTIES.entrySet()) {
            String additionalNodesProperty = entry.getValue();
            String additionalNodes = config.getValue(additionalNodesProperty);

            if (additionalNodes != null && additionalNodes.contains(dns)) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Iterate through registered additional node types to find type which = prefix of dns, and then return NodeConfig(found_type, dns).
     * For example: given:
     * dns = "builder2.dev.com"
     * $builder_host_name = "builder1.dev.com"  => base_node_domain = ".dev.com"
     * Result = new NodeConfig(BUILDER, "builder2.dev.com")
     * <p/>
     * Example 2: given:
     * dns = "builder2.dev.com"
     * $builder_host_name = "builder1.example.com"  => base_node_domain = ".example.com"  != ".dev.com"
     * Result = IllegalArgumentException("Illegal DNS name 'builder2.dev.com' of additional node....)
     *
     * @throws IllegalArgumentException
     *         if dns doesn't comply with convention '<supported_node_type><number>(base_node_domain)' where supported prefix
     */
    public NodeConfig recognizeNodeConfigFromDns(String dns) throws IllegalArgumentException, IllegalStateException {
        for (Map.Entry<NodeConfig.NodeType, String> entry : ADDITIONAL_NODES_CODENVY_PROPERTIES.entrySet()) {
            NodeConfig.NodeType type = entry.getKey();

            NodeConfig baseNode = NodeConfig.extractConfigFrom(config, type);
            if (baseNode == null) {
                throw new IllegalStateException(format("Host name of base node of type '%s' wasn't found.", type));
            }

            String typeString = type.toString().toLowerCase();
            String base_node_domain = ConfigManager.getBaseNodeDomain(baseNode).toLowerCase();
            String regex = format("^%s\\d+%s$",
                                  typeString,
                                  base_node_domain);

            if (dns != null && dns.toLowerCase().matches(regex)) {
                return new NodeConfig(type, dns, null);
            }
        }

        throw new IllegalArgumentException(format("Illegal DNS name '%s' of additional node. " +
                                                  "Correct name template is '<prefix><number><base_node_domain>' where supported prefix is one from" +
                                                  " the list '%s'",
                                                  dns,
                                                  ADDITIONAL_NODES_CODENVY_PROPERTIES.keySet().toString().toLowerCase()));
    }

    /**
     * @return name of property of puppet master config, which holds additional nodes of certain type.
     */
    @Nullable
    public String getPropertyNameBy(NodeConfig.NodeType nodeType) {
        return ADDITIONAL_NODES_CODENVY_PROPERTIES.get(nodeType);
    }

    /**
     * Construct url of adding node, add it to the list of additional nodes of type = addingNode.getType() of the configuration of puppet master,
     * and return this list as row with comma-separated values.
     * For example: given:
     * $additional_builders = "http://builder2.example.com:8080/builder/internal/builder"
     * addingNode = new NodeConfig(BUILDER, "builder3.example.com")
     * Result = "http://builder2.example.com:8080/builder/internal/builder,http://builder3.example.com:8080/builder/internal/builder"
     *
     * @throws IllegalArgumentException
     *         if there is adding node in the list of additional nodes
     * @throws IllegalStateException
     *         if additional nodes property isn't found in Codenvy config
     */
    public String getValueWithNode(NodeConfig addingNode) throws IllegalArgumentException, IllegalStateException {
        String additionalNodesProperty = getPropertyNameBy(addingNode.getType());
        List<String> nodesUrls = config.getAllValues(additionalNodesProperty);
        if (nodesUrls == null) {
            throw new IllegalStateException(format("Additional nodes property '%s' isn't found in Codenvy config", additionalNodesProperty));
        }

        String nodeUrl = getAdditionalNodeUrl(addingNode);
        if (nodesUrls.contains(nodeUrl)) {
            throw new IllegalArgumentException(format("Node '%s' has been already used", addingNode.getHost()));
        }

        nodesUrls.add(nodeUrl);

        return on(',').skipNulls().join(nodesUrls);
    }

    /**
     * Erase url of removing node from the list of additional nodes of type = removingNode.getType() of the configuration of puppet master,
     * and return this list as row with comma-separated values.
     * For example: given:
     * $additional_builders = "http://builder2.example.com:8080/builder/internal/builder,http://builder3.example.com:8080/builder/internal/builder"
     * removingNode = new NodeConfig(BUILDER, "builder3.example.com")
     * Result = "http://builder2.example.com:8080/builder/internal/builder"
     *
     * @throws IllegalArgumentException
     *         if there is no removing node in the list of additional nodes
     * @throws IllegalStateException
     *         if additional nodes property isn't found in Codenvy config
     */
    public String getValueWithoutNode(NodeConfig removingNode) throws IllegalArgumentException {
        String additionalNodesProperty = getPropertyNameBy(removingNode.getType());
        List<String> nodesUrls = config.getAllValues(additionalNodesProperty);
        if (nodesUrls == null) {
            throw new IllegalStateException(format("Additional nodes property '%s' isn't found in Codenvy config", additionalNodesProperty));
        }

        String nodeUrl = getAdditionalNodeUrl(removingNode);
        if (!nodesUrls.contains(nodeUrl)) {
            throw new IllegalArgumentException(format("There is no node '%s' in the list of additional nodes", removingNode.getHost()));
        }

        nodesUrls.remove(nodeUrl);

        return on(',').skipNulls().join(nodesUrls);
    }

    /**
     * @return for given additional node type: Map[{additionalNodesPropertyName}, {List<String> of additionalNodesDns}]
     */
    @Nullable
    public Map<String, List<String>> extractAdditionalNodesDns(NodeConfig.NodeType nodeType) {
        Map<String, List<String>> result = new HashMap<>();

        String additionalNodesProperty = getPropertyNameBy(nodeType);
        List<String> nodesUrls = config.getAllValues(additionalNodesProperty);
        if (nodesUrls == null) {
            return null;
        }

        List<String> nodesDns = new ArrayList<>();
        for (String nodeUrl: nodesUrls) {
            String nodeDns = getAdditionalNodeDns(nodeUrl);
            if (nodeDns != null) {
                nodesDns.add(nodeDns);
            }
        }

        result.put(additionalNodesProperty, nodesDns);

        return result;
    }

    /**
     * @return link like "http://builder3.example.com:8080/builder/internal/builder", or "http://runner3.example.com:8080/runner/internal/runner"
     * For example: given:
     * node = new NodeConfig(BUILDER, "builder2.example.com")
     * Result = "http://builder2.example.com:8080/builder/internal/builder"
     */
    protected String getAdditionalNodeUrl(NodeConfig node) {
        return format(ADDITIONAL_NODE_URL_TEMPLATE,
                      node.getHost(),
                      node.getType().toString().toLowerCase()
                     );
    }

    /**
     * @return dns name like "builder3.example.com"
     * For example: given:
     * nodeUrl = "http://builder2.example.com:8080/builder/internal/builder"
     * Result = builder2.example.com
     */
    @Nullable
    private String getAdditionalNodeDns(String nodeUrl) {
        String regex = "^http(|s)://([^:/]+)";
        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(nodeUrl);
        if (m.find()) {
            return m.group().replaceAll("^http(|s)://", "");
        }

        return null;
    }
}
