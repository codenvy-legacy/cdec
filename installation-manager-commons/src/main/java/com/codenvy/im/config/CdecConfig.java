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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class CdecConfig {
    private List<PuppetConfig> puppetClients = new ArrayList<>();
    private PuppetMasterConfig puppetMaster;
    private PuppetConfig       puppetClient;

    private final static String DEFAULT_BASE_DIR = CdecConfig.class.getClassLoader().getResource("codenvy/cdec").getPath();  // TODO read path from installation-manager.properties
    private final static String PUPPET_CLIENT_CONFIG_FILE = "puppet-client.properties";

    public CdecConfig() {
        Path baseDir = FileSystems.getDefault().getPath(DEFAULT_BASE_DIR);

        puppetClient = new PuppetConfig();
        Path configFile = baseDir.resolve(PUPPET_CLIENT_CONFIG_FILE);
//        puppetClient.load(configFile);   // TODO

        // TODO load puppetMaster configurations
        // TODO load puppetClients configurations
    }

    public List<PuppetConfig> getPuppetClients() {
        return puppetClients;
    }

    public PuppetMasterConfig getPuppetMaster() {
        return puppetMaster;
    }

    public PuppetConfig getPuppetClient() {
        return puppetClient;
    }
}
