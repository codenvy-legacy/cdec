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
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class AdditionalNodesConfigHelperCodenvy4Impl extends AdditionalNodesConfigHelper {

    private static final List<AdditionalNode> ADDITIONAL_NODES = ImmutableList.of(AdditionalNode.MACHINE);

    private static final String ADDITIONAL_NODE_URL_TEMPLATE = "%1$s:2375";

    private static final String ADDITIONAL_NODE_REGEX = "^";

    public static final char ADDITIONAL_NODE_DELIMITER = '\n';


    public AdditionalNodesConfigHelperCodenvy4Impl(Config config) {
        super(config);
    }

    /**
     * @return node with dns = ".host_url"
     */
    @Override
    public String getBaseNodeDomain(NodeConfig.NodeType type, Config config) {
        String baseNode = config.getHostUrl();
        if (baseNode == null) {
            throw new IllegalStateException(format("Host name of base node of type '%s' wasn't found.", type));
        }

        return "." + baseNode;
    }

    @Override
    public String getAdditionalNodeRegex() {
        return ADDITIONAL_NODE_REGEX;
    }

    @Override
    public String getAdditionalNodeTemplate() {
        return ADDITIONAL_NODE_URL_TEMPLATE;
    }

    @Override
    public List<AdditionalNode> getAdditionalNodes() {
        return ADDITIONAL_NODES;
    }

    @Override
    public char getAdditionalNodeDelimiter() {
        return ADDITIONAL_NODE_DELIMITER;
    }
}
