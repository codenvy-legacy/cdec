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
import com.codenvy.im.install.InstallType;
import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.ini4j.IniFile;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;

import static java.nio.file.Files.exists;

/**
 * Detects which Codenvy installation type we use by analyzing puppet.conf.
 *
 * SINGLE type configuration sample:
 * [master]
 *      certname = host_name
 *      ...
 * [main]
 *      ...
 * [agent]
 *      certname = host_name
 *
 * MULTI type configuration sample:
 * [master]
 *      ...
 * [main]
 *      certname = some_host_name
 *      ...
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class DetectInstallationTypeCommand implements Command {
    public final Path puppetConfFile;

    @Inject
    private DetectInstallationTypeCommand(@Named("puppet.base_dir") String puppetDir) {
        this.puppetConfFile = Paths.get(puppetDir, Config.PUPPET_CONF_FILE_NAME).toAbsolutePath();
    }

    /** Utility method. */
    public static InstallType detectInstallationType() throws IOException {
        DetectInstallationTypeCommand command = InjectorBootstrap.INJECTOR.getInstance(DetectInstallationTypeCommand.class);
        return InstallType.valueOf(command.execute());
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        if (!exists(puppetConfFile)) {
            return InstallType.UNKNOWN.name();
        }

        try {
            IniFile iniFile = new IniFile(puppetConfFile.toFile());
            return isSingleTypeConfig(iniFile) ? InstallType.CODENVY_SINGLE_SERVER.name()
                                               : InstallType.CODENVY_MULTI_SERVER.name();
        } catch (BackingStoreException e) {
            throw new CommandException(e.getMessage(), e);
        }
    }

    private boolean isSingleTypeConfig(IniFile iniFile) throws BackingStoreException {
        return iniFile.nodeExists("agent")
               && !iniFile.node("agent").get("certname", "").isEmpty()
               && iniFile.nodeExists("master")
               && !iniFile.node("master").get("certname", "").isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Detect which Codenvy type is installed";
    }
}
