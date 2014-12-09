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
package com.codenvy.im.utils;

import com.google.inject.Inject;

import javax.inject.Named;
import javax.inject.Singleton;

/** @author Anatoliy Bazko */
@Singleton
public class HttpTransportConfiguration {

    private String proxyUrl;
    private int    proxyPort;

    @Inject
    public HttpTransportConfiguration(@Named("installation-manager.proxy_url") String proxyUrl,
                                      @Named("installation-manager.proxy_port") String proxyPort) {
        this.proxyUrl = proxyUrl;
        this.proxyPort = parseProxyPort(proxyPort);
    }

    private int parseProxyPort(String proxyPort) {
        try {
            return Integer.parseInt(proxyPort);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl == null ? null : proxyUrl.trim();
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = parseProxyPort(proxyPort);
    }

    public boolean isProxyConfValid() {
        return (proxyUrl != null) && (!proxyUrl.isEmpty()) && (proxyPort > 0);
    }
}
