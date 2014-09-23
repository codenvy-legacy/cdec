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
package com.codenvy.im.response;

import com.codenvy.im.artifacts.Artifact;

/** @author Dmytro Nochevnov */
public class DownloadArtifactInfo extends ArtifactInfo {
    private Status status;
    private String file;

    public DownloadArtifactInfo(Artifact artifact, String version, String file, Status status) {
        this(artifact.getName(), version, file, status);
    }

    public DownloadArtifactInfo(String artifact, String version, String file, Status status) {
        super(artifact, version);
        this.file = file;
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public String getFile() {
        return file;
    }
}
