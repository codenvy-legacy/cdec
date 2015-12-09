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

import com.codenvy.im.commands.Command;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;

import java.io.IOException;

/**
 * @author Dmytro Nochevnov
 */
public class NodeManagerHelperCodenvy4Impl extends NodeManagerHelper {

    public NodeManagerHelperCodenvy4Impl(ConfigManager configManager) {
        super(configManager);
    }

    @Override
    public Command getAddNodeCommand(NodeConfig node, String property) throws IOException {
        // TODO [ndp]
        return null;
    }

    @Override
    public Command getRemoveNodeCommand(NodeConfig node, String property) throws IOException {
        // TODO [ndp]
        return null;
    }

    @Override
    public void checkInstallType() throws IllegalStateException {
        // adding/removing nodes are supported in Single-Server and Multi-Server Codenvy
    }

    @Override public NodeConfig.NodeType recognizeNodeTypeFromConfigBy(String dns) throws IOException {
        return null;
    }

    @Override public String getPropertyNameBy(NodeConfig.NodeType nodeType) throws IOException {
        return null;
    }

    @Override public NodeConfig recognizeNodeConfigFromDns(String dns) throws IllegalArgumentException, IllegalStateException, IOException {
        return null;
    }

}
