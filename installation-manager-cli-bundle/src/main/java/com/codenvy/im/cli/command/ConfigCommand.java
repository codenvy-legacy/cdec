/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.im.cli.command;


import com.codenvy.im.service.InstallationManagerConfig;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/** @author Anatoliy Bazko */
@Command(scope = "codenvy", name = "im-config", description = "Config installation manager")
public class ConfigCommand extends AbstractIMCommand {

    @Option(name = "--download-dir", description = "To set the download directory", required = false)
    private String downloadDir;

    @Option(name = "--proxy-url", description = "To set the proxy url", required = false)
    private String proxyUrl;

    @Option(name = "--proxy-port", description = "To set the proxy port", required = false)
    private String proxyPort;

    @Override
    protected void doExecuteCommand() throws Exception {
        InstallationManagerConfig config = new InstallationManagerConfig();
        if (downloadDir != null) {
            config.setDownloadDir(downloadDir);
        }
        if (proxyUrl != null) {
            config.setProxyUrl(proxyUrl);
        }
        if (proxyPort != null) {
            config.setProxyPort(proxyPort);
        }

        if (!config.checkEmptyConfig()) {
            console.printResponse(service.setConfig(config));
        } else {
            console.printResponse(service.getConfig());
        }
    }
}
