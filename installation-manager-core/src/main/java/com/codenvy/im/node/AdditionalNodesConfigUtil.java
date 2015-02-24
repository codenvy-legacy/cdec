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

import com.codenvy.im.config.Config;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class AdditionalNodesConfigUtil {

    private static final Map<NodeConfig.NodeType, String> ADDITIONAL_NODES_CODENVY_PROPERTIES = ImmutableMap.of(
        NodeConfig.NodeType.BUILDER, "additional_builders",
        NodeConfig.NodeType.RUNNER, "additional_runners"
    );

    public static final String ADDITIONAL_NODE_URL_TEMPLATE = "http://%1$s:8080/%2$s/internal/%2$s";

    private Config config;

    public AdditionalNodesConfigUtil(Config config) {
        this.config = config;
    }

    @Nullable
    public NodeConfig.NodeType recognizeNodeTypeBy(String dns) {
        for (Map.Entry<NodeConfig.NodeType, String> entry : ADDITIONAL_NODES_CODENVY_PROPERTIES.entrySet()) {
            String additionalNodesProperty = entry.getValue();
            String additionalNodes = config.getValue(additionalNodesProperty);

            if (additionalNodes != null && additionalNodes.contains(dns)) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Nullable
    public String getPropertyNameBy(NodeConfig.NodeType nodeType) {
        return ADDITIONAL_NODES_CODENVY_PROPERTIES.get(nodeType);
    }

    /**
     * @throws IllegalArgumentException if there is adding node in the list of additional nodes
     * @throws IllegalStateException if additional nodes property isn't found in Codenvy config
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

        return StringUtils.join(nodesUrls, ',');
    }

    /**
     * @throws IllegalArgumentException if there is no removing node in the list of additional nodes
     * @throws IllegalStateException if additional nodes property isn't found in Codenvy config
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

        return StringUtils.join(nodesUrls, ",");
    }

    /**
     * @return link like "http://builder3.example.com:8080/builder/internal/builder", or "http://runner3.example.com:8080/runner/internal/runner"
     */
    protected String getAdditionalNodeUrl(NodeConfig node) {
        return format(ADDITIONAL_NODE_URL_TEMPLATE,
                      node.getHost(),
                      node.getType().toString().toLowerCase()
        );
    }
}
