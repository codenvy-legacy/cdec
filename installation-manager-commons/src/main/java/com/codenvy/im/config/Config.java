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

import java.util.Collections;
import java.util.Map;

/** @author Dmytro Nochevnov */
public abstract class Config {
    private Map<String, String> properties;

    public Config(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    /** @return the property value */
    public final String getValue(ConfigProperty property) {
        return properties.get(property.toString().toLowerCase());
    }

    /**
     * @return property name, for instance, if property name is "MONGO_ADMIN_PASSWORD" then "mongo admin password" will be returned
     */
    public static String getPropertyName(ConfigProperty property) {
        return property.toString().replace("_", " ").toLowerCase();
    }
}
