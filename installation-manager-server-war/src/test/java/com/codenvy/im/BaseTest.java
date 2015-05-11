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
package com.codenvy.im;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

import org.everrest.assured.JettyHttpServer;
import org.testng.annotations.BeforeTest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.codenvy.im.utils.Commons.toJson;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Anatoliy Bazko
 */
public class BaseTest {

    private RepositoryService testedRepositoryService;

    @BeforeTest
    public void startHttpServer() throws Exception {
        initMocks(this);

        testedRepositoryService = new RepositoryService();

        JettyHttpServer jettyHttpServer = new JettyHttpServer(5555);
        jettyHttpServer.start();
        jettyHttpServer.addSingleton(testedRepositoryService);
    }

    @Path("update/repository")
    public class RepositoryService {

        @GET
        @Path("/properties/{artifact}/{version}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getArtifactProperties(@PathParam("artifact") final String artifact,
                                              @PathParam("version") final String version) throws JsonProcessingException {
            return Response.ok(toJson(ImmutableMap.of(artifact, version))).build();
        }
    }
}
