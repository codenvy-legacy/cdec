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

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.codenvy.im.command.ReadMasterHostNameCommand.fetchMasterHostName;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class TestReadMasterHostNameCommand {
    private static final Path PUPPET_CONF_FILE = Paths.get("target", "puppet", ReadMasterHostNameCommand.CONF_FILE);

    @Test
    public void shouldReturnNullIfFileAbsent() throws Exception {
        Files.delete(PUPPET_CONF_FILE);
        assertNull(fetchMasterHostName());
    }

    @Test
    public void shouldReturnNullIfRowAbsent() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[main]");

        assertNull(fetchMasterHostName());
    }

    @Test
    public void shouldReturnNullIfStringIsEmpty() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "   server = ");

        assertNull(fetchMasterHostName());
    }

    @Test
    public void shouldReturnNullIfWrongFormat() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "   server  bla.bla.com");

        assertNull(fetchMasterHostName());
    }

    @Test
    public void shouldReturnHostName() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "   server=    bla.bla.com   ");

        assertEquals(fetchMasterHostName(), "bla.bla.com");
    }

    @Test
    public void shouldReturnHostNameNoWhiteSpacesInLine() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[main]\n" +
                                                   "server=bla.bla.com");

        assertEquals(fetchMasterHostName(), "bla.bla.com");
    }
}