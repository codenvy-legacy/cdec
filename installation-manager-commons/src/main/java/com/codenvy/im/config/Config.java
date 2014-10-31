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
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public abstract class Config {
    private Map<String, String> properties = new HashMap<>();
    
    public void load(InputStream in) throws ConfigException {
        this.properties = ConfigUtils.readProperties(in);
    }

    protected final String getProperty(ConfigProperty property) throws ConfigException {
        if (! properties.containsKey(property.toString())) {
            throw new ConfigException(format("Property '%s' hasn't been found.", property));
        }

        return properties.get(property.toString());
    }

    protected final void setProperty(ConfigProperty property, String value) {
        properties.put(property.toString(), value);
    }


}
