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
package com.codenvy.im.command;

import com.codenvy.im.BaseTest;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.nio.file.Files;

import static com.codenvy.im.command.ReadMasterHostNameCommand.fetchMasterHostName;
import static org.testng.Assert.assertEquals;

public class TestReadMasterHostNameCommand {

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldReturnNullIfFileAbsent() throws Exception {
        Files.delete(BaseTest.PUPPET_CONF_FILE);
        fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldReturnNullIfRowAbsent() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]");

        fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldReturnNullIfStringIsEmpty() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "   certname = ");

        fetchMasterHostName();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldReturnNullIfWrongFormat() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "    certname  bla.bla.com\n");

        fetchMasterHostName();
    }

    @Test
    public void shouldReturnHostName() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "    certname = master.dev.com\n" +
                                                   "    hostprivkey= $privatekeydir/$certname.pem { mode = 640 }\n");

        assertEquals(fetchMasterHostName(), "master.dev.com");
    }

    @Test
    public void shouldReturnHostNameNoWhiteSpacesInLine() throws Exception {
        FileUtils.write(BaseTest.PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "certname=master.dev.com\n" +
                                                   "    hostprivkey= $privatekeydir/$certname.pem { mode = 640 }\n" +
                                                   "[agent]\n" +
                                                   "certname=la-la.com");

        assertEquals(fetchMasterHostName(), "master.dev.com");
    }
}