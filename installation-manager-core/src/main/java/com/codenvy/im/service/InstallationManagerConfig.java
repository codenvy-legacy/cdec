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
package com.codenvy.im.service;

/** @author Anatoliy Bazko */
public class InstallationManagerConfig {
    private String downloadDir;
    private String proxyPort;
    private String proxyUrl;

    public InstallationManagerConfig() {
    }

    /** Getter for #downloadDir */
    public String getDownloadDir() {
        return downloadDir;
    }

    /** Setter for #downloadDir */
    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    /** Getter for #proxyUrl */
    public String getProxyUrl() {
        return proxyUrl;
    }

    /** Setter for #proxyUrl */
    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    /** Getter for #proxyPort */
    public String getProxyPort() {
        return proxyPort;
    }

    /** Setter for #proxyPort */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean checkEmptyConfig() {
        return checkEmptyParam(downloadDir) && checkEmptyParam(proxyUrl) && checkEmptyParam(proxyPort);
    }

    private boolean checkEmptyParam(String param) {
        return param == null || param.isEmpty();
    }
}
