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


import com.codenvy.im.agent.LocalAgent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.File;

import static java.lang.String.format;

/**
 * Reads puppet master host name from the puppet configuration file /etc/puppet/puppet.conf.
 * File structure:
 *
 * [main]
 *   certname = PUPPET_MASTER_HOST_NAME
 *   ...
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class ReadMasterHostNameCommand extends SimpleCommand {
    public static final String CONF_FILE = "puppet.conf";

    @Inject
    private ReadMasterHostNameCommand(@Named("puppet.base_dir") String puppetDir) {
        super(format("if [ ! -f %1$s ]; then" +
                     "     exit 1;" +
                     " else" +
                     "     cat %1$s | grep 'certname\\s*=' | sed 's/\\s*certname\\s*=\\(.*\\)/\\1/';" +
                     " fi",
                     puppetDir + File.separator + CONF_FILE),
              new LocalAgent(),
              "Reads puppet master host name");
    }

    /**
     * @see com.codenvy.im.command.SimpleCommand#fetchValue
     */
    @Nullable
    public static String fetchMasterHostName() {
        return fetchValue(ReadMasterHostNameCommand.class);
    }
}
