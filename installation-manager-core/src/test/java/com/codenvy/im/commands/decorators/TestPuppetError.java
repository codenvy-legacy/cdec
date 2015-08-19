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
package com.codenvy.im.commands.decorators;

import com.codenvy.im.managers.NodeConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestPuppetError {
    @Mock
    NodeConfig mockNode;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(dataProvider = "getTestGetLineToDisplay")
    public void testMatchPuppetError(String logLine, NodeConfig node, PuppetError expectedPuppetError) {
        PuppetError testError = PuppetError.match(logLine, node);
        assertEquals(testError, expectedPuppetError);
    }

    @DataProvider
    public Object[][] getTestGetLineToDisplay() {
        return new Object[][]{
            {null, null, null},

            {"test", null, null},

            {"2015-07-29 14:02:29 +0100 /Stage[main]/Multi_server::Api_instance::Service_codeassistant/Service[codenvy-codeassistant] (notice): Dependency Service[codenvy] has failures: true\r",
             null, null},   // issue CDEC-264

            {"2015-07-29 16:12:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\r",
             null,
             new PuppetError(null, "Dependency Package[openldap] has failures: true")},

            {"2015-07-29 16:12:52 +0100 /Stage[main]/Third_party::Openldap_servers::Package/Package[openldap-servers] (notice): Dependency Package[openldap] has failures: true\n",
             mockNode,
             new PuppetError(mockNode, "Dependency Package[openldap] has failures: true")},

            {"2015-07-29 16:24:58 +0100 Puppet (err): Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy\n",
             mockNode,
             new PuppetError(mockNode, "Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy")},
        };
    }
}
