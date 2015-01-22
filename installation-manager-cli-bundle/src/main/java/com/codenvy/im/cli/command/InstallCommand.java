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
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.response.Status;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.config.Config.isEmpty;
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

    private final ConfigUtil configUtil;

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    private String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Option(name = "--config", aliases = "-c", description = "Path to the configuration file", required = false)
    private String configFilePath;

    @Option(name = "--step", aliases = "-s", description = "Particular installation step to perform", required = false)
    private String installStep;

    public InstallCommand() {
        this.configUtil = INJECTOR.getInstance(ConfigUtil.class);
    }

    /** For testing purpose only */
    @Deprecated
    InstallCommand(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

    @Override
    protected void doExecuteCommand() throws Exception {
        if (list) {
            doExecuteListOption();
        } else {
            doExecuteInstall();
        }
    }

    protected Void doExecuteInstall() throws JSONException, IOException, JsonParseException {
        if (artifactName == null) {
            artifactName = CDECArtifact.NAME;
        }

        if (version == null) {
            version = service.getVersionToInstall(initRequest(artifactName, null), getFirstInstallStep());
        }

        final Request request = initRequest(artifactName, version);

        final InstallOptions installOptions = new InstallOptions();
        setOptionsFromConfig(installOptions);

        if (!installOptions.checkValid()) {
            enterInstallOptions(installOptions, true);
            confirmOrReenterInstallOptions(installOptions);
        }

        String response = service.getInstallInfo(installOptions, request);
        Response responseObj = Response.fromJson(response);

        if (responseObj.getStatus() == ResponseCode.ERROR) {
            console.printErrorAndExit(response);
            return null;
        }

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
            console.printWithoutCodenvyPrompt(new String(new char[maxInfoLen - info.length()]).replace("\0", " "));

            console.showProgressor();

            try {
                installOptions.setStep(step);

                response = service.install(installOptions, request);
                responseObj = Response.fromJson(response);

                if (responseObj.getStatus() == ResponseCode.ERROR) {
                    console.printError(" [FAIL]", true);
                    console.printErrorAndExit(response);
                    return null;
                } else {
                    console.printSuccessWithoutCodenvyPrompt(" [OK]");
                }
            } finally {
                console.hideProgressor();
            }
        }

        // only OK response can be here
        if (lastStep == finalStep) {
            console.println(response);
            responseObj = Response.fromJson(response);

            if (isInteractive() && isIMSuccessfullyUpdated(responseObj)) {
                console.pressAnyKey("'Installation Manager CLI' is being updated! Press any key to exit...\n");
                exit(0);
            }
        }

        return null;
    }

    protected Void doExecuteListOption() throws IOException, JSONException, JsonParseException {
        String response = service.getInstalledVersions(initRequest(artifactName, version));
        console.printResponse(response);
        return null;
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

    protected InstallOptions enterInstallOptions(InstallOptions options, boolean askMissedOptionsOnly) throws IOException {
        switch (artifactName) {
            case CDECArtifact.NAME:
                console.println("Please, enter CDEC required parameters:");

                Map<String, String> m = new HashMap<>();

                for (Map.Entry<String, String> e : options.getConfigProperties().entrySet()) {
                    for (; ; ) {
                        String propName = e.getKey().toLowerCase();
                        String currentValue = e.getValue();

                        if (isEmpty(currentValue) || !askMissedOptionsOnly) {
                            console.print(propName);

                            if (!isEmpty(currentValue)) {
                                console.printWithoutCodenvyPrompt(format(" (%s)", currentValue));
                            }

                            console.printWithoutCodenvyPrompt(": ");
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
                    properties = configUtil.loadConfigProperties(configFilePath);
                } else {
                    if (isInstall()) {
                        properties = configUtil.loadCdecDefaultProperties(version);
                    } else {
                        properties = configUtil.merge(configUtil.loadInstalledCssProperties(),
                                                      configUtil.loadCdecDefaultProperties(version));
                        properties.put(Config.VERSION, version);
                    }
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

                properties.remove(Config.CODENVY_USER_NAME);
                properties.remove(Config.CODENVY_PASSWORD);

                break;
            default:
                throw ArtifactNotFoundException.from(artifactName);
        }
    }

    protected boolean isInstall() throws IOException {
        return Commons.isInstall(ArtifactFactory.createArtifact(CDECArtifact.NAME), Version.valueOf(version));
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

    private int getFirstInstallStep() {
        if (installStep == null) {
            return 0;
        } else {
            try {
                return Integer.parseInt(installStep.split("-")[0]) - 1;
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
                return Integer.parseInt(installStep.split("-")[0]) - 1;
            } else {
                return Integer.parseInt(installStep.split("-")[1]) - 1;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(format("Wrong installation step format '%s'", installStep));
        }
    }
}
