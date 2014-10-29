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
package com.codenvy.im.config;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class PuppetConfig extends HostConfig {
    public static final String PUPPET_VERSION      = Property.PUPPET_VERSION.toString();
    public static final String PUPPET_RESOURCE_URL = Property.PUPPET_RESOURCE_URL.toString();

    enum Property implements ConfigProperty {
        PUPPET_VERSION,
        PUPPET_RESOURCE_URL
    }

    public String getPuppetVersion() {
        return getProperty(Property.PUPPET_VERSION);
    }

    public String getPuppetResourceUrl() {
        return getProperty(Property.PUPPET_RESOURCE_URL);
    }
}
