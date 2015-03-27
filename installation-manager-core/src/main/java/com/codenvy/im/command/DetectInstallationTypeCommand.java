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
import com.codenvy.im.config.Config;
import com.codenvy.im.install.InstallType;
import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.nio.file.Files.exists;

/**
 * Detects which Codenvy installation type we have deal with.
 * The main idea is that we check if puppet configuration file contains [agent] section.
 * Commands relies on {@link com.codenvy.im.artifacts.helper.CDECMultiServerHelper}
 * where [agent] section is removed from config file in case of multi-node installation type.
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class DetectInstallationTypeCommand extends SimpleCommand {
    public final Path puppetConfFile;

    @Inject
    private DetectInstallationTypeCommand(@Named("puppet.base_dir") String puppetDir) {
        super(format("if [ ! -f %1$s ]; then" +
                     "     exit 1;" +
                     " else" +
                     "     cat %1$s | grep -F [agent];" +
                     " fi",
                     puppetDir + "/" + Config.PUPPET_CONF_FILE_NAME),
              new LocalAgent(),
              "Detect Codenvy installation type");

        this.puppetConfFile = Paths.get(puppetDir, Config.PUPPET_CONF_FILE_NAME);
    }

    /**
     * Utility method.
     */
    public static InstallType detectInstallationType() {
        DetectInstallationTypeCommand command = InjectorBootstrap.INJECTOR.getInstance(DetectInstallationTypeCommand.class);

        if (!exists(command.puppetConfFile)) {
            return InstallType.UNKNOWN;
        }

        try {
            String value = command.execute().trim();
            return value.isEmpty() ? InstallType.CODENVY_MULTI_SERVER : InstallType.CODENVY_SINGLE_SERVER;
        } catch (IOException e) {
            return InstallType.CODENVY_MULTI_SERVER;
        }
    }
}
