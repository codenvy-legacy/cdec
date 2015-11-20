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

import org.eclipse.che.commons.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String  ENCLOSED_PROPERTY_NAME         = "\\$\\w+";
    public static final Pattern ENCLOSED_PROPERTY_NAME_PATTERN =  Pattern.compile(ENCLOSED_PROPERTY_NAME);

    public static final String VERSION = "version";

    public static final String PUPPET_AGENT_VERSION  = "puppet_agent_version";
    public static final String PUPPET_SERVER_VERSION = "puppet_server_version";
    public static final String PUPPET_RESOURCE_URL   = "puppet_resource_url";

    public static final String AIO_HOST_URL              = "aio_host_url"; // 3.1.0
    public static final String HOST_URL                  = "host_url";
    public static final String NODE_HOST_PROPERTY_SUFFIX = "_host_name";  // suffix of property like "builder_host_name"

    public static final String PUPPET_MASTER_HOST_NAME = "puppet_master_host_name";

    public static final String MONGO_ADMIN_USERNAME      = "mongo_admin_user_name";
    public static final String MONGO_ADMIN_PASSWORD      = "mongo_admin_pass";
    public static final String NODE_SSH_USER_NAME        = "node_ssh_user_name";
    public static final String NODE_SSH_USER_PRIVATE_KEY = "node_ssh_user_private_key";

    /* ldap properties */
    public static final String ADMIN_LDAP_PASSWORD   = "admin_ldap_password";
    public static final String SYSTEM_LDAP_USER_BASE = "system_ldap_user_base";

    public static final String ADMIN_LDAP_DN        = "admin_ldap_dn";
    public static final String ADMIN_LDAP_USER_NAME = "admin_ldap_user_name";

    public static final String USER_LDAP_USER_CONTAINER_DN = "user_ldap_user_container_dn";
    public static final String USER_LDAP_OBJECT_CLASSES    = "user_ldap_object_classes";
    public static final String USER_LDAP_PASSWORD          = "user_ldap_password";

    public static final String LDAP_HOST     = "ldap_host";
    public static final String LDAP_PORT     = "ldap_port";
    public static final String LDAP_PROTOCOL = "ldap_protocol";

    public static final String JAVA_NAMING_SECURITY_AUTHENTICATION = "java_naming_security_authentication";
    public static final String JAVA_NAMING_SECURITY_PRINCIPAL      = "java_naming_security_principal";





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

    private Map<String, String> properties;

    public Config(Map<String, String> properties) {
        this.properties = Collections.unmodifiableMap(properties);
    }

    /**
     * @return the property value; substitute enclosed properties like user_ldap_dn in the value "ou=People,$user_ldap_dn"
     * It doesn't take into account:
     * - qualified variable names,
     * - names like ${foo},
     * - second, ... enclosed variables in the same value
     *
     */
    @Nullable
    public String getValue(String property) {
        String value;
        if (PROPERTIES_DEPEND_ON_VERSION.contains(property)) {
            value = PROPERTIES_BY_VERSION.get(property).get(getVersion());
        } else {
            value = properties.get(property);
        }

        if (value != null && value.contains("$")) {
            Matcher m = ENCLOSED_PROPERTY_NAME_PATTERN.matcher(value);
            if (m.find()) {
                String enclosedPropertyName = m.group();
                String enclosedPropertyValue = getValue(enclosedPropertyName.replace("$", ""));

                if (enclosedPropertyValue != null) {
                    value = value.replace(enclosedPropertyName, enclosedPropertyValue);
                }
            }
        }

        return value;
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

    /** @return the either #HOST_URL or #AIO_HOST_URL property value */
    public String getHostUrl() {
        return properties.containsKey(HOST_URL) ? properties.get(HOST_URL) : properties.get(AIO_HOST_URL);
    }
}
