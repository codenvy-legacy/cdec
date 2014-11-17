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

import com.codenvy.im.utils.ConfigUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public abstract class Config {
    private Map<String, String> properties = new HashMap<>();

    private String configSource;

    public final void load(InputStream in, String configSource) throws ConfigException {
        this.properties = ConfigUtils.readProperties(in);
        this.configSource = configSource;
    }

    public final void store(OutputStream out) throws ConfigException {
        ConfigUtils.storeProperties(this.properties, out);
    }

    public String getConfigSource() {
        return configSource;
    }

    protected final String getProperty(ConfigProperty property) throws ConfigException {
        String propertyName = property.toString().toLowerCase();
        if (! properties.containsKey(propertyName)) {
            if (configSource != null) {
                throw new ConfigException(format("Property '%s' hasn't been found at '%s'.", propertyName, getConfigSource()));
            } else {
                throw new ConfigException(format("Property '%s' hasn't been found.", propertyName));
            }
        }

        return properties.get(propertyName);
    }

    protected final void setProperty(ConfigProperty property, String value) {
        String propertyName = property.toString().toLowerCase();
        properties.put(propertyName, value);
    }
}
