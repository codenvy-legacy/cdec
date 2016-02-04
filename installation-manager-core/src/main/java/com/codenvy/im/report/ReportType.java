/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.im.report;

import com.codenvy.im.utils.InjectorBootstrap;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author Dmytro Nochevnov
 */
public enum ReportType {
    CODENVY_ONPREM_USER_NUMBER_REPORT;

    public ReportParameters getParameters() {
        String parameterPrefix = this.name().toLowerCase();
        ReportParameters parameters = new ReportParameters(
            InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named(parameterPrefix + ".title"))),
            InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named(parameterPrefix + ".sender"))),
            InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named(parameterPrefix + ".receiver")))
        );
        return parameters;
    }
}
