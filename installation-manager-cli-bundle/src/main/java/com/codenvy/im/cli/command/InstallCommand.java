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
import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.installer.Installer;
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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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
              description = "The name of the specific artifact to install.", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    protected static final Installer.Type DEFAULT_INSTALL_TYPE = Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER;

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

        JacksonRepresentation<Request> requestRep = getRequestRep(DEFAULT_INSTALL_TYPE);
        String response = installationManagerProxy.install(requestRep);

        Response responseObj = Response.fromJson(response);
        if (responseObj.getStatus() == ResponseCode.ERROR) {
            printError(response);
            return null;
        }

        ArtifactInfo artifact = getInstallingArtifactInfo(responseObj);
        if ((artifact != null) && (artifact.getStatus() == Status.INSTALL_STARTED)) {
            return doInstallStepwisely(artifact);
        }

        printResponse(response);

        if (isIMSuccessfullyUpdated(artifact)) {
            printInfo("'Installation Manager CLI' is being updated! Press any key to exit...\n");

            session.getKeyboard().read();
            System.exit(0);
        }

        return null;
    }

    private Void doInstallStepwisely(ArtifactInfo artifact) throws IOException, JsonParseException {
        InstallOptions options = artifact.getInstallOptions();
        if (options == null) {
            return null;
        }

        List<String> commands = artifact.getInstallOptions().getCommandsInfo();
        if (commands == null) {
            return null;
        }

        printInfo(format("List of commands to install artifact '%s' version '%s': \n%s\n",
                         artifact.getArtifact(),
                         artifact.getVersion(),
                         Arrays.toString(commands.toArray()).replace(", ", ",\n")));

        if (!askUser("Start installation? [y/N]: ")) {
            return null;
        }

        for (String command: commands) {
            printInfo(format("Executing %s ...", command));

            JacksonRepresentation<Request> requestRep = getRequestRep(options.getId()); // do this before each call of install service
            String response = installationManagerProxy.install(requestRep);
            Response responseObj = Response.fromJson(response);

            if (responseObj.getStatus() == ResponseCode.OK) {
                printInfo(" Done.\n");
            } else {
                printError("\n" + responseObj.getMessage());
                if (!askUser("Continue installation? [y/N]: ")) {
                    printError("Installation has been interrupted.");
                    return null;
                }
            }
        }

        printInfo("Installation has been finished.\n");
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

    private boolean isIMSuccessfullyUpdated(ArtifactInfo artifact) {
        if (InstallManagerArtifact.NAME.equals(artifact.getArtifact())
            && artifact.getStatus() == Status.SUCCESS) {
            return true;
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

    private JacksonRepresentation<Request> getRequestRep(String installId) {
        return getRequestRep(installId, null);
    }

    private JacksonRepresentation<Request> getRequestRep(Installer.Type installType) {
        return getRequestRep(null, installType);
    }

    private JacksonRepresentation<Request> getRequestRep(@Nullable String installId, @Nullable Installer.Type installType) {
        InstallOptions options = new InstallOptions()
                                     .setId(installId)
                                     .setType(installType);

        return new Request().setArtifactName(artifactName)
                            .setVersion(version)
                            .setUserCredentials(getCredentials())
                            .setInstallOptions(options)
                            .toRepresentation();
    }

    @Nullable
    private ArtifactInfo getInstallingArtifactInfo(Response response) {
        List<ArtifactInfo> artifacts = response.getArtifacts();
        if (artifacts == null) {
            return null;
        }

        for (ArtifactInfo artifact: artifacts) {
            if (artifactName.equals(artifact.getArtifact())) {
                return artifact;
            }
        }

        return null;
    }

}
