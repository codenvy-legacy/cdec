/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.im.config;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/** @author Dmytro Nochevnov */
public class NodeConfig {
    public enum NodeType {
        DATA(),
        API,
        SITE,
        RUNNER,
        BUILDER,
        DATASOURCE,
        ANALYTICS
    }

    private String host;
    private int port = 22;
    private String user;
    private String privateKeyFile = "~/.ssh/id_rsa";  // there should be absolute path to file
    private NodeType type;

    public NodeConfig(NodeType type, String host) {
        this.type = type;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * @throws IllegalArgumentException if the port = 0.
     */
    public NodeConfig setPort(int port) throws IllegalArgumentException {
        if (port == 0) {
            throw new IllegalArgumentException("Port 0 isn't supported.");
        }

        this.port = port;
        return this;
    }

    public String getUser() {
        return user;
    }

    public NodeConfig setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public NodeConfig setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    public NodeType getType() {
        return type;
    }

    /**
     * Return default node config of all types of node where just the type and host={NodeType}.{host_url} are set according to the existent types of nodes
     */
    public static List<NodeConfig> extractConfigsFrom(Config config) {
        List<NodeConfig> nodeConfigs = new ArrayList<>();
        for (NodeType type: NodeType.values()) {
            String nodeHostPropertyName = type.toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX;
            String nodeHost = config.getProperty(nodeHostPropertyName);
            if ((nodeHost != null) && (!nodeHost.isEmpty())) {
                nodeConfigs.add(new NodeConfig(type, nodeHost));
            }
        }

        return nodeConfigs;
    }

    @Override
    public String toString() {
        return format("{'host':'%1$s', 'port':'%2$s', 'user':'%3$s', 'privateKeyFile':'%4$s', 'type':'%5$s'}",
                      host,
                      port,
                      user,
                      privateKeyFile,
                      type);
    }
}
