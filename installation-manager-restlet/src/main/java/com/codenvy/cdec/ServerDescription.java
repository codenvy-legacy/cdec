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
package com.codenvy.cdec;

import com.codenvy.cdec.utils.InjectorBootstrap;

import java.util.UUID;

/**
 * @author Dmytro Nochevnov
 */
public class ServerDescription {
    public static final String LOGIN    = "im";
    public static final char[] PASSWORD = UUID.randomUUID().toString().toCharArray();

    public static final String REALM             = "im-realm";
    public static final String SERVER_DIGEST_KEY = "imSecretServerKey";

    // public static final String SERVER_URL = InjectorBootstrap.getProperty("codenvy.restlet.server_url");  // TODO commented because it throws java.lang.ExceptionInInitializerError in CLI client
    
    public static final String SERVER_URL = "http://localhost:8182";

}
