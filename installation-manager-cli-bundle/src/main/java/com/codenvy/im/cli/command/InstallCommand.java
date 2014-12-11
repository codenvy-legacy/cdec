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
import com.codenvy.im.config.CodenvySingleServerConfig;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.config.ConfigProperty;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;
import org.restlet.ext.jackson.JacksonRepresentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.config.Config.getPropertyName;
import static com.codenvy.im.config.Config.isEmpty;
import static com.codenvy.im.response.Response.isError;
import static com.codenvy.im.utils.Commons.toJsonWithSortedAndAlignedProperties;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.Math.max;
import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-install", description = "Install, update artifact or print the list of already installed ones")
public class InstallCommand extends AbstractIMCommand {

    private static final Pattern VARIABLE_TEMPLATE = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

    private final ConfigFactory configFactory;

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Option(name = "--config", aliases = "-c", description = "Path to configuration file", required = false)
    private String configFilePath;

    @Option(name = "--step", aliases = "-s", description = "Installation step to perform", required = false)
    private String installStep;

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
            console.printErrorAndExit(e);
        }

        return null;
    }

    protected Void doExecuteInstall() throws JSONException, IOException, JsonParseException {
        if (artifactName == null) {
            console.printErrorAndExit("Argument 'artifact' is required.");
            return null;
        }

        InstallOptions installOptions = new InstallOptions();
        setOptionsFromConfig(installOptions);

        if (!installOptions.checkValid()) {
            enterInstallOptions(installOptions, true);
            confirmOrReenterInstallOptions(installOptions);
        }

        String response = installationManagerProxy.getInstallInfo(prepareRequest(installOptions));
        if (isError(response)) {
            console.printErrorAndExit(response);
            return null;
        }

        Response responseObj = Response.fromJson(response);
        List<String> infos = responseObj.getInfos();
        final int finalStep = infos.size() - 1;
        final int firstStep = getFirstInstallStep();
        final int lastStep = getLastInstallationStep(finalStep);

        int maxInfoLen = 0;
        for (String i : infos) {
            maxInfoLen = max(maxInfoLen, i.length());
        }

        for (int step = firstStep; step <= lastStep; step++) {
            String info = infos.get(step);
            console.print(info);
            console.print(StringUtils.repeat(" ", maxInfoLen - info.length()));

            ShowProgress showProgress = new ShowProgress();
            showProgress.start();

            try {
                installOptions.setStep(step);

                response = installationManagerProxy.install(prepareRequest(installOptions));
                responseObj = Response.fromJson(response);

                if (responseObj.getStatus() == ResponseCode.ERROR) {
                    console.printError(" [FAIL]");
                    console.printErrorAndExit(response);
                    return null;
                } else {
                    console.printSuccess(" [OK]");
                }
            } finally {
                showProgress.interrupt();
            }
        }

        // only OK response can be here
        console.println(response);
        responseObj = Response.fromJson(response);

        if (isInteractive() && isIMSuccessfullyUpdated(responseObj)) {
            console.pressAnyKey("'Installation Manager CLI' is being updated! Press any key to exit...\n");
        }

        return null;
    }

    protected Void doExecuteListOption() throws IOException, JSONException, JsonParseException {
        String response = installationManagerProxy.getVersions(getCredentialsRep());
        console.printResponse(insertClientVersionInfo(response));
        return null;
    }

    protected String insertClientVersionInfo(String response) throws IOException {
        StringBuilder newResponse = new StringBuilder(response);
        String clientVersionInfo = format("  \"CLI client version\" : \"%s\",\n", getClientBuildVersion());
        newResponse.insert(2, clientVersionInfo);
        return newResponse.toString();
    }

    protected boolean isIMSuccessfullyUpdated(Response response) {
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

    protected InstallOptions enterInstallOptions(InstallOptions options, boolean askMissedOptionsOnly) throws IOException {
        switch (artifactName) {
            case CDECArtifact.NAME:
                console.println("Please, enter CDEC required parameters:");

                Map<String, String> m = new HashMap<>();

                for (ConfigProperty property : CodenvySingleServerConfig.Property.values()) {
                    for (; ; ) {
                        String propName = property.toString().toLowerCase();
                        String currentValue = options.getConfigProperties().get(propName);

                        if (isEmpty(currentValue) || !askMissedOptionsOnly) {
                            console.print(getPropertyName(property));

                            if (!isEmpty(currentValue)) {
                                console.print(format(" (%s)", currentValue));
                            }

                            console.print(": ");
                            String newValue = console.readLine();

                            if (!isEmpty(newValue)) {
                                m.put(propName, newValue);
                                break;
                            } else if (!isEmpty(currentValue)) {
                                m.put(propName, currentValue);
                                break;
                            }
                        } else {
                            m.put(propName, currentValue);
                            break;
                        }
                    }
                }

                options.setConfigProperties(m);
        }

        return options;
    }

    protected void setOptionsFromConfig(InstallOptions options) throws IOException {
        switch (artifactName) {
            case InstallManagerArtifact.NAME:
                options.setCliUserHomeDir(System.getProperty("user.home"));
                break;

            case CDECArtifact.NAME:
                options.setInstallType(InstallOptions.InstallType.CODENVY_SINGLE_SERVER);

                Map<String, String> properties;
                if (configFilePath != null) {
                    properties = configFactory.loadConfigProperties(configFilePath);
                } else {
                    properties = Collections.emptyMap();
                }
                options.setConfigProperties(properties);

                // it's allowed to use ${} templates to set properties values
                for (Map.Entry<String, String> e : options.getConfigProperties().entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();

                    Matcher matcher = VARIABLE_TEMPLATE.matcher(value);
                    if (matcher.find()) {
                        String newValue = properties.get(matcher.group(1));
                        properties.put(key, newValue);
                    }
                }

                break;

            default:
                throw ArtifactNotFoundException.from(artifactName);
        }
    }

    protected void confirmOrReenterInstallOptions(InstallOptions installOptions) throws IOException {
        if (installOptions.getConfigProperties() == null) {
            return;
        }

        for (; ; ) {
            console.println(toJsonWithSortedAndAlignedProperties(installOptions.getConfigProperties()));
            if (console.askUser("Continue installation")) {
                break;
            }

            enterInstallOptions(installOptions, false);
        }
    }

    /** Printing progress thread */
    private class ShowProgress extends Thread {
        final String[] progressChars = {"-", "\\", "|", "/", "-", "\\", "|", "/"};

        @Override
        public void run() {
            int step = 0;
            while (!isInterrupted()) {
                console.printProgress(progressChars[step]);
                try {
                    sleep(250);
                } catch (InterruptedException e) {
                    break;
                }

                step++;
                if (step == progressChars.length) {
                    step = 0;
                }
            }
        }
    }

    private int getFirstInstallStep() {
        if (installStep == null) {
            return 0;
        } else {
            try {
                return Integer.parseInt(installStep.split("-")[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(format("Wrong installation step format '%s'", installStep));
            }
        }
    }

    private int getLastInstallationStep(int maxStep) {
        try {
            if (installStep == null) {
                return maxStep;
            } else if (!installStep.contains("-")) {
                return Integer.parseInt(installStep.split("-")[0]);
            } else {
                return Integer.parseInt(installStep.split("-")[1]);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(format("Wrong installation step format '%s'", installStep));
        }
    }
}
