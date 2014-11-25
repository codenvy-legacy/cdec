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

import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class AgentConfig extends Config {
    public enum Property implements ConfigProperty {
        HOST("127.0.0.1"),
        PORT("22"),
        USER("codenvy"),
        PRIVATE_KEY_FILE_ABSOLUTE_PATH("/home/codenvy/.ssh/id_rsa");

        private String defaultValue;

        Property() {
        }

        Property(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    public String getHost() throws ConfigException {
        return getProperty(Property.HOST);
    }

    public int getPort() throws ConfigException {
        try {
            return Integer.parseInt(getProperty(Property.PORT));
        } catch(NumberFormatException e) {
            throw new ConfigException(format("Incorrect value of property '%s'.", Property.PORT), e);
        }
    }

    public String getUser() throws ConfigException {
        return getProperty(Property.USER);
    }

    public String getPrivateKeyFileAbsolutePath() throws ConfigException {
        return getProperty(Property.PRIVATE_KEY_FILE_ABSOLUTE_PATH);
    }

    /** {@inheritDoc} */
    @Override
    public void validate() throws IllegalStateException {
        for (ConfigProperty property : Property.values()) {
            if (getProperty(property) == null) {
                throw new IllegalStateException(format("Property '%s' is missed in the configuration", property.toString().toLowerCase()));
            }
        }
    }
}
