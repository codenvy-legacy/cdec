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
package com.codenvy.im.restlet;

import com.codenvy.im.utils.InjectorBootstrap;

/**
 * @author Dmytro Nochevnov
 */
public class ServerDescription {
    private ServerDescription() {
    }  // Prevents instantiation because this is constant utility class

    static final String SERVER_URL = InjectorBootstrap.getProperty("installation-manager.restlet.server_url");
    static final String LOGIN      = InjectorBootstrap.getProperty("installation-manager.restlet.server_login");
    static final char[] PASSWORD   = InjectorBootstrap.getProperty("installation-manager.restlet.server_password").toCharArray();
}
