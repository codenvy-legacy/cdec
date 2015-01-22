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

import com.codenvy.im.command.Command;
import com.codenvy.im.command.DetectRedHatVersionCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableMap;

/** @author Dmytro Nochevnov */
public class Config {
    public static final String SINGLE_SERVER_PROPERTIES      = "manifests/nodes/single_server/single_server.pp";
    public static final String SINGLE_SERVER_BASE_PROPERTIES = "manifests/nodes/single_server/base_config.pp";

    public static final String VERSION           = "version";
    public static final String CODENVY_USER_NAME = "codenvy_user_name";
    public static final String CODENVY_PASSWORD  = "codenvy_password";

    public static final String PUPPET_AGENT_VERSION  = "puppet_agent_version";
    public static final String PUPPET_SERVER_VERSION = "puppet_server_version";
    public static final String PUPPET_RESOURCE_URL   = "puppet_resource_url";

    public static final String AIO_HOST_URL = "aio_host_url"; // 3.1.0
    public static final String HOST_URL     = "host_url";

    public static final Map<String, Map<String, String>> PROPERTIES_BY_VERSION = new HashMap<String, Map<String, String>>() {{
        put(PUPPET_AGENT_VERSION, new HashMap<String, String>() {{
            put("6", "puppet-3.4.3-1.el6.noarch");
            put("7", "puppet-3.5.1-1.el7.noarch");
        }});
        put(PUPPET_SERVER_VERSION, new HashMap<String, String>() {{
            put("6", "puppet-server-3.4.3-1.el6");
            put("7", "puppet-server-3.5.1-1.el7.noarch");
        }});
        put(PUPPET_RESOURCE_URL, new HashMap<String, String>() {{
            put("6", "http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");
            put("7", "https://yum.puppetlabs.com/el/7/products/x86_64/puppetlabs-release-7-11.noarch.rpm");
        }});
    }};

    public static final String      DEFAULT_OS_VERSION           = "6";
    public static final Set<String> PROPERTIES_DEPEND_ON_VERSION = PROPERTIES_BY_VERSION.keySet();

    public static final String OS_VERSION;

    static {
        OS_VERSION = detectOSVersion();
    }

    private Map<String, String> properties;

    public Config(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    /** Indicates if property is set or isn't. */
    public static boolean isEmpty(String value) {
        return value == null || value.equalsIgnoreCase("MANDATORY");
    }

    /** @return the property value */
    public final String getProperty(String property) {
        property = property.toLowerCase();
        if (PROPERTIES_DEPEND_ON_VERSION.contains(property)) {
            return PROPERTIES_BY_VERSION.get(property).get(OS_VERSION);
        }
        return properties.get(property);
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

    protected static String detectOSVersion() {
        String version;

        Command command = new DetectRedHatVersionCommand();
        try {
            version = command.execute();
        } catch (Exception e) {
            return DEFAULT_OS_VERSION;
        }

        return fetchVersion(version);
    }

    protected static String fetchVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("OS version is unknown");
        }

        String[] str = version.split(Pattern.quote("."));
        if (str.length == 0) {
            throw new IllegalArgumentException("OS version is unknown");
        }

        return str[0];
    }
}
