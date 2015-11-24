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
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 */
public class BaseLdapTest extends BaseTest {

    public EmbeddedADS ads;

    @BeforeClass
    public void initializationDirectoryService() throws Exception {
        File workDir = new File("target/server-work");
        workDir.mkdirs();

        // Create the server
        ads = new EmbeddedADS(workDir);

        // Import codenvy 3 ldap user db
        JdbmPartition codenvyUserPartition = ads.addPartition("codenvy-user", EmbeddedADS.TEST_USER_LDAP_DN);
        // use command "sudo slapcat -b 'dc=codenvy-enterprise,dc=com'" to obtain it
        ads.importEntriesFromLdif(codenvyUserPartition, Paths.get("target/test-classes/ldap/codenvy3-user-db.ldif"));

        // Import codenvy 3 ldap admin db
        // use command "sudo slapcat -b 'dc=codenvycorp,dc=com'" to obtain it
        JdbmPartition codenvyAdminPartition = ads.addPartition("codenvy-admin", EmbeddedADS.TEST_ADMIN_LDAP_DN);
        ads.importEntriesFromLdif(codenvyAdminPartition, Paths.get("target/test-classes/ldap/codenvy3-admin-db.ldif"));

        // optionally we can start a server too
        ads.startServer();
    }

    protected Map<String, String> getTestSingleNodeProperties() {
        Map<String, String> properties = new HashMap<>(super.getTestSingleNodeProperties());
        properties.putAll(getLdapSpecificProperties());
        
        return properties;
    }

    private Map<String, String> getLdapSpecificProperties() {
        return new HashMap<String, String>() {{
            put(Config.LDAP_PROTOCOL, EmbeddedADS.ADS_PROTOCOL);
            put(Config.LDAP_HOST, EmbeddedADS.ADS_HOST);
            put(Config.LDAP_PORT, EmbeddedADS.ADS_PORT);

            put(Config.JAVA_NAMING_SECURITY_AUTHENTICATION, EmbeddedADS.ADS_SECURITY_AUTHENTICATION);
            put(Config.JAVA_NAMING_SECURITY_PRINCIPAL, EmbeddedADS.ADS_SECURITY_PRINCIPAL);

            put(Config.ADMIN_LDAP_USER_NAME, "admin");
            put(Config.USER_LDAP_PASSWORD, EmbeddedADS.ADS_SECURITY_CREDENTIALS);
            put(Config.ADMIN_LDAP_PASSWORD, EmbeddedADS.ADS_SECURITY_CREDENTIALS);

            put(Config.USER_LDAP_USER_CONTAINER_DN, "ou=People,$user_ldap_dn");
            put(Config.USER_LDAP_OBJECT_CLASSES, "inetOrgPerson");

            put(Config.USER_LDAP_DN, EmbeddedADS.TEST_USER_LDAP_DN);
            put(Config.ADMIN_LDAP_DN, EmbeddedADS.TEST_ADMIN_LDAP_DN);
            put(Config.SYSTEM_LDAP_USER_BASE, "ou=users,$admin_ldap_dn");
            put(Config.SYSTEM_LDAP_JAVA_NAMING_SECURITY_PRINCIPAL, EmbeddedADS.ADS_SECURITY_PRINCIPAL);
        }};
    }
}
