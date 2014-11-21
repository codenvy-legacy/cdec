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
 * @author Anatoliy Bazko
 */
public class DefaultOptions implements InstallOptions {

    private int step;

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
