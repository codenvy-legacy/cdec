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
public class DownloadDescriptorTest extends BaseTest {

    @Test
    public void testToJson() throws Exception {
        DownloadArtifactDescriptor artifactInfo = new DownloadArtifactDescriptor(ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                 Version.valueOf("1.0.1"),
                                                                                 Paths.get(DOWNLOAD_DIR).resolve("file"),
                                                                                 DownloadArtifactStatus.DOWNLOADED);

        DownloadDescriptor downloadDescriptor = new DownloadDescriptor(ResponseCode.OK, ImmutableList.of(artifactInfo));

        assertEquals(downloadDescriptor.getStatus(), ResponseCode.OK);
        assertEquals(downloadDescriptor.getArtifacts().get(0), artifactInfo);
        assertEquals(Commons.toJson(downloadDescriptor), "{\n" +
                                                         "  \"artifacts\" : [ {\n" +
                                                         "    \"artifact\" : \"codenvy\",\n" +
                                                         "    \"version\" : \"1.0.1\",\n" +
                                                         "    \"file\" : \"target/download/file\",\n" +
                                                         "    \"status\" : \"DOWNLOADED\"\n" +
                                                         "  } ],\n" +
                                                         "  \"status\" : \"OK\"\n" +
                                                         "}");
    }

    @Test
    public void testResponseCode() throws Exception {
        DownloadArtifactDescriptor artifactInfo = new DownloadArtifactDescriptor(ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                 Version.valueOf("1.0.1"),
                                                                                 Paths.get(DOWNLOAD_DIR).resolve("file"),
                                                                                 DownloadArtifactStatus.DOWNLOADING);

        DownloadProgressDescriptor
                downloadProgressDescriptor = new DownloadProgressDescriptor(DownloadArtifactStatus.DOWNLOADED, 100, ImmutableList.of(artifactInfo));
        DownloadDescriptor downloadDescriptor = new DownloadDescriptor(downloadProgressDescriptor);

        assertEquals(downloadDescriptor.getStatus(), ResponseCode.OK);

        downloadProgressDescriptor = new DownloadProgressDescriptor(DownloadArtifactStatus.FAILED, 100, ImmutableList.of(artifactInfo));
        downloadDescriptor = new DownloadDescriptor(downloadProgressDescriptor);

        assertEquals(downloadDescriptor.getStatus(), ResponseCode.ERROR);
    }
}
