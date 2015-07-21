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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestPuppetError {
    PuppetErrorInterrupter testInterrupter;

    @BeforeMethod
    public void setup() {
        testInterrupter = spy(new PuppetErrorInterrupter(null, null));
    }

    @Test(dataProvider = "TestCheckPuppetErrorData")
    public void testCheckPuppetError(String line, PuppetError expectedResult) {
        assertEquals(testInterrupter.checkPuppetError(line), expectedResult);
    }

    @DataProvider(name = "TestCheckPuppetErrorData")
    public Object[][] getTestCheckPuppetErrorData() {
        return new Object[][] {
            {"", null},
            {"any message", null},
            {"Apr 26 03:46:41 WR7N1 puppet-agent[10240]: Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy\r", PuppetError.COULD_NOT_RETRIEVE_CATALOG},
            {"Jun 17 10:03:40 ns2 puppet-agent[23932]: (/Stage[main]/Third_party::Zabbix::Server_config/Exec[init_zabbix_db]) Dependency Exec[set-mysql-password] has failures: true\r", PuppetError.DEPENDENCY_HAS_FAILURES},
            {"Jun 22 18:26:22 api puppet-agent[5867]: (/Stage[main]/Multi_server::Api_instance::Service_codeassistant/Service[codenvy-codeassistant]) Dependency Service[codenvy] has failures: true\r", null}
        };
    }

    @Test(dataProvider = "TestGetLineToDisplay")
    public void testGetLineToDisplay(PuppetError puppetError, String logLine, String lineToDisplay) {
        assertEquals(puppetError.getLineToDisplay(logLine), lineToDisplay);

        assertEquals(PuppetError.COULD_NOT_RETRIEVE_CATALOG.getLineToDisplay("Apr 26 03:46:41 WR7N1 puppet-agent[10240]: Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy\r"),
                     "Apr 26 03:46:41 WR7N1 puppet-agent[10240]: Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy");

    }

    @DataProvider(name = "TestGetLineToDisplay")
    public Object[][] getTestGetLineToDisplay() {
        return new Object[][] {
            {PuppetError.COULD_NOT_RETRIEVE_CATALOG, "test", "test"},
            {PuppetError.COULD_NOT_RETRIEVE_CATALOG,
             "Apr 26 03:46:41 WR7N1 puppet-agent[10240]: Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy\r",
             "Apr 26 03:46:41 WR7N1 puppet-agent[10240]: Could not retrieve catalog from remote server: Error 400 on SERVER: Unrecognized operating system at /etc/puppet/modules/third_party/manifests/puppet/service.pp:5 on node hwcodenvy"},

            {PuppetError.DEPENDENCY_HAS_FAILURES, "test", "test"},
            {PuppetError.DEPENDENCY_HAS_FAILURES,
             "Jun 17 10:03:40 ns2 puppet-agent[23932]: (/Stage[main]/Third_party::Zabbix::Server_config/Exec[init_zabbix_db]) Dependency Exec[set-mysql-password] has failures: true\r",
             "Dependency Exec[set-mysql-password] has failures: true"}
        };
    }
}
