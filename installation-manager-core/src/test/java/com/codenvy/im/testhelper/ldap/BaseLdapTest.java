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
package com.codenvy.im.testhelper.ldap;

import com.codenvy.im.BaseTest;
import com.codenvy.im.managers.Config;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 */
public class BaseLdapTest extends BaseTest {
    public static final String TEST_LDAP_ADMIN           = "ldap_admin";
    public static final String TEST_SYSTEM_LDAP_PASSWORD = "system_ldap_password";

    public EmbeddedADS ads;

    @BeforeClass
    public void initializationDirectoryService() throws Exception {
        File workDir = new File("target/server-work");
        workDir.mkdirs();

        // Create the server
        ads = new EmbeddedADS(workDir);

        // optionally we can start a server too
        ads.startServer();
    }

    @Test
    public void checkTestData() throws Exception {
        // Read an entry
        Entry result = ads.getService().getAdminSession().lookup(new Dn("dc=codenvy-enterprise,dc=com"));

        // Check test data
        assertEquals(result.toString(), "Entry\n" +
                             "    dn[n]: dc=apache,dc=org\n" +
                             "    objectClass: top\n" +
                             "    objectClass: domain\n" +
                             "    objectClass: extensibleObject\n" +
                             "    dc: Apache\n");
    }

    protected Map<String, String> getTestSingleNodeProperties() {
        Map<String, String> properties = new HashMap<>(super.getTestSingleNodeProperties());
        properties.putAll(getLdapSpecificProperties());

        properties.put("Config.LDAP_PROTOCOL", "ldap");
        properties.put("Config.LDAP_HOST", "localhost");
        properties.put("Config.LDAP_PORT", "10389");
        
        properties.put("Config.JAVA_NAMING_SECURITY_AUTHENTICATION", "simple");
        properties.put("Config.JAVA_NAMING_SECURITY_PRINCIPAL", "cn=Admin,$user_ldap_dn");

        properties.put("Config.ADMIN_LDAP_USER_NAME", "admin");
        properties.put("Config.ADMIN_LDAP_PASSWORD", "password");
        
        properties.put("Config.USER_LDAP_USER_CONTAINER_DN", "ou=People,$user_ldap_dn");
        properties.put("Config.USER_LDAP_OBJECT_CLASSES", "inetOrgPerson");
        
        properties.put("Config.SYSTEM_LDAP_USER_BASE", "ou=users,$admin_ldap_dn");

        return properties;
    }

    protected Map<String, String> getTestMultiNodeProperties() {
        Map<String, String> properties = new HashMap<>(super.getTestMultiNodeProperties());
        properties.putAll(getLdapSpecificProperties());

        return properties;
    }

    private Map<String, String> getLdapSpecificProperties() {
        return new HashMap<String, String>() {{

            put("api_host_name", "api.example.com");
            put("data_host_name", "data.example.com");
            put("analytics_host_name", "analytics.example.com");
            put("host_url", "hostname");
            put(Config.ADMIN_LDAP_USER_NAME, TEST_LDAP_ADMIN);
            put(Config.ADMIN_LDAP_PASSWORD, TEST_SYSTEM_LDAP_PASSWORD);
        }};
    }
}
