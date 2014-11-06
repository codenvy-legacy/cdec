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

import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.response.Status;

import com.codenvy.im.restlet.InstallationManager;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-install", description = "Install updates")
public class InstallCommand extends AbstractIMCommand {


    @Argument(index = 0, name = "artifact",
              description = "The name of the specific artifact to install. Installation manager will be updated by default.",
              required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    private static final String DEFAULT_ARTIFACT_NAME = InstallManagerArtifact.NAME;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (list) {
                return doExecuteListOption();
            } else {
                return doExecuteInstall();
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }

    private Void doExecuteInstall() throws JSONException, IOException {
        String response;
        if (artifactName != null && version != null) {
            response = installationManagerProxy.install(artifactName, version, getCredentialsRep());
        } else if (artifactName != null) {
            response = installationManagerProxy.install(artifactName, getCredentialsRep());
        } else {
            response = installationManagerProxy.install(DEFAULT_ARTIFACT_NAME, getCredentialsRep());
        }

        printResponse(response);

        if (isIMSuccessfullyUpdated(response)) {
            printInfo("'Installation Manager CLI' is being updated! Press any key to exit...");

            session.getKeyboard().read();
            System.exit(0);
        }

        return null;
    }

    private Void doExecuteListOption() throws IOException, JSONException {
        String response = installationManagerProxy.getVersions(getCredentialsRep());
        JSONObject jsonResponse = new JSONObject(response);
        jsonResponse.put("CLI client version", getClientBuildVersion());

        printResponse(jsonResponse.toString());

        return null;
    }

    private boolean isIMSuccessfullyUpdated(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        if (response.contains("artifacts")) {
            JSONArray l = jsonResponse.getJSONArray("artifacts");
            for (int i = 0; i < l.length(); i++) {
                String artifact = ((JSONObject)l.get(i)).getString("artifact");
                String status = ((JSONObject)l.get(i)).getString("status");

                if (artifact != null && artifact.equalsIgnoreCase(InstallManagerArtifact.NAME)) {
                    return status != null && status.equals(Status.SUCCESS.name());
                }
            }
        }

        return false;
    }

    private String getClientBuildVersion() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("codenvy/ClientBuildInfo.properties")) {
            Properties props = new Properties();
            props.load(in);

            if (props.containsKey("version")) {
                return (String)props.get("version");
            } else {
                throw new IOException(this.getClass().getSimpleName());
            }
        }
    }
}
