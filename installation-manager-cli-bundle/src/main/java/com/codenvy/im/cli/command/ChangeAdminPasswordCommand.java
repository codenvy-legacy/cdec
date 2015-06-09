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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.response.BasicResponse;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.HashMap;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/**
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-password", description = "Change Codenvy admin password")
public class ChangeAdminPasswordCommand extends AbstractIMCommand {

    @Argument(name = "currentPassword", description = "current admin password", required = true, multiValued = false, index = 0)
    private String currentPassword;

    @Argument(name = "newPassword", description = "new admin password", required = true, multiValued = false, index = 1)
    private String newPassword;

    @Override
    protected void doExecuteCommand() throws Exception {
        facade.changeAdminPassword(currentPassword.getBytes("UTF-8"), newPassword.getBytes("UTF-8"));

        Artifact artifact = createArtifact(CDECArtifact.NAME);
        Map<String, String> properties = new HashMap<>();
        properties.put("user_ldap_password", newPassword);
        properties.put("system_ldap_password", newPassword);

        facade.updateArtifactConfig(artifact, properties);

        console.printResponse(BasicResponse.ok());
    }
}
