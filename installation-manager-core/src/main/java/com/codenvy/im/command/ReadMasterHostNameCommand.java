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


import com.codenvy.im.config.Config;
import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.ini4j.IniFile;
import org.ini4j.InvalidIniFormatException;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;

import static java.nio.file.Files.exists;

/**
 * Reads puppet master host name from the puppet configuration file /etc/puppet/puppet.conf.
 * It is supposed that we have deal with multi-server configuration type.
 * @see com.codenvy.im.command.DetectInstallationTypeCommand
 *
 * File structure:
 *
 * [main]
 *   certname = PUPPET_MASTER_HOST_NAME
 *   ...
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class ReadMasterHostNameCommand implements Command {
    public final Path puppetConfFile;

    @Inject
    private ReadMasterHostNameCommand(@Named("puppet.base_dir") String puppetDir) {
        this.puppetConfFile = Paths.get(puppetDir, Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();
    }

    /**
     * Fetches master host name from the configuration.
     *
     * @throws java.lang.IllegalStateException
     *         if property is absent
     */
    @Nullable
    public static String fetchMasterHostName() throws CommandException, IllegalStateException {
        Command command = InjectorBootstrap.INJECTOR.getInstance(ReadMasterHostNameCommand.class);
        String hostName = command.execute();
        if (hostName == null || hostName.trim().isEmpty()) {
            throw new IllegalStateException("There is no puppet master host name in the configuration");
        }

        return hostName;
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        if (!exists(puppetConfFile)) {
            return null;
        }

        try {
            IniFile iniFile = new IniFile(puppetConfFile.toFile());
            return iniFile.node("main").get("certname", "");
        } catch (BackingStoreException e) {
            if (e.getCause() instanceof InvalidIniFormatException) {
                return null;
            }
            throw new CommandException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Reads puppet master host name";
    }
}
