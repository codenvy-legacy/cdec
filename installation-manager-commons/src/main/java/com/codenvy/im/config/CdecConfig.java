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
        PUPPET_VERSION,
        PUPPET_RESOURCE_URL,
    }

    public String getPuppetVersion() throws ConfigException {
        return getProperty(Property.PUPPET_VERSION);
    }

    public String getPuppetResourceUrl() throws ConfigException {
        return getProperty(Property.PUPPET_RESOURCE_URL);
    }

    /** {@inheritDoc} */
    @Override
    public void validate() throws IllegalStateException {
        for (ConfigProperty property : Property.values()) {
            if (getProperty(property) == null) {
                throw new IllegalStateException(String.format("Property '%s' is missed in the configuration", property.toString().toLowerCase()));
            }
        }
    }
}
