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
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestResponse {
    @Test
    public void testToJsonArtifactInfoList() throws Exception {
        ArtifactInfo info1 = new ArtifactInfo("cdec", "1.0.1");
        ArtifactInfo info2 = new ArtifactInfo("cdec", "1.0.2", Status.SUCCESS);
        Response response = new Response.Builder().withArtifacts(Arrays.asList(info1, info2)).withStatus(ResponseCode.OK).build();

        assertEquals(response.toJson(), "{\n" +
                                        "  \"artifacts\" : [ {\n" +
                                        "    \"artifact\" : \"cdec\",\n" +
                                        "    \"version\" : \"1.0.1\"\n" +
                                        "  }, {\n" +
                                        "    \"artifact\" : \"cdec\",\n" +
                                        "    \"version\" : \"1.0.2\",\n" +
                                        "    \"status\" : \"SUCCESS\"\n" +
                                        "  } ],\n" +
                                        "  \"status\" : \"OK\"\n" +
                                        "}");
    }

    @Test
    public void testToJsonArtifactsMap() throws Exception {
        Map<Artifact, String> m = new LinkedHashMap<Artifact, String>() {{
            put(ArtifactFactory.createArtifact(CDECArtifact.NAME), "1.0.1");
        }};

        Response response = new Response.Builder().withArtifacts(m).withStatus(ResponseCode.OK).build();

        assertEquals(response.toJson(), "{\n" +
                                        "  \"artifacts\" : [ {\n" +
                                        "    \"artifact\" : \"cdec\",\n" +
                                        "    \"version\" : \"1.0.1\"\n" +
                                        "  } ],\n" +
                                        "  \"status\" : \"OK\"\n" +
                                        "}");
    }
}
