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


import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableList;

import org.eclipse.che.commons.json.JsonParseException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class TestResponse {
    @Test
    public void testToJsonArtifactInfoList() throws Exception {
        ArtifactInfo info1 = new ArtifactInfo(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.1"));
        ArtifactInfo info2 = new ArtifactInfo(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.2"), ArtifactStatus.SUCCESS);
        Response response = new Response().setArtifacts(Arrays.asList(info1, info2)).setStatus(ResponseCode.OK);

        assertEquals(response.toJson(), "{\n" +
                                        "  \"artifacts\" : [ {\n" +
                                        "    \"artifact\" : \"codenvy\",\n" +
                                        "    \"version\" : \"1.0.1\"\n" +
                                        "  }, {\n" +
                                        "    \"artifact\" : \"codenvy\",\n" +
                                        "    \"version\" : \"1.0.2\",\n" +
                                        "    \"status\" : \"SUCCESS\"\n" +
                                        "  } ],\n" +
                                        "  \"status\" : \"OK\"\n" +
                                        "}");
    }

    @Test
    public void testToJsonArtifactsMap() throws Exception {
        Map<Artifact, Version> m = new LinkedHashMap<Artifact, Version>() {{
            put(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.1"));
        }};

        Response response = new Response().addArtifacts(m).setStatus(ResponseCode.OK);

        assertEquals(response.toJson(), "{\n" +
                                        "  \"artifacts\" : [ {\n" +
                                        "    \"artifact\" : \"codenvy\",\n" +
                                        "    \"version\" : \"1.0.1\"\n" +
                                        "  } ],\n" +
                                        "  \"status\" : \"OK\"\n" +
                                        "}");
    }

    @Test
    public void testResponseJsonParsing() throws IOException, JsonParseException {
        final ArtifactInfo info1 = new ArtifactInfo(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.1"));
        final ArtifactInfo info2 = new ArtifactInfo(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.2"), ArtifactStatus.SUCCESS);
        ArtifactInfo info3 =
                new ArtifactInfo(createArtifact(InstallManagerArtifact.NAME), Version.valueOf("1.0.2"), Paths.get("test"), ArtifactStatus.SUCCESS);
        Response response = new Response().setArtifacts(new ArrayList<ArtifactInfo>() {{
            add(info1);
            add(info2);
        }})
                                          .addArtifact(info3)
                                          .setSubscription("subscription")
                                          .setInfos(ImmutableList.of("info1", "info2"))
                                          .setProperties(new LinkedHashMap<String, String>() {{
                                              put("prop1", "value1");
                                              put("prop2", "value2");
                                          }})
                                          .setMessage("message")
                                          .setStatus(ResponseCode.OK);
        String json = response.toJson();
        String expectedJson = "{\n" +
                              "  \"properties\" : {\n" +
                              "    \"prop1\" : \"value1\",\n" +
                              "    \"prop2\" : \"value2\"\n" +
                              "  },\n" +
                              "  \"artifacts\" : [ {\n" +
                              "    \"artifact\" : \"codenvy\",\n" +
                              "    \"version\" : \"1.0.1\"\n" +
                              "  }, {\n" +
                              "    \"artifact\" : \"codenvy\",\n" +
                              "    \"version\" : \"1.0.2\",\n" +
                              "    \"status\" : \"SUCCESS\"\n" +
                              "  }, {\n" +
                              "    \"artifact\" : \"installation-manager-cli\",\n" +
                              "    \"version\" : \"1.0.2\",\n" +
                              "    \"file\" : \"test\",\n" +
                              "    \"status\" : \"SUCCESS\"\n" +
                              "  } ],\n" +
                              "  \"subscription\" : \"subscription\",\n" +
                              "  \"message\" : \"message\",\n" +
                              "  \"status\" : \"OK\",\n" +
                              "  \"infos\" : [ \"info1\", \"info2\" ]\n" +
                              "}";
        assertEquals(json, expectedJson);

        // check real-life scenario of parsing JSON using Commons.fromJson()
        Response restoredResponse = Commons.fromJson(json, Response.class);

        assertNotNull(restoredResponse);
        assertEquals(Commons.toJson(restoredResponse), expectedJson);
    }

    @Test
    public void testIsError() throws JsonParseException {
        String errorResponse = new Response().setStatus(ResponseCode.ERROR).toJson();
        assertTrue(Response.isError(errorResponse));

        String okResponse = new Response().setStatus(ResponseCode.OK).toJson();
        assertFalse(Response.isError(okResponse));

        assertFalse(Response.isError("{}"));
    }

    @Test(expectedExceptions = JsonParseException.class)
    public void testIsErrorException() throws JsonParseException {
        Response.isError("");
    }
}
