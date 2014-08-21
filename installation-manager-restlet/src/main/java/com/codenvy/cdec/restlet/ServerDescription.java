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
package com.codenvy.cdec.restlet;

import com.codenvy.cdec.utils.InjectorBootstrap;

/**
 * @author Dmytro Nochevnov
 */
public class ServerDescription {
    public static final String LOGIN    = "im";
    public static final char[] PASSWORD = "cdec-secret".toCharArray();  // TODO store password in safe place and in safe view

    public static final String REALM             = "im-realm";
    public static final String SERVER_DIGEST_KEY = "imSecretServerKey";

    public static final String SERVER_URL = InjectorBootstrap.getProperty("codenvy.restlet.server_url");
}
