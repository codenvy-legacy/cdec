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

import java.util.Map;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class CdecConfig extends Config {

    public CdecConfig(Map<String, String> properties) {
        super(properties);
    }

    public enum Property implements ConfigProperty {
        HOST,
        SSH_PORT,
        USER,
        PASSWORD,
        PRIVATE_KEY_FILE_ABSOLUTE_PATH,
        PUPPET_VERSION,
        PUPPET_RESOURCE_URL,
        PUPPET_MASTER_PORT
    }


    public String getHost() throws ConfigException {
        return getProperty(Property.HOST);
    }

    public String getSSHPort() throws ConfigException {
        return getProperty(Property.SSH_PORT);
    }

    public String getUser() throws ConfigException {
        return getProperty(Property.USER);
    }

    public String getPassword() throws ConfigException {
        return getProperty(Property.PASSWORD);
    }

    public String getPrivateKeyFileAbsolutePath() throws ConfigException {
        return getProperty(Property.PRIVATE_KEY_FILE_ABSOLUTE_PATH);
    }

    public String getPuppetVersion() throws ConfigException {
        return getProperty(Property.PUPPET_VERSION);
    }

    public String getPuppetResourceUrl() throws ConfigException {
        return getProperty(Property.PUPPET_RESOURCE_URL);
    }

    public String getPuppetMasterPort() throws ConfigException {
        return getProperty(Property.PUPPET_MASTER_PORT);
    }
}
