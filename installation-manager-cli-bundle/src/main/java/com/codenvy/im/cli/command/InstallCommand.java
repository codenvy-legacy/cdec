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
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.CdecInstallOptions;
import com.codenvy.im.install.DefaultOptions;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;
import org.restlet.ext.jackson.JacksonRepresentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-install", description = "Install artifact or update it")
public class InstallCommand extends AbstractIMCommand {

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Override
    protected Void execute() {
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
        if (artifactName == null) {
            printError("Argument 'artifact' is required.");
            return null;
        }

        InstallOptions installOptions = askAdditionalInstallOptions();
        JacksonRepresentation<Request> requestRep = prepareRequest(installOptions);

        String response = installationManagerProxy.getInstallInfo(requestRep);
        Response responseObj = Response.fromJson(response);

        List<String> infos = responseObj.getInfos();
        for (int step = 1; step < infos.size() + 1; step++) {
            printInfo(infos.get(step));

            installOptions.setStep(step);
            response = installationManagerProxy.install(requestRep);

            if (responseObj.getStatus() == ResponseCode.ERROR) {
                printError(response);
                return null;
            }
        }

        printResponse(response);
        responseObj = Response.fromJson(response);

        if (isIMSuccessfullyUpdated(responseObj)) {
            pressAnyKey("'Installation Manager CLI' is being updated! Press any key to exit...\n");
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
        StringBuilder newResponse = new StringBuilder(response);
        String clientVersionInfo = format("  \"CLI client version\" : \"%s\",\n", getClientBuildVersion());
        newResponse.insert(2, clientVersionInfo);
        return newResponse.toString();
    }

    private boolean isIMSuccessfullyUpdated(Response response) {
        List<ArtifactInfo> artifacts = response.getArtifacts();
        if (artifacts != null) {
            for (ArtifactInfo artifact : artifacts) {
                if (InstallManagerArtifact.NAME.equals(artifact.getArtifact()) && artifact.getStatus() == Status.SUCCESS) {
                    return true;
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


    private JacksonRepresentation<Request> prepareRequest(InstallOptions installOptions) {
        return new Request().setArtifactName(artifactName)
                            .setVersion(version)
                            .setUserCredentials(getCredentials())
                            .setInstallOptions(installOptions)
                            .toRepresentation();
    }

    private InstallOptions askAdditionalInstallOptions() throws ArtifactNotFoundException {
        switch (artifactName) {
            case InstallManagerArtifact.NAME:
                return new DefaultOptions();
            case CDECArtifact.NAME:
                // we can ask for additional options
                return new CdecInstallOptions().setCdecInstallType(CdecInstallOptions.CDECInstallType.SINGLE_NODE);
            default:
                throw new ArtifactNotFoundException(artifactName);
        }
    }
}
