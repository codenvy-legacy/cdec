/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.managers;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.codenvy.im.utils.OSUtils.getVersion;
import static java.util.Collections.unmodifiableMap;

/** @author Dmytro Nochevnov */
public class Config {
    public static final String PUPPET_CONF_FILE_NAME = "puppet.conf";

    public static final String SINGLE_SERVER_PROPERTIES      = "manifests/nodes/single_server/single_server.pp";
    public static final String SINGLE_SERVER_BASE_PROPERTIES = "manifests/nodes/single_server/base_config.pp";

    public static final String MULTI_SERVER_PROPERTIES       = "manifests/nodes/multi_server/custom_configurations.pp";
    public static final String MULTI_SERVER_BASE_PROPERTIES  = "manifests/nodes/multi_server/base_configurations.pp";
    public static final String MULTI_SERVER_NODES_PROPERTIES = "manifests/nodes/multi_server/nodes.pp";

    public static final String VERSION = "version";

    public static final String PUPPET_AGENT_VERSION  = "puppet_agent_version";
    public static final String PUPPET_SERVER_VERSION = "puppet_server_version";
    public static final String PUPPET_RESOURCE_URL   = "puppet_resource_url";

    public static final String AIO_HOST_URL                       = "aio_host_url"; // 3.1.0
    public static final String HOST_URL                           = "host_url";
    public static final String NODE_HOST_PROPERTY_SUFFIX          = "_host_name";  // suffix of property like "builder_host_name"
    public static final String PUPPET_MASTER_HOST_NAME_PROPERTY   = "puppet_master_host_name";
    public static final String MONGO_ADMIN_PASSWORD_PROPERTY      = "mongo_admin_pass";
    public static final String NODE_SSH_USER_NAME_PROPERTY        = "node_ssh_user_name";
    public static final String NODE_SSH_USER_PRIVATE_KEY_PROPERTY = "node_ssh_user_private_key";

    public static final Map<String, Map<String, String>> PROPERTIES_BY_VERSION = new HashMap<String, Map<String, String>>() {{
        put(PUPPET_AGENT_VERSION, new HashMap<String, String>() {{
            put("6", "puppet-3.4.3-1.el6.noarch");
            put("7", "puppet-3.5.1-1.el7.noarch");
        }});
        put(PUPPET_SERVER_VERSION, new HashMap<String, String>() {{
            put("6", "puppet-server-3.4.3-1.el6.noarch");
            put("7", "puppet-server-3.5.1-1.el7.noarch");
        }});
        put(PUPPET_RESOURCE_URL, new HashMap<String, String>() {{
            put("6", "http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");
            put("7", "https://yum.puppetlabs.com/el/7/products/x86_64/puppetlabs-release-7-11.noarch.rpm");
        }});
    }};

    public static final Set<String> PROPERTIES_DEPEND_ON_VERSION = PROPERTIES_BY_VERSION.keySet();

    public static final String MANDATORY = "MANDATORY";

    private Map<String, String> properties;

    public Config(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    /** Indicates is value valid for configuration. */
    public static boolean isValid(String value) {
        return value != null && !isMandatory(value);
    }

    /** Indicates is value empty. */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /** Indicates does value indicate mandatory config property. */
    public static boolean isMandatory(String value) {
        return value != null && value.equalsIgnoreCase(MANDATORY);
    }

    /** Indicates is value valid for mandatory config property. */
    public static boolean isValidForMandatoryProperty(String value) {
        return isValid(value) && !isEmpty(value);
    }

    /** @return the property value */
    @Nullable
    public String getValue(String property) {
        property = property.toLowerCase();
        if (PROPERTIES_DEPEND_ON_VERSION.contains(property)) {
            return PROPERTIES_BY_VERSION.get(property).get(getVersion());
        }
        return properties.get(property);
    }

    /** @return list of values separated by comma */
    @Nullable
    public List<String> getAllValues(String property) {
        property = property.toLowerCase();
        String value;
        if (PROPERTIES_DEPEND_ON_VERSION.contains(property)) {
            value = PROPERTIES_BY_VERSION.get(property).get(getVersion());
        } else {
            value = getValue(property);
        }

        List<String> result = new LinkedList<>();
        if (value == null) {
            return null;
        }

        for (String item : value.split(",")) {
            item = item.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }

        return result;
    }


    /** Getter for #properties. Unmodifiable map will be returned */
    public Map<String, String> getProperties() {
        return unmodifiableMap(properties);
    }

    /** Checks if all properties are set and have correct values. */
    public boolean isValid() {
        for (String v : properties.values()) {
            if (!isValid(v)) {
                return false;
            }
        }

        return true;
    }

    public Object getMongoAdminPassword() {
        return getValue(MONGO_ADMIN_PASSWORD_PROPERTY);
    }

    /** @return the either #HOST_URL or #AIO_HOST_URL property value */
    public String getHostUrl() {
        return properties.containsKey(HOST_URL) ? properties.get(HOST_URL) : properties.get(AIO_HOST_URL);
    }
}
