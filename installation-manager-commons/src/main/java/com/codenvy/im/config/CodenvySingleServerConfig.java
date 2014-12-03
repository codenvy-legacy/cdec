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

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class CodenvySingleServerConfig extends Config {
    public CodenvySingleServerConfig(Map<String, String> properties) {
        super(properties);
    }

    public enum Property implements ConfigProperty {
        HOST_URL,
        MONGO_ADMIN_PASS,
        MONGO_USER_PASS,
        MONGO_ORGSERVICE_USER_PWD,
        USER_LDAP_PASSWORD,
        ADMIN_LDAP_USER_NAME,
        ADMIN_LDAP_PASSWORD,
        MYSQL_ROOT_PASSWORD,
        ZABBIX_DB_PASS,
        ZABBIX_TIME_ZONE,
        ZABBIX_ADMIN_EMAIL,
        ZABBIX_ADMIN_PASSWORD,
        HAPROXY_STATISTIC_PASS,
        JMX_USERNAME,
        JMX_PASSWORD,
        BUILDER_MAX_EXECUTION_TIME,
        BUILDER_WAITING_TIME,
        BUILDER_KEEP_RESULT_TIME,
        BUILDER_QUEUE_SIZE,
        RUNNER_DEFAULT_APP_MEM_SIZE,
        RUNNER_WORKSPACE_MAX_MEMSIZE,
        RUNNER_APP_LIFETIME,
        RUNNER_WAITING_TIME,
        WORKSPACE_INACTIVE_TEMPORARY_STOP_TIME,
        WORKSPACE_INACTIVE_PERSISTENT_STOP_TIME,
        CODENVY_SERVER_XMX,
        GOOGLE_CLIENT_ID,
        GOOGLE_SECRET,
        GITHUB_CLIENT_ID,
        GITHUB_SECRET,
        BITBUCKET_CLIENT_ID,
        BITBUCKET_SECRET,
        WSO2_CLIENT_ID,
        WSO2_SECRET,
        PROJECTLOCKER_CLIENT_ID,
        PROJECTLOCKER_SECRET,
        PUPPET_AGENT_VERSION,
        PUPPET_SERVER_VERSION,
        PUPPET_RESOURCE_URL
    }

    public Map<String, String> getProperties() {
        Map<String, String> m = new HashMap<>(Property.values().length);
        for (ConfigProperty property : Property.values()) {
            m.put(property.toString().toLowerCase(), getValue(property));
        }
        return unmodifiableMap(m);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        return super.isValid(Property.values());
    }
}
