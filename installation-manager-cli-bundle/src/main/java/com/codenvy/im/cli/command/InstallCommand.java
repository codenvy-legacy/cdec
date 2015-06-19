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
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.response.InstallArtifactInfo;
import com.codenvy.im.response.InstallArtifactStatus;
import com.codenvy.im.response.InstallArtifactStepInfo;
import com.codenvy.im.response.InstallResponse;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.managers.Config.isEmpty;
import static com.codenvy.im.managers.Config.isMandatory;
import static com.codenvy.im.managers.Config.isValidForMandatoryProperty;
import static com.codenvy.im.utils.Commons.toJson;
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

    private final ConfigManager configManager;
    private       InstallType   installType;

    @Argument(index = 0, name = "artifact", description = "The name of the specific artifact to install.", required = false, multiValued = false)
    protected String artifactName;

    @Argument(index = 1, name = "version", description = "The specific version of the artifact to install", required = false, multiValued = false)
    private String versionNumber;

    @Option(name = "--list", aliases = "-l", description = "To show installed list of artifacts", required = false)
    private boolean list;

    @Option(name = "--multi", aliases = "-m", description = "To install artifact on multiply nodes (by default on single node)", required = false)
    private boolean multi;

    @Option(name = "--config", aliases = "-c", description = "Path to the configuration file", required = false)
    private String configFilePath;

    @Option(name = "--step", aliases = "-s", description = "Particular installation step to perform", required = false)
    private String installStep;

    @Option(name = "--force", aliases = "-f", description = "Force installation in case of splitting process by steps", required = false)
    private boolean force;

    public InstallCommand() {
        this.configManager = INJECTOR.getInstance(ConfigManager.class);
    }

    /** For testing purpose only */
    @Deprecated
    InstallCommand(ConfigManager configManager) {
        this.configManager = configManager;
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

        final Artifact artifact = createArtifact(artifactName);
        final Version version = versionNumber != null ? Version.valueOf(versionNumber) : artifact.getLatestInstallableVersion();
        if (version == null) {
            throw new IllegalStateException("There is no new version to install");
        }
        versionNumber = version.toString();

        final InstallOptions installOptions = new InstallOptions();
        final boolean isInstall = isInstall(artifact);

        if (multi) {
            installType = InstallType.MULTI_SERVER;
        } else {
            installType = InstallType.SINGLE_SERVER;
        }

        installOptions.setInstallType(installType);
        setInstallProperties(installOptions, isInstall);

        if (!installOptions.checkValid()) {
            enterMandatoryOptions(installOptions);
            confirmOrReenterOptions(installOptions);
        }


        List<String> infos;
        if (isInstall) {
            infos = facade.getInstallInfo(artifact, installType);
        } else {
            infos = facade.getUpdateInfo(artifact, installType);
        }

        final int finalStep = infos.size() - 1;
        final int firstStep = getFirstInstallStep();
        final int lastStep = getLastInstallationStep(finalStep);

        int maxInfoLen = 0;
        for (String i : infos) {
            maxInfoLen = max(maxInfoLen, i.length());
        }

        InstallArtifactInfo installArtifactInfo = new InstallArtifactInfo();
        installArtifactInfo.setArtifact(artifactName);
        installArtifactInfo.setVersion(versionNumber);
        installArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);

        InstallResponse installResponse = new InstallResponse();
        installResponse.setStatus(ResponseCode.OK);
        installResponse.setArtifacts(ImmutableList.of(installArtifactInfo));


        for (int step = firstStep; step <= lastStep; step++) {
            String info = infos.get(step);
            console.print(info);
            console.printWithoutCodenvyPrompt(new String(new char[maxInfoLen - info.length()]).replace("\0", " "));

            console.showProgressor();

            try {
                installOptions.setStep(step);

                try {
                    String stepId;
                    if (isInstall) {
                        stepId = facade.install(artifact, version, installOptions);
                    } else {
                        stepId = facade.update(artifact, version, installOptions);
                    }
                    facade.waitForInstallStepCompleted(stepId);
                    InstallArtifactStepInfo updateStepInfo = facade.getUpdateStepInfo(stepId);
                    if (updateStepInfo.getStatus() == InstallArtifactStatus.FAILURE) {
                        installResponse.setStatus(ResponseCode.ERROR);
                        installResponse.setMessage(updateStepInfo.getMessage());
                    }
                } catch (Exception e) {
                    installArtifactInfo.setStatus(InstallArtifactStatus.FAILURE);
                    installResponse.setStatus(ResponseCode.ERROR);
                    installResponse.setMessage(e.getMessage());
                }

                if (installResponse.getStatus() == ResponseCode.ERROR) {
                    console.printError(" [FAIL]", true);
                    console.printResponseExitInError(installResponse);
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
            console.println(toJson(installResponse));

            if (isInteractive() && artifactName.equals(InstallManagerArtifact.NAME)) {
                console.pressAnyKey("'Installation Manager CLI' is being updated! Press any key to exit...\n");
                console.exit(0);
            }
        }

        return null;
    }

    protected Void doExecuteListInstalledArtifacts() throws IOException, JsonParseException {
        Collection<InstallArtifactInfo> installedVersions = facade.getInstalledVersions();
        InstallResponse installResponse = new InstallResponse();
        installResponse.setArtifacts(installedVersions);
        installResponse.setStatus(ResponseCode.OK);
        console.printResponseExitInError(installResponse);
        return null;
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

    protected void setInstallProperties(InstallOptions options, boolean isInstall) throws IOException {
        switch (artifactName) {
            case InstallManagerArtifact.NAME:
                options.setCliUserHomeDir(System.getProperty("user.home"));
                break;

            case CDECArtifact.NAME:
                Map<String, String> properties = configManager.prepareInstallProperties(configFilePath,
                                                                                        installType,
                                                                                        createArtifact(artifactName),
                                                                                        Version.valueOf(versionNumber),
                                                                                        isInstall);
                options.setConfigProperties(properties);
                break;
            default:
                throw ArtifactNotFoundException.from(artifactName);
        }
    }

    protected boolean isInstall(Artifact artifact) throws IOException {
        return (installStep != null && force) || artifact.getInstalledVersion() == null;
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
