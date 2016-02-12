/*
 *  [2012] - [2016] Codenvy, S.A.
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
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.VersionLabel;
import com.codenvy.im.utils.Version;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;

/**
 * @author Anatoliy Bazko
 */
public abstract class BasicArtifactInfo implements Info, Comparable<BasicArtifactInfo> {

    public abstract String getArtifact();

    public abstract String getVersion();

    public abstract void setLabel(VersionLabel label);

    /** {@inheritDoc} */
    @Override
    public int compareTo(BasicArtifactInfo o) {
        if (!getArtifact().equals(o.getArtifact())) {
            try {
                Artifact thisArtifact = createArtifact(getArtifact());
                Artifact thatArtifact = createArtifact(o.getArtifact());
                return thatArtifact.compareTo(thisArtifact);
            } catch (ArtifactNotFoundException e) {
                throw new IllegalStateException(e);
            }
        } else {
            Version thisVersion = Version.valueOf(getVersion());
            Version thatVersion = Version.valueOf(o.getVersion());

            return thatVersion.compareTo(thisVersion);
        }
    }

    /** {@inheritDoc} */
    @Override
    public abstract boolean equals(Object obj);

    /** {@inheritDoc} */
    @Override
    public abstract int hashCode();
}
