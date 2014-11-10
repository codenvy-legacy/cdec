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

import com.codenvy.commons.json.JsonParseException;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

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

    protected static final String DEFAULT_ARTIFACT_NAME = InstallManagerArtifact.NAME;

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

    private Void doExecuteInstall() throws JSONException, IOException, JsonParseException {
        String response;
        if (artifactName != null && version != null) {
            response = installationManagerProxy.install(artifactName, version, getCredentialsRep());
        } else if (artifactName != null) {
            response = installationManagerProxy.install(artifactName, getCredentialsRep());
        } else {
            response = installationManagerProxy.install(DEFAULT_ARTIFACT_NAME, getCredentialsRep());
        }

        Response responseObj = Response.fromJson(response);
        if (isStepwiseInstallStarted(responseObj)) {
            List<String> steps = extractStepInfo(responseObj);
            if (steps != null && !steps.isEmpty()) {
                return doInstallStepwisely(steps);
            }
        }

        printResponse(response);

        if (isIMSuccessfullyUpdated(responseObj)) {
            printInfo("'Installation Manager CLI' is being updated! Press any key to exit...");

            session.getKeyboard().read();
            System.exit(0);
        }

        return null;
    }

    private Void doExecuteListOption() throws IOException, JSONException {
        String response = installationManagerProxy.getVersions(getCredentialsRep());
        response = insertClientVersionInfo(response);
        printResponse(response);
        return null;
    }

    private String insertClientVersionInfo(String response) throws IOException {
        StringBuffer newResponse = new StringBuffer(response);
        String clientVersionInfo = format("  \"CLI client version\" : \"%s\",\n", getClientBuildVersion());
        newResponse.insert(2, clientVersionInfo);
        return newResponse.toString();
    }

    private boolean isIMSuccessfullyUpdated(Response response) throws JSONException {
        List<ArtifactInfo> artifacts = response.getArtifacts();
        if (artifacts == null) {
            return false;
        }

        for (ArtifactInfo artifact: artifacts) {
            if (artifact.getArtifact().equals(InstallManagerArtifact.NAME)
                && artifact.getStatus() == Status.SUCCESS) {
                return true;
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

    private Void doInstallStepwisely(List<String> steps) throws IOException, JsonParseException {
        String response = "";
        for(String step: steps) {
            printInfo(format("Executing command %s ...", step));

            if (artifactName != null && version != null) {
                response = installationManagerProxy.install(artifactName, version, getCredentialsRep());
            } else if (artifactName != null) {
                response = installationManagerProxy.install(artifactName, getCredentialsRep());
            } else {
                response = installationManagerProxy.install(DEFAULT_ARTIFACT_NAME, getCredentialsRep());
            }

            Response responseObj = Response.fromJson(response);
            if (responseObj.getStatus() == ResponseCode.ERROR) {
                printLineSeparator();
                printError(responseObj.getMessage());

                printInfo("Is it ok [y/N]: ");
                String userAnswer = null;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                    userAnswer = reader.readLine();
                    printLineSeparator();
                }

                if (userAnswer != null && userAnswer.equals("y")) {
                    continue;
                }

                // TODO clear installer commands list
                break;

            } else {
                printInfo(" Ready.");
                printLineSeparator();
            }
        }

        printInfo(response);

        return null;
    }

    private boolean isStepwiseInstallStarted(Response response) {
        List<ArtifactInfo> artifacts = response.getArtifacts();
        if (artifacts == null) {
            return false;
        }

        for (ArtifactInfo artifact: artifacts) {
            if (artifact.getArtifact().equals(artifactName)
                && artifact.getStatus() == Status.INSTALL_STARTED) {
                return true;
            }
        }

        return false;
    }

    private List<String> extractStepInfo(Response response) throws JSONException, JsonParseException {
        List<ArtifactInfo> artifacts = response.getArtifacts();
        if (artifacts == null) {
            return null;
        }

        for (ArtifactInfo artifact: artifacts) {
            if (artifact.getArtifact().equals(artifactName)
                && artifact.getStatus() == Status.INSTALL_STARTED) {
                return artifact.getInstallCommandsInfo();
            }
        }

        return null;
    }

}
