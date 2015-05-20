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
package com.codenvy.im.cli.command;


import com.codenvy.im.managers.Config;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

/** @author Anatoliy Bazko */
@Command(scope = "codenvy", name = "im-config", description = "Config installation manager and Codenvy on-prem")
public class ConfigCommand extends AbstractIMCommand {


    @Option(name = "--codenvy_host_url", description = "DNS name of codenvy host to change", required = false)
    private String codenvyHostUrl;

    @Override

    protected void doExecuteCommand() throws Exception {
        if (codenvyHostUrl != null && !codenvyHostUrl.isEmpty()) {
            changeCodenvyHostUrl();
            return;
        }

        getImConfig();
    }

    private void getImConfig() throws JsonParseException {
        console.printResponse(facade.getConfig());
    }

    private void changeCodenvyHostUrl() throws JsonParseException {
        console.printResponse(facade.changeCodenvyConfig(Config.HOST_URL, codenvyHostUrl));
    }
}
