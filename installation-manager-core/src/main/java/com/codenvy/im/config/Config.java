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

import static java.util.Collections.unmodifiableMap;

/** @author Dmytro Nochevnov */
public class Config {
    public static final String SINGLE_SERVER_PROPERTIES      = "manifests/nodes/single_server/single_server.pp";
    public static final String SINGLE_SERVER_BASE_PROPERTIES = "manifests/nodes/single_server/base_config.pp";

    public static final String VERSION               = "version";
    public static final String CODENVY_USER_NAME     = "codenvy_user_name";
    public static final String CODENVY_PASSWORD      = "codenvy_password";
    public static final String PUPPET_AGENT_VERSION  = "puppet_agent_version";
    public static final String PUPPET_SERVER_VERSION = "puppet_server_version";
    public static final String PUPPET_RESOURCE_URL   = "puppet_resource_url";
    public static final String AIO_HOST_URL          = "aio_host_url"; // 3.1.0
    public static final String HOST_URL              = "host_url";

    private Map<String, String> properties;

    public Config(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    /** Indicates if property is set or isn't. */
    public static boolean isEmpty(String value) {
        return value == null || value.equalsIgnoreCase("MANDATORY");
    }

    /** @return the property value */
    public final String getValue(String property) {
        return properties.get(property.toLowerCase());
    }

    /** @return the either #HOST_URL or #AIO_HOST_URL property value */
    public final String getHostUrl() {
        return properties.containsKey(HOST_URL) ? properties.get(HOST_URL) : properties.get(AIO_HOST_URL);
    }

    /** Getter for #properties. Unmodifiable map will be returned */
    public Map<String, String> getProperties() {
        return unmodifiableMap(properties);
    }

    /** Checks if all properties are set and have correct values. */
    public boolean isValid() {
        for (String v : properties.values()) {
            if (isEmpty(v)) {
                return false;
            }
        }

        return true;
    }
}
