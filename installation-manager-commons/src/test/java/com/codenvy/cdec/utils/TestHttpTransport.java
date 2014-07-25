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
package com.codenvy.cdec.utils;

import com.codenvy.dto.server.JsonStringMapImpl;

import org.apache.commons.io.IOUtils;
import org.everrest.assured.EverrestJetty;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestHttpTransport {

    private TestService   testService;
    private HttpTransport httpTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        testService = new TestService();
        httpTransport = new HttpTransport();
    }

    @Test
    public void testDoGetRequest(ITestContext context) throws Exception {
        Object port = context.getAttribute(EverrestJetty.JETTY_PORT);
        Map value = Commons.fromJson(httpTransport.doGetRequest("http://0.0.0.0:" + port + "/rest/test/get/json"), Map.class);

        assertEquals(value.size(), 1);
        assertEquals(value.get("key"), "value");
    }

    @Test
    public void testDownload(ITestContext context) throws Exception {
        java.nio.file.Path destDir = Paths.get("target", "download");
        java.nio.file.Path destFile = destDir.resolve("tmp");

        Object port = context.getAttribute(EverrestJetty.JETTY_PORT);
        httpTransport.download("http://0.0.0.0:" + port + "/rest/test/download", destDir);

        assertTrue(Files.exists(destFile));
        try (InputStream in = Files.newInputStream(destFile)) {
            assertEquals(IOUtils.toString(in), "content");
        }
    }

    @Path("test")
    public class TestService {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("get/json")
        public Response getJson() {
            Map<String, String> value = new HashMap<String, String>() {{
                put("key", "value");
            }};
            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(value)).build();
        }

        @GET
        @Path("download")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response doDownloadArtifact() throws IOException {
            java.nio.file.Path file = Paths.get("target", "tmp");
            Files.copy(new ByteArrayInputStream("content".getBytes()), file, StandardCopyOption.REPLACE_EXISTING);
            return Response.ok(file.toFile(), MediaType.APPLICATION_OCTET_STREAM)
                           .header("Content-Length", String.valueOf(Files.size(file)))
                           .header("Content-Disposition", "attachment; filename=" + file.getFileName().toString())
                           .build();
        }
    }
}
