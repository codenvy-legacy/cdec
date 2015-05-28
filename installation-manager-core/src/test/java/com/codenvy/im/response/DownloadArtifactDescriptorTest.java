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

import com.codenvy.im.utils.Commons;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


/**
 * @author Anatoliy Bazko
 */
public class DownloadArtifactDescriptorTest {

    @Test
    public void test() throws Exception {
        DownloadArtifactDescriptor expectedDescriptor = new DownloadArtifactDescriptor();
        expectedDescriptor.setVersion("1.0.1");
        expectedDescriptor.setArtifact("codenvy");
        expectedDescriptor.setFile("file");
        expectedDescriptor.setStatus(DownloadArtifactStatus.DOWNLOADED);


        String json = Commons.toJson(expectedDescriptor);

        DownloadArtifactDescriptor actualDescriptor = Commons.fromJson(json, DownloadArtifactDescriptor.class);

        assertEquals(actualDescriptor, expectedDescriptor);
    }
}