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
public class DownloadResultTest extends BaseTest {

    @Test
    public void testToJson() throws Exception {
        DownloadArtifactResult downloadArtifactResult = new DownloadArtifactResult();
        downloadArtifactResult.setFile("file");
        downloadArtifactResult.setStatus(DownloadArtifactStatus.DOWNLOADED);
        downloadArtifactResult.setArtifact("codenvy");
        downloadArtifactResult.setVersion("1.0.1");

        DownloadResult actualDescriptor = new DownloadResult();
        actualDescriptor.setArtifacts(ImmutableList.of(downloadArtifactResult));
        actualDescriptor.setMessage("error");
        actualDescriptor.setStatus(ResponseCode.OK);

        String json = Commons.toJson(actualDescriptor);
        assertEquals(json, "{\n" +
                           "  \"artifacts\" : [ {\n" +
                           "    \"artifact\" : \"codenvy\",\n" +
                           "    \"version\" : \"1.0.1\",\n" +
                           "    \"file\" : \"file\",\n" +
                           "    \"status\" : \"DOWNLOADED\"\n" +
                           "  } ],\n" +
                           "  \"message\" : \"error\",\n" +
                           "  \"status\" : \"OK\"\n" +
                           "}");

        assertEquals(Commons.fromJson(json, DownloadResult.class), actualDescriptor);
    }

    @Test
    public void testResponseCode() throws Exception {
        DownloadArtifactResult artifactInfo = new DownloadArtifactResult(ArtifactFactory.createArtifact(CDECArtifact.NAME),
                                                                                 Version.valueOf("1.0.1"),
                                                                                 Paths.get(DOWNLOAD_DIR).resolve("file"),
                                                                                 DownloadArtifactStatus.DOWNLOADING);

        DownloadProgressDescriptor
                downloadProgressDescriptor = new DownloadProgressDescriptor(DownloadArtifactStatus.DOWNLOADED, 100, ImmutableList.of(artifactInfo));
        DownloadResult downloadResult = new DownloadResult(downloadProgressDescriptor);

        assertEquals(downloadResult.getStatus(), ResponseCode.OK);

        downloadProgressDescriptor = new DownloadProgressDescriptor(DownloadArtifactStatus.FAILED, 100, ImmutableList.of(artifactInfo));
        downloadResult = new DownloadResult(downloadProgressDescriptor);

        assertEquals(downloadResult.getStatus(), ResponseCode.ERROR);
    }
}
