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
package com.codenvy.im.response;

import com.codenvy.im.artifacts.CDECArtifact;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/** @author Dmytro Nochevnov */
public class TestBackupInfo {
    @Test
    public void testBackupInfo() {
        String testFile = "test";
        ArtifactInfo artifactInfo = new ArtifactInfo().setArtifact(CDECArtifact.NAME)
                                                      .setVersion("1.0.0");

        BackupInfo info = new BackupInfo().setArtifactInfo(artifactInfo)
                                          .setFile(testFile)
                                          .setStatus(ArtifactStatus.SUCCESS);

        assertEquals(info.getArtifactInfo(), artifactInfo);
        assertEquals(info.getFile(), testFile);
        assertEquals(info.getStatus(), ArtifactStatus.SUCCESS);
    }
}
