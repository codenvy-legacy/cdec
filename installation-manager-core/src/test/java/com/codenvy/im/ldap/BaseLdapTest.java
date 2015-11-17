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
package com.codenvy.im.ldap;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;

/**
 * @author Alexander Reshetnyak
 */
public class BaseLdapTest {
    public EmbeddedADS ads;

    @BeforeClass
    public void initializationDirectoryService() throws Exception {
        File workDir = new File("target/server-work");
        workDir.mkdirs();

        // Create the server
        ads = new EmbeddedADS(workDir);

        // optionally we can start a server too
        //ads.startServer();
    }

    @Test
    public void checkTestData() throws Exception {
        // Read an entry
        Entry result = ads.getService().getAdminSession().lookup(new Dn("dc=apache,dc=org"));

        // Check test data
        assertEquals(result.toString(), "Entry\n" +
                             "    dn[n]: dc=apache,dc=org\n" +
                             "    objectClass: top\n" +
                             "    objectClass: domain\n" +
                             "    objectClass: extensibleObject\n" +
                             "    dc: Apache\n");
    }
}
