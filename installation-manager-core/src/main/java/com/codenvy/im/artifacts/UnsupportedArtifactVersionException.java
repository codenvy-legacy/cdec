/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.artifacts;

import com.codenvy.im.utils.Version;

/**
 * @author Dmytro Nochevnov
 */
public class UnsupportedArtifactVersionException extends RuntimeException {
    public UnsupportedArtifactVersionException(Artifact artifact, Version version) {
        super(String.format("Version '%1$s' of artifact '%2$s' is not supported", version, artifact.getName()));
    }
}
