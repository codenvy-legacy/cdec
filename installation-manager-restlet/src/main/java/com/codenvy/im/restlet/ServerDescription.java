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
    public static final String SERVER_URL        = InjectorBootstrap.getProperty("installation-manager.restlet.server_url");
    public static final String LOGIN             = InjectorBootstrap.getProperty("installation-manager.restlet.server_login");
    public static final char[] PASSWORD          = InjectorBootstrap.getProperty("installation-manager.restlet.server_password").toCharArray();
    public static final String REALM             = InjectorBootstrap.getProperty("installation-manager.restlet.server_realm");
    public static final String SERVER_DIGEST_KEY = InjectorBootstrap.getProperty("installation-manager.restlet.server_digest_key");

}
