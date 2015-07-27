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
package com.codenvy.im.artifacts;

import com.codenvy.im.artifacts.helper.CDECArtifactHelper;
import com.codenvy.im.artifacts.helper.CDECMultiServerHelper;
import com.codenvy.im.artifacts.helper.CDECSingleServerHelper;
import com.codenvy.im.commands.Command;
import com.codenvy.im.commands.CommandException;
import com.codenvy.im.commands.SimpleCommand;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.PropertiesNotFoundException;
import com.codenvy.im.managers.UnknownInstallationTypeException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.rest.shared.dto.ApiInfo;

import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.commands.SimpleCommand.createCommand;
import static com.codenvy.im.utils.Commons.createDtoFromJson;
import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "codenvy";
    private final Map<InstallType, CDECArtifactHelper> helpers = ImmutableMap.of(
            InstallType.SINGLE_SERVER, new CDECSingleServerHelper(this, configManager),
            InstallType.MULTI_SERVER, new CDECMultiServerHelper(this, configManager));

    protected final String assemblyProperties;

    @Inject
    public CDECArtifact(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                        @Named("installation-manager.download_dir") String downloadDir,
                        @Named("installation-manager.assembly_properties") String assemblyProperties,
                        HttpTransport transport,
                        ConfigManager configManager) {
        super(NAME, updateEndpoint, downloadDir, transport, configManager);
        this.assemblyProperties = assemblyProperties;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public Version getInstalledVersion() throws IOException {
        try {
            ApiInfo apiInfo;
            try {
                String response = transport.doOption(configManager.getApiEndpoint() + "/", null);
                apiInfo = createDtoFromJson(response, ApiInfo.class);
                if (apiInfo == null) {
                    return null; // server is down
                }
            } catch (IOException e) {
                return null; // server is down
            }

            Version version = fetchAssemblyVersion();
            if (version != null) {
                return version;
            }

            if (apiInfo.getIdeVersion().contains("codenvy.ide.version")) {
                return fetchVersionFromPuppetConfig(); // workaround
            } else {
                return Version.valueOf(apiInfo.getIdeVersion());
            }
        } catch (UnknownInstallationTypeException | IOException e) {
            return null;
        }
    }

    @Nullable
    protected Version fetchVersionFromPuppetConfig() throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        String value = config.getValue(Config.VERSION);
        return value == null ? null : Version.valueOf(value);
    }

    @Nullable
    protected Version fetchAssemblyVersion() throws CommandException {
        String cmd = format("if sudo test -f %1$s; then " +
                            "   sudo cat %1$s " +
                            "       | grep assembly.version " +
                            "       | sed 's/assembly.version\\W*=\\W*\\(.*\\)/\\1/';" +
                            "fi", assemblyProperties);
        if (!assemblyProperties.startsWith("/")) { // make it works for tests
            cmd = cmd.replace("sudo", "");
        }

        SimpleCommand command = createCommand(cmd);
        String result = command.execute().trim();
        return result.isEmpty() ? null : Version.valueOf(result);
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 10;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUpdateInfo(InstallType installType) throws IOException {
        if (installType != configManager.detectInstallationType()) {
            throw new IllegalArgumentException("Only update to the Codenvy of the same installation type is supported");
        }

        return ImmutableList.of("Unzip Codenvy binaries to /tmp/codenvy",
                                "Configure Codenvy",
                                "Patch resources before update",
                                "Move Codenvy binaries to /etc/puppet",
                                "Update Codenvy",
                                "Patch resources after update");
    }

    /** {@inheritDoc} */
    @Override
    public Command getUpdateCommand(Version versionToUpdate,
                                    Path pathToBinaries,
                                    InstallOptions installOptions) throws IOException, IllegalArgumentException {
        if (installOptions.getInstallType() != configManager.detectInstallationType()) {
            throw new IllegalArgumentException("Only update to the Codenvy of the same installation type is supported");
        }

        return getHelper(installOptions.getInstallType()).getUpdateCommand(versionToUpdate, pathToBinaries, installOptions);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(InstallType installType) throws IOException {
        return getHelper(installType).getInstallInfo();
    }

    /** {@inheritDoc} */
    @Override
    public Command getInstallCommand(final Version versionToInstall,
                                     final Path pathToBinaries,
                                     final InstallOptions installOptions) throws IOException {

        return getHelper(installOptions.getInstallType())
                .getInstallCommand(versionToInstall, pathToBinaries, installOptions);
    }

    /** {@inheritDoc} */
    @Override
    public Command getBackupCommand(BackupConfig backupConfig) throws IOException {
        CDECArtifactHelper helper = getHelper(configManager.detectInstallationType());
        return helper.getBackupCommand(backupConfig);
    }

    /** {@inheritDoc} */
    @Override
    public Command getRestoreCommand(BackupConfig backupConfig) throws IOException {
        CDECArtifactHelper helper = getHelper(configManager.detectInstallationType());
        return helper.getRestoreCommand(backupConfig);
    }

    /** {@inheritDoc} */
    @Override
    public void updateConfig(Map<String, String> propertiesToUpdate) throws IOException {
        Config config = configManager.loadInstalledCodenvyConfig();
        Map<String, String> actualProperties = config.getProperties();

        // check if there are nonexistent property among propertiesToUpdate
        List<String> nonexistentProperties = new ArrayList<>();
        for (String property : propertiesToUpdate.keySet()) {
            if (! actualProperties.containsKey(property)) {
                nonexistentProperties.add(property);
            }
        }

        if (nonexistentProperties.size() != 0) {
            throw new PropertiesNotFoundException(nonexistentProperties);
        }

        CDECArtifactHelper helper = getHelper(configManager.detectInstallationType());
        Command commands = helper.getUpdateConfigCommand(config, propertiesToUpdate);
        commands.execute();
    }

    protected CDECArtifactHelper getHelper(InstallType type) {
        return helpers.get(type);
    }
}
