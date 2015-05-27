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

import com.codenvy.im.BaseTest;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import org.testng.annotations.Test;

import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class DownloadProgressDescriptorTest extends BaseTest {

    @Test
    public void test() throws Exception {
        DownloadArtifactDescriptor artifactInfo = new DownloadArtifactDescriptor(ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                 Version.valueOf("1.0.1"),
                                                                                 Paths.get(DOWNLOAD_DIR).resolve("file"),
                                                                                 DownloadArtifactStatus.DOWNLOADED);

        DownloadProgressDescriptor downloadProgressDescriptor = new DownloadProgressDescriptor(DownloadArtifactStatus.DOWNLOADED,
                                                                                               "message",
                                                                                               100,
                                                                                               ImmutableList.of(artifactInfo));

        assertEquals(downloadProgressDescriptor.getStatus(), DownloadArtifactStatus.DOWNLOADED);
        assertEquals(downloadProgressDescriptor.getPercents(), 100);
        assertEquals(downloadProgressDescriptor.getMessage(), "message");
        assertEquals(downloadProgressDescriptor.getArtifacts().size(), 1);
        assertEquals(downloadProgressDescriptor.getArtifacts().get(0), artifactInfo);
        assertEquals(Commons.toJson(downloadProgressDescriptor), "{\n" +
                                                                 "  \"artifacts\" : [ {\n" +
                                                                 "    \"artifact\" : \"codenvy\",\n" +
                                                                 "    \"version\" : \"1.0.1\",\n" +
                                                                 "    \"file\" : \"target/download/file\",\n" +
                                                                 "    \"status\" : \"DOWNLOADED\"\n" +
                                                                 "  } ],\n" +
                                                                 "  \"percents\" : 100,\n" +
                                                                 "  \"message\" : \"message\",\n" +
                                                                 "  \"status\" : \"DOWNLOADED\"\n" +
                                                                 "}");
    }
}
