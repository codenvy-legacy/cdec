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


import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.managers.Config;
import com.codenvy.im.response.BasicResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;

/** @author Anatoliy Bazko */
@Command(scope = "codenvy", name = "im-config", description = "Configure installation manager")
public class ConfigCommand extends AbstractIMCommand {

    @Option(name = "--hostname", description = "new Codenvy hostname", required = false)
    private String hostname;

    /** {@inheritDoc} */
    @Override
    protected void doExecuteCommand() throws Exception {
        if (!isNullOrEmpty(hostname)) {
            doUpdateCodenvyHostUrl();
            return;
        }

        doGetConfig();
    }

    private void doUpdateCodenvyHostUrl() throws IOException, JsonParseException {
        console.showProgressor();
        try {
            facade.updateArtifactConfig(createArtifact(CDECArtifact.NAME), ImmutableMap.of(Config.HOST_URL, hostname));
            console.printResponse(BasicResponse.ok());
        } finally {
            console.hideProgressor();
        }
    }

    private void doGetConfig() throws JsonParseException, JsonProcessingException {
        Map<String, String> properties = facade.getInstallationManagerProperties();
        BasicResponse response = BasicResponse.ok();
        response.setProperties(properties);
        console.printResponse(response);
    }
}
