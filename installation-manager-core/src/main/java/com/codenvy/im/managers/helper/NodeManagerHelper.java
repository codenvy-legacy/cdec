/*
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

import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.managers.UnknownInstallationTypeException;
import org.eclipse.che.commons.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dmytro Nochevnov
 */
public abstract class NodeManagerHelper {
    protected ConfigManager configManager;

    public NodeManagerHelper(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public abstract Command getAddNodeCommand(NodeConfig node, String property) throws IOException;

    public abstract Command getRemoveNodeCommand(NodeConfig node,
                                                 String property) throws IOException;

    public abstract void checkInstallType() throws IllegalStateException, UnknownInstallationTypeException, IOException;

    /**
     * Read all urls of additional nodes stored from the puppet master config, find out node with certain dns and return type of additional node with
     * certain dns.
     */
    @Nullable
    public NodeConfig.NodeType recognizeNodeTypeFromConfigBy(String dns) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        return getNodesConfigHelper(config).recognizeNodeTypeFromConfigBy(dns);
    }

    /**
     * @return name of property of puppet master config, which holds additional nodes of certain type.
     */
    @Nullable
    public String getPropertyNameBy(NodeConfig.NodeType nodeType) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        return getNodesConfigHelper(config).getPropertyNameBy(nodeType);
    }

    /**
     * Iterate through registered additional node types to find type which = prefix of dns, and then return NodeConfig(found_type, dns).
     */
    public NodeConfig recognizeNodeConfigFromDns(String dns) throws IllegalArgumentException, IllegalStateException, IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        return getNodesConfigHelper(config).recognizeNodeConfigFromDns(dns);
    }

    public abstract AdditionalNodesConfigHelper getNodesConfigHelper(Config config);

    /** Update puppet.conf on additional nodes */
    public abstract Command getUpdatePuppetConfigCommand(String oldHostName, String newHostName) throws IOException;

    /** @return Map<NodeType, List<NodeDns>> */
    public abstract Map<String,List<String>> getNodes() throws IOException;

}
