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

/**
 * @author Dmytro Nochevnov</a>
 */
public class ServerDescription {
    public static final String LOGIN             = "im";
    // public static final char[] PASSWORD          = getRandomPassword();  // TODO uncomment
    public static final char[] PASSWORD          = "secret".toCharArray();

    public static final String REALM             = "im-realm";
    public static final String SERVER_DIGEST_KEY = "imSecretServerKey";     // TODO

    public static final int    PORT              = 8182;
    public static final String SERVER_ADDRESS    = "localhost";
    public static final String BASE_URI          = "http://" + SERVER_ADDRESS + ":" + PORT;

    private static char[] getRandomPassword() {
        String randomNumber = Double.toString(Math.random());
        char[] passwd = randomNumber.toCharArray();
        
        return passwd;
    }
}
