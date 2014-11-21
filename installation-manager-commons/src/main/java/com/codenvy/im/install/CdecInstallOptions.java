/*
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
package com.codenvy.im.install;

/**
 * @author Dmytro Nochevnov
 */
public class CdecInstallOptions implements InstallOptions {
    private int             step;
    private CDECInstallType cdecInstallType;

    /** Getter for {@link #cdecInstallType} */
    public CDECInstallType getCdecInstallType() {
        return cdecInstallType;
    }

    /** Setter for {@link #cdecInstallType} */
    public CdecInstallOptions setCdecInstallType(CDECInstallType cdecInstallType) {
        this.cdecInstallType = cdecInstallType;
        return this;
    }

    /** Installation types for {@link com.codenvy.im.artifacts.CDECArtifact} */
    public static enum CDECInstallType {
        SINGLE_NODE
    }

    /** Getter for {@link #step} */
    public int getStep() {
        return step;
    }

    /** Setter for {@link #step} */
    @Override
    public void setStep(int step) {
        this.step = step;
    }
}
