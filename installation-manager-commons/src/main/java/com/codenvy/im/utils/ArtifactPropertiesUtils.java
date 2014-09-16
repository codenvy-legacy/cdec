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

import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.exceptions.ArtifactNotFoundException;

import java.io.IOException;
import java.util.Map;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.fromJson;

/** @author Dmytro Nochevnov */
public class ArtifactPropertiesUtils {

    /**
     * Utility class so no public constructor.
     */
    private ArtifactPropertiesUtils() {
        
    }
    
    public static boolean isAuthenticationRequired(String artifactName,
                                                   String version,
                                                   HttpTransport transport,
                                                   String updateEndpoint) throws IOException {
        Map<String, String> properties = getArtifactProperties(artifactName, version, transport, updateEndpoint);
        return Boolean.valueOf(properties.get(ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY));
    }

    public static String getSubscription(String artifactName,
                                         String version,
                                         HttpTransport transport,
                                         String updateEndpoint) throws IOException {
        Map<String, String> properties = getArtifactProperties(artifactName, version, transport, updateEndpoint);
        return properties.get(ArtifactProperties.SUBSCRIPTION_PROPERTY);
    }

    private static Map<String, String> getArtifactProperties(String artifactName,
                                                             String version,
                                                             HttpTransport transport,
                                                             String updateEndpoint) throws IOException {
        String versionInfoServiceHref = combinePaths(updateEndpoint, "repository/properties/" + artifactName + "/" + version);
        Map<String, String> properties = (Map<String, String>)fromJson(transport.doGetRequest(versionInfoServiceHref), Map.class);
        if (properties == null) {
            throw new ArtifactNotFoundException(artifactName, version);
        }
        return properties;
    }

}
