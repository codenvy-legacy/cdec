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

import com.codenvy.im.artifacts.CDECArtifact;

import java.nio.file.Paths;

import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;

/**
 * @author Dmytro Nochevnov
 */
public class ConfigFactory {
    private static final String DEFAULT_BASE_DIR = "codenvy";   // TODO get path from installation-manager.properties
    private static final String SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES = "cdec/single-node-without-puppet-master.properties";

    /**
     * Config factory.
     */
    public static CdecConfig loadConfig(CDECArtifact.InstallType installType) {
        switch (installType) {
            case SINGLE_NODE_WITHOUT_PUPPET_MASTER:
                String configFilePath = Paths.get(DEFAULT_BASE_DIR).resolve(SINGLE_NODE_WITHOUT_PUPPET_MASTER_PROPERTIES).toString();
                return loadConfig(configFilePath, CdecConfig.class);
        }

        throw new IllegalArgumentException("CDEC config of type '" + installType + "' isn't found");
    }

    private static <T extends Config> T loadConfig(String configFilePath, Class<T> clazz) {
        T config = INJECTOR.getInstance(clazz);
        config.load(ConfigFactory.class.getClassLoader().getResourceAsStream(configFilePath));   // TODO load from file system
        return config;
    }
}
