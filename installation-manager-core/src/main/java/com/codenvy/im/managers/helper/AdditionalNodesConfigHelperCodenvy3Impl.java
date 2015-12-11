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
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.NodeConfig;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class AdditionalNodesConfigHelperCodenvy3Impl extends AdditionalNodesConfigHelper {

    private static final Map<NodeConfig.NodeType, String> ADDITIONAL_NODES_CODENVY_PROPERTIES = ImmutableMap.of(
        NodeConfig.NodeType.BUILDER, Config.ADDITIONAL_BUILDERS,
        NodeConfig.NodeType.RUNNER, Config.ADDITIONAL_RUNNERS
                                                                                                               );

    private static final String ADDITIONAL_NODE_URL_TEMPLATE = "http://%1$s:8080/%2$s/internal/%2$s";

    private static final String ADDITIONAL_NODE_REGEX = "^http(|s)://";

    public static final char ADDITIONAL_NODE_DELIMITER = ',';

    public AdditionalNodesConfigHelperCodenvy3Impl(Config config) {
        super(config);
    }

    @Override public String getAdditionalNodeRegex() {
        return ADDITIONAL_NODE_REGEX;
    }

    @Override
    public String getAdditionalNodeTemplate() {
        return ADDITIONAL_NODE_URL_TEMPLATE;
    }

    @Override
    public Map<NodeConfig.NodeType, String> getAdditionalNodesCodenvyProperties() {
        return ADDITIONAL_NODES_CODENVY_PROPERTIES;
    }

    @Override
    public char getAdditionalNodeDelimiter() {
        return ADDITIONAL_NODE_DELIMITER;
    }

    @Override
    public String getBaseNodeDomain(NodeConfig.NodeType type, Config config) {
        NodeConfig baseNode = NodeConfig.extractConfigFrom(config, type);
        if (baseNode == null) {
            throw new IllegalStateException(format("Host name of base node of type '%s' wasn't found.", type));
        }

        return ConfigManager.getBaseNodeDomain(baseNode).toLowerCase();
    }

}
