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
package com.codenvy.cdec.response;

import com.codenvy.cdec.artifacts.Artifact;

/** @author Dmytro Nochevnov */
// TODO order
public class ArtifactInfo {
    private String artifact;
    private String version;

    public ArtifactInfo(String artifact, String version) {
        this.artifact = artifact;
        this.version = version;
    }

    public ArtifactInfo(Artifact artifact, String version) {
        this(artifact.getName(), version);
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

}
