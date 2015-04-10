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

import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.config.Config;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
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
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codenvy.im.config.Config.isEmpty;
import static com.codenvy.im.config.Config.isMandatory;
import static com.codenvy.im.config.Config.isValidForMandatoryProperty;
import static com.codenvy.im.utils.Commons.toJsonWithSortedAndAlignedProperties;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.Thread.sleep;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-install", description = "Install, update artifact or print the list of already installed ones")
public class InstallCommand extends AbstractIMCommand {

    private static final Pattern VARIABLE_TEMPLATE = Pattern.compile("\\$\\{([^\\}]*)\\}"); // ${...}

    private final ConfigUtil  configUtil;
    private       InstallType installType;

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    protected String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String version;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Option(name = "--multi", aliases = "-m", description = "To install artifact on multiply nodes (by default on single node)", required = false)
    private boolean multi;

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
            doExecuteListInstalledArtifacts();
        } else {
            doExecuteInstall();
        }
    }

    protected Void doExecuteInstall() throws IOException, JsonParseException, InterruptedException {
        if (artifactName == null) {
            artifactName = CDECArtifact.NAME;
        }

        final InstallOptions installOptions = new InstallOptions();
        if (version == null) {
            installOptions.setStep(getFirstInstallStep());
            version = facade.getVersionToInstall(initRequest(artifactName, null).setInstallOptions(installOptions));
        }

        if (multi) {
            installType = InstallType.MULTI_SERVER;
        } else {
            installType = InstallType.SINGLE_SERVER;
        }

        final Request request = initRequest(artifactName, version);

        setOptionsFromConfig(installOptions);

        if (!installOptions.checkValid()) {
            enterMandatoryOptions(installOptions);
            confirmOrReenterOptions(installOptions);
        }

        String response = facade.getInstallInfo(request.setInstallOptions(installOptions));
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

                response = facade.install(request.setInstallOptions(installOptions));
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

    protected Void doExecuteListInstalledArtifacts() throws IOException, JsonParseException {
        String response = facade.getInstalledVersions(initRequest(artifactName, version));
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

    protected InstallOptions enterMandatoryOptions(InstallOptions options) throws IOException {
        if (options.getConfigProperties().isEmpty()) {
            return options;
        }

        Map<String, String> m = new TreeMap<>(options.getConfigProperties());    // ask properties in alphabetical order
        switch (artifactName) {
            case CDECArtifact.NAME:
                console.println("Please, enter mandatory Codenvy parameters (values cannot be left blank):");
                for (Map.Entry<String, String> e : m.entrySet()) {
                    String propName = e.getKey().toLowerCase();
                    String currentValue = e.getValue();

                    if (isMandatory(currentValue)) {
                        String newValue = "";
                        while (!isValidForMandatoryProperty(newValue)) {
                            console.print(propName);
                            console.printWithoutCodenvyPrompt(": ");
                            newValue = console.readLine();
                        }
                        m.put(propName, newValue);
                    } else {
                        m.put(propName, currentValue);
                    }
                }

                options.setConfigProperties(m);
        }

        return options;
    }

    protected InstallOptions enterAllOptions(InstallOptions options) throws IOException {
        if (options.getConfigProperties().isEmpty()) {
            return options;
        }

        Map<String, String> m = new TreeMap<>(options.getConfigProperties());    // ask properties in alphabetical order
        switch (artifactName) {
            case CDECArtifact.NAME:
                console.println("Please, enter Codenvy parameters (just press 'Enter' key to keep value as is):");

                for (Map.Entry<String, String> e : m.entrySet()) {
                    String propName = e.getKey().toLowerCase();
                    String currentValue = e.getValue();

                    console.print(propName);
                    console.printWithoutCodenvyPrompt(format(" (value='%s'): ", currentValue));

                    String newValue = console.readLine();
                    if (!isEmpty(newValue) && !isMandatory(newValue)) {
                        m.put(propName, newValue);
                    } else {
                        m.put(propName, currentValue);
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
                options.setInstallType(installType);

                Map<String, String> properties;
                if (configFilePath != null) {
                    properties = configUtil.loadConfigProperties(configFilePath);
                } else {
                    if (isInstall()) {
                        properties = configUtil.loadCodenvyDefaultProperties(version, installType);
                    } else {
                        properties = configUtil.merge(configUtil.loadInstalledCodenvyProperties(installType),
                                                      configUtil.loadCodenvyDefaultProperties(version, installType));
                        properties.put(Config.VERSION, version);

                        if (installType == InstallType.MULTI_SERVER) {
                            properties.put("puppet_master_host_name", configUtil.fetchMasterHostName());  // restore host name of puppet master
                        }
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

    public void confirmOrReenterOptions(InstallOptions installOptions) throws IOException, InterruptedException {
        if (installOptions.getConfigProperties() == null) {
            return;
        }

        for (; ; ) {
            console.println();
            console.println("Codenvy parameters list:");
            console.println(toJsonWithSortedAndAlignedProperties(installOptions.getConfigProperties()));
            sleep(1500);   // pause reading keyboard 1,5 sec to allow user to stop before confirming parameters list
            console.reset();
            if (console.askUser("Do you confirm parameters above?")) {
                break;
            }

            enterAllOptions(installOptions);
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
