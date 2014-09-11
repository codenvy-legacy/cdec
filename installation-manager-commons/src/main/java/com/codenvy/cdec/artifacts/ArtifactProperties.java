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
package com.codenvy.cdec.artifacts;

import java.util.HashSet;
import java.util.Set;

/** @author Dmytro Nochevnov */
public class ArtifactProperties {
    public static final String VERSION_PROPERTY                 = "version";
    public static final String ARTIFACT_PROPERTY                = "artifact";
    public static final String BUILD_TIME_PROPERTY              = "build-time";
    public static final String FILE_NAME_PROPERTY               = "file";
    public static final String AUTHENTICATION_REQUIRED_PROPERTY = "authentication-required";
    public static final String SUBSCRIPTION_PROPERTY            = "subscription";

    public static final Set<String> PUBLIC_PROPERTIES = new HashSet<String>() {{
        add(VERSION_PROPERTY);
        add(ARTIFACT_PROPERTY);
        add(BUILD_TIME_PROPERTY);
        add(AUTHENTICATION_REQUIRED_PROPERTY);
    }};
}
