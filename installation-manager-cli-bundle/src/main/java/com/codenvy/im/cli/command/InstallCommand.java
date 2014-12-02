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
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.config.ConfigProperty;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.utils.Commons.toJson;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-install", description = "Install, update artifact or print the list of already installed ones")
public class InstallCommand extends AbstractIMCommand {

    private final ConfigFactory configFactory;

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Option(name = "--config", aliases = "-c", description = "Path to configuration file", required = false)
    private String configFilePath;

    public InstallCommand() {
        this.configFactory = INJECTOR.getInstance(ConfigFactory.class);
    }

    /** For testing purpose only */
    @Deprecated
    InstallCommand(ConfigFactory configFactory) {
        this.configFactory = configFactory;
    }

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
            exitIfNotInteractive();
        }

        return null;
    }

    private Void doExecuteInstall() throws JSONException, IOException, JsonParseException {
        if (artifactName == null) {
            printError("Argument 'artifact' is required.");
            return null;
        }

        InstallOptions installOptions = new InstallOptions();
        enterInstallOptions(installOptions);
        confirmOrReenterInstallOptions(installOptions);

        String response = installationManagerProxy.getInstallInfo(prepareRequest(installOptions));
        Response responseObj = Response.fromJson(response);

        if (responseObj.getStatus() == ResponseCode.ERROR) {
            printError(response);
            exitIfNotInteractive();
            return null;
        }

        List<String> infos = responseObj.getInfos();
        for (int step = 0; step < infos.size(); step++) {
            printInfo(infos.get(step));

            installOptions.setStep(step);
            response = installationManagerProxy.install(prepareRequest(installOptions));
            responseObj = Response.fromJson(response);

            if (responseObj.getStatus() == ResponseCode.ERROR) {
                printInfo(" [FAIL]\n");
                printError(response);
                exitIfNotInteractive();
                return null;
            } else {
                printInfo(" [OK]\n");
            }
        }

        // only OK response can be here
        printResponse(response);
        responseObj = Response.fromJson(response);

        if (isInteractive() && isIMSuccessfullyUpdated(responseObj)) {
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

    protected InstallOptions enterInstallOptions(InstallOptions options) throws IOException {
        switch (artifactName) {
            case InstallManagerArtifact.NAME:
                return options.setCliUserHomeDir(System.getProperty("user.home"));

            case CDECArtifact.NAME:
                options.setInstallType(InstallOptions.InstallType.CDEC_SINGLE_NODE);

                if (options.getConfigProperties() == null) {
                    setOptionsFromConfig(options);
                }


                printInfo("Please, enter CDEC required parameters:\n");

                Map<String, String> m = new HashMap<>();

                for (ConfigProperty property : CdecConfig.Property.values()) {
                    printInfo(Config.getPropertyName(property));

                    String propName = property.toString().toLowerCase();
                    String currentValue = options.getConfigProperties().get(propName);

                    if (!isEmpty(currentValue)) {
                        printInfo(String.format(" (%s)", currentValue));
                    }
                    for (; ; ) {
                        String newValue = readLine(": ");

                        if (!isEmpty(newValue)) {
                            m.put(propName, newValue);
                            break;
                        } else if (!isEmpty(currentValue)) {
                            m.put(propName, currentValue);
                            break;
                        }
                    }
                }

                return options.setConfigProperties(m);
            default:
                throw ArtifactNotFoundException.from(artifactName);
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private void setOptionsFromConfig(InstallOptions options) throws IOException {
        if (configFilePath != null) {
            Map<String, String> m = configFactory.loadConfigProperties(Paths.get(configFilePath));
            options.setConfigProperties(m);
        } else {
            CdecConfig config = (CdecConfig)configFactory.loadOrCreateConfig(options);
            options.setConfigProperties(config.getProperties());
        }
    }

    protected void confirmOrReenterInstallOptions(InstallOptions installOptions) throws IOException {
        if (installOptions.getConfigProperties() == null) {
            return;
        }

        for (; ; ) {
            printInfo(toJson(installOptions.getConfigProperties()) + "\n");
            if (askUser("Continue installation")) {
                break;
            }

            enterInstallOptions(installOptions);
        }

        configFactory.writeConfig(new CdecConfig(installOptions.getConfigProperties()));
    }
}
