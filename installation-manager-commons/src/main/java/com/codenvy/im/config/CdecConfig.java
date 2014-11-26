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
import java.util.HashMap;
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
        DNS_NAME,

        MONGO_ADMIN_PASSWORD,
        MONGO_USER_PASSWORD,
        MONGO_ORGSERVICE_USER_PASSWORD,

        ADMIN_LDAP_USER_NAME,
        ADMIN_LDAP_PASSWORD,

        MYSQL_ROOT_USER_PASSWORD,

        ZABBIX_DATABASE_PASSWORD,
        ZABBIX_ADMIN_EMAIL,
        ZABBIX_ADMIN_PASSWORD,

        PUPPET_VERSION("puppet-3.4.3-1.el6.noarch"),
        PUPPET_RESOURCE_URL("http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm");

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

    public String getDnsName() throws ConfigException {
        return getValue(Property.DNS_NAME);
    }

    public String getMongoAdminPassword() throws ConfigException {
        return getValue(Property.MONGO_ADMIN_PASSWORD);
    }

    public String getMongoUserPassword() throws ConfigException {
        return getValue(Property.MONGO_USER_PASSWORD);
    }

    public String getMongoOrgserviceUserPassword() throws ConfigException {
        return getValue(Property.MONGO_ORGSERVICE_USER_PASSWORD);
    }

    public String getAdminLdapUserName() throws ConfigException {
        return getValue(Property.ADMIN_LDAP_USER_NAME);
    }

    public String getAdminLdapPassword() throws ConfigException {
        return getValue(Property.ADMIN_LDAP_PASSWORD);
    }

    public String getMysqlRootUserPassword() throws ConfigException {
        return getValue(Property.MYSQL_ROOT_USER_PASSWORD);
    }

    public String getZabbixDatabasePassword() throws ConfigException {
        return getValue(Property.ZABBIX_DATABASE_PASSWORD);
    }

    public String getZabbixAdminEmail() throws ConfigException {
        return getValue(Property.ZABBIX_ADMIN_EMAIL);
    }

    public String getZabbixAdminPassword() throws ConfigException {
        return getValue(Property.ZABBIX_ADMIN_PASSWORD);
    }

    public String getPuppetVersion() throws ConfigException {
        return getValue(Property.PUPPET_VERSION);
    }

    public String getPuppetResourceUrl() throws ConfigException {
        return getValue(Property.PUPPET_RESOURCE_URL);
    }

    public Map<String, String> getProperties() {
        Map<String, String> m = new HashMap<>(Property.values().length);
        for (ConfigProperty property : Property.values()) {
            m.put(property.toString().toLowerCase(), getValue(property));
        }
        return Collections.unmodifiableMap(m);
    }

    /** {@inheritDoc} */
    @Override
    public void validate() throws IllegalStateException {
        for (ConfigProperty property : Property.values()) {
            if (getValue(property) == null) {
                throw new IllegalStateException(String.format("Property '%s' is missed in the configuration", property.toString().toLowerCase()));
            }
        }
    }
}
