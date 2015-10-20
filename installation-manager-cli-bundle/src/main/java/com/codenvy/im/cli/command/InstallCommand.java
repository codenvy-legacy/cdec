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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.event.EventFactory.createImArtifactInstallStartedWithTime;
import static com.codenvy.im.event.EventFactory.createImArtifactInstallSuccessWithTime;
import static com.codenvy.im.event.EventFactory.createImArtifactInstallUnsuccessWithTime;
import static com.codenvy.im.utils.Commons.toJson;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;
import static java.lang.Math.max;

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

    @Option(name = "--binaries", aliases = "-b", description = "Path to binaries to install", required = false)
    private String binaries;

    /** Could be */
    @Option(name = "--step", aliases = "-s", description = "Particular installation step to perform", required = false)
    private Integer installStep;

    @Option(name = "--forceInstall", aliases = "-fi", description = "Force installation in case of splitting process by steps", required = false)
    private boolean forceInstall;

    @Option(name = "--reinstall", aliases = "-r", description = "Re-install Codenvy (binaries only)", required = false)
    private boolean reinstall;

    public InstallCommand() {
        this.configManager = INJECTOR.getInstance(ConfigManager.class);
    }

    @Deprecated
    /**
     * For testing purpose only
     */
    InstallCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    protected void doExecuteCommand() throws Exception {
        if (list) {
            doExecuteListInstalledArtifacts();
        } else if (reinstall) {
            doExecuteReinstall();
        } else {
            doExecuteInstall();
        }
    }

    private void doExecuteReinstall() throws JsonParseException, JsonProcessingException {
        if (artifactName == null) {
            artifactName = CDECArtifact.NAME;
        }

        console.showProgressor();

        InstallArtifactInfo installArtifactInfo = new InstallArtifactInfo();
        installArtifactInfo.setArtifact(artifactName);

        InstallResponse installResponse = new InstallResponse();
        installResponse.setArtifacts(ImmutableList.of(installArtifactInfo));

        try {
            facade.reinstall(createArtifact(artifactName));
            installArtifactInfo.setStatus(InstallArtifactStatus.SUCCESS);
            installResponse.setStatus(ResponseCode.OK);
            console.println(toJson(installResponse));

        } catch (Exception e) {
            installArtifactInfo.setStatus(InstallArtifactStatus.FAILURE);
            installResponse.setStatus(ResponseCode.ERROR);
            installResponse.setMessage(e.getMessage());
            console.printResponseExitInError(installResponse);
        } finally {
            console.hideProgressor();
        }
    }

    protected Void doExecuteInstall() throws IOException, JsonParseException, InterruptedException {
        if (binaries != null) {
            if (versionNumber == null) {
                throw new IllegalStateException("Parameter 'version' is missed");
            } else if (artifactName == null) {
                throw new IllegalStateException("Parameter 'artifact' is missed");
            }
        }

        if (artifactName == null) {
            artifactName = CDECArtifact.NAME;
        }

        final Artifact artifact = createArtifact(artifactName);
        final Version version = versionNumber != null ? Version.valueOf(versionNumber) : facade.getLatestInstallableVersion(artifact);
        if (version == null) {
            throw new IllegalStateException("There is no new version to install");
        }
        versionNumber = version.toString();

        final InstallOptions installOptions = new InstallOptions();
        final boolean isInstall = isInstall(artifact);


        final int firstStep = getFirstInstallStep();

        if ((firstStep == 0) && isInstall) {
            logEventToSaasCodenvy(createImArtifactInstallStartedWithTime(artifactName, versionNumber));
        }

        if (isInstall) {
            if (multi) {
                installType = InstallType.MULTI_SERVER;
            } else {
                installType = InstallType.SINGLE_SERVER;
            }
        } else {
            installType = configManager.detectInstallationType();
        }

        installOptions.setInstallType(installType);
        setInstallProperties(installOptions, isInstall);

        List<String> infos;
        if (isInstall) {
            try {
                infos = facade.getInstallInfo(artifact, installType);
            } catch(Exception e) {
                logEventToSaasCodenvy(createImArtifactInstallUnsuccessWithTime(artifactName, versionNumber, e.getMessage()));
                throw e;
            }
        } else {
            infos = facade.getUpdateInfo(artifact, installType);
        }

        final int finalStep = infos.size() - 1;
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
                        stepId = binaries != null ? facade.install(artifact, version, Paths.get(binaries), installOptions)
                                                  : facade.install(artifact, version, installOptions);
                    } else {
                        stepId = binaries != null ? facade.update(artifact, version, Paths.get(binaries), installOptions)
                                                  : facade.update(artifact, version, installOptions);
                    }
                    facade.waitForInstallStepCompleted(stepId);
                    InstallArtifactStepInfo updateStepInfo = facade.getUpdateStepInfo(stepId);
                    if (updateStepInfo.getStatus() == InstallArtifactStatus.FAILURE) {
                        installResponse.setStatus(ResponseCode.ERROR);
                        installResponse.setMessage(updateStepInfo.getMessage());
                        installArtifactInfo.setStatus(InstallArtifactStatus.FAILURE);
                    }
                } catch (Exception e) {
                    installArtifactInfo.setStatus(InstallArtifactStatus.FAILURE);
                    installResponse.setStatus(ResponseCode.ERROR);
                    installResponse.setMessage(e.getMessage());
                }

                if (installResponse.getStatus() == ResponseCode.ERROR) {
                    if (isInstall) {
                        logEventToSaasCodenvy(createImArtifactInstallUnsuccessWithTime(artifactName, versionNumber, installResponse.getMessage()));
                    }

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
            if (isInstall) {
                logEventToSaasCodenvy(createImArtifactInstallSuccessWithTime(artifactName, versionNumber));
            }

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

    protected void setInstallProperties(InstallOptions options, boolean isInstall) throws IOException {
        if (artifactName.equals(InstallManagerArtifact.NAME)) {
            options.setCliUserHomeDir(System.getProperty("user.home"));
        }

        Map<String, String> properties = configManager.prepareInstallProperties(configFilePath,
                                                                                binaries == null ? null : Paths.get(binaries),
                                                                                installType,
                                                                                createArtifact(artifactName),
                                                                                Version.valueOf(versionNumber),
                                                                                isInstall);
        options.setConfigProperties(properties);
    }

    protected boolean isInstall(Artifact artifact) throws IOException {
        return (installStep != null && forceInstall)
               || !artifact.getInstalledVersion().isPresent();  // TODO [ndp] check is Codenvy alive
    }

    private int getFirstInstallStep() {
        if (installStep == null) {
            return 0;
        }

        return installStep - 1;
    }

    private int getLastInstallationStep(int maxStep) {
        if (installStep == null) {
            return maxStep;
        }

        return installStep - 1;
    }

}
