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
package com.codenvy.im.exceptions;

import java.io.FileNotFoundException;

/**
 * @author Anatoliy Bazko
 */
public class ArtifactNotFoundException extends FileNotFoundException {

    public ArtifactNotFoundException(String artifact, String version) {
        super("Artifact '" + artifact + "' version '" + version + "' not found");
    }

    public ArtifactNotFoundException(String artifact) {
        super(String.format("'%s' artifact not found", artifact));
    }
}
