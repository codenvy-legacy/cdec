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
            InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named(parameterPrefix + ".receiver"))),
            Boolean.valueOf(InjectorBootstrap.INJECTOR.getInstance(Key.get(String.class, Names.named(parameterPrefix + ".active"))))
        );
        return parameters;
    }
}
