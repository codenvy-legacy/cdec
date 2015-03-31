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
package com.codenvy.im;

import com.codenvy.im.command.ReadMasterHostNameCommand;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;

/**
 * @author Anatoliy Bazko
 */
public class BaseTest {
    private static final Path PUPPET_CONF_FILE = Paths.get("target", "puppet", ReadMasterHostNameCommand.CONF_FILE);

    @BeforeMethod
    public void clear() throws Exception {
        if (exists(PUPPET_CONF_FILE)) {
            delete(PUPPET_CONF_FILE);
        }
    }

    protected void prepareConfForSingleNodeInstallation() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                   "certname = hostname\n" +
                                                   "[agent]\n" +
                                                   "certname = hostname\n");
    }

    protected void prepareConfForMultiNodeInstallation() throws Exception {
        FileUtils.write(PUPPET_CONF_FILE.toFile(), "[master]\n" +
                                                   "[main]\n" +
                                                   "certname = hostname\n");
    }
}
