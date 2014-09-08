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
package com.codenvy.cdec.utils;

import static com.codenvy.cdec.utils.Commons.combinePaths;
import static com.codenvy.cdec.utils.Commons.fromJson;

import java.io.IOException;
import java.util.Map;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactProperties;
import com.codenvy.cdec.exceptions.ArtifactNotFoundException;

/** @author Dmytro Nochevnov */
public class ArtifactUtils {
    public static boolean isAuthenticationRequired(Artifact artifact, String version, HttpTransport transport, String updateEndpoint) throws IOException {
        Map<String, String> properties = getArtifactProperties(artifact, version, transport, updateEndpoint);
        return new Boolean(properties.get(ArtifactProperties.AUTHENTICATION_REQUIRED_PROPERTY));
    }

    public static String getSubscription(Artifact artifact, String version, HttpTransport transport, String updateEndpoint) throws IOException {
        Map<String, String> properties = getArtifactProperties(artifact, version, transport, updateEndpoint);
        return properties.get(ArtifactProperties.SUBSCRIPTION_PROPERTY);
    }
    
    private static Map<String, String> getArtifactProperties(Artifact artifact, String version, HttpTransport transport, String updateEndpoint) throws IOException, ArtifactNotFoundException {
        String versionInfoServiceHref = combinePaths(updateEndpoint, "repository/info/" + artifact.getName() + "/" + version);
        Map<String, String> properties = (Map<String, String>) fromJson(transport.doGetRequest(versionInfoServiceHref), Map.class);
        if (properties == null) {
            throw new ArtifactNotFoundException(artifact.getName(), version);
        }
        return properties;
    }

}
