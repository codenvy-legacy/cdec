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
package com.codenvy.im.install;

import com.codenvy.im.config.Config;

import java.util.Map;

/**
 * @author Anatoliy Bazko
 */
public class InstallOptions {

    private int                 step;
    private InstallType         installType;
    private Map<String, String> configProperties;
    private String cliUserHomeDir;

    /** Installation types for {@link com.codenvy.im.artifacts.CDECArtifact} */
    public static enum InstallType {
        CODENVY_SINGLE_SERVER,
        CODENVY_MULTI_SERVER
    }

    /** Getter for {@link #installType} */
    public InstallType getInstallType() {
        return installType;
    }

    /** Setter for {@link #installType} */
    public InstallOptions setInstallType(InstallType installType) {
        this.installType = installType;
        return this;
    }

    /** Getter for {@link #step} */
    public int getStep() {
        return step;
    }

    /** Setter for {@link #step} */
    public void setStep(int step) {
        this.step = step;
    }

    /** Getter for {@link #configProperties} */
    public Map<String, String> getConfigProperties() {
        return configProperties;
    }

    /** Setter for {@link #configProperties} */
    public InstallOptions setConfigProperties(Map<String, String> configProperties) {
        this.configProperties = configProperties;
        return this;
    }

    public String getCliUserHomeDir() {
        return cliUserHomeDir;
    }

    public InstallOptions setCliUserHomeDir(String cliUserHomeDir) {
        this.cliUserHomeDir = cliUserHomeDir;
        return this;
    }

    /** Indicates if all necessary options are set. */
    public boolean checkValid() {
        if (getInstallType() == InstallType.CODENVY_SINGLE_SERVER
            || getInstallType() == InstallType.CODENVY_MULTI_SERVER) {
            if (configProperties == null) {
                return false;
            }

            Config config = new Config(configProperties);
            return config.isValid();
        }

        return true;
    }
}
