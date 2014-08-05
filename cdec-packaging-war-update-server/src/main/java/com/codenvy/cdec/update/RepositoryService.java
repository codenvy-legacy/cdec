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
package com.codenvy.cdec.update;


import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.google.inject.Singleton;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.cdec.utils.Commons.isValidSubscription;
import static com.codenvy.cdec.utils.Version.isValidVersion;


/**
 * Repository API.
 *
 * @author Anatoliy Bazko
 */
@Singleton
@Path("repository")
public class RepositoryService {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

    private final ArtifactHandler artifactHandler;
    private final HttpTransport transport;
    private final String        apiEndpoint;

    @Inject
    public RepositoryService(@Named("api.endpoint") String apiEndpoint,
                             ArtifactHandler artifactHandler,
                             HttpTransport transport) {
        this.artifactHandler = artifactHandler;
        this.transport = transport;
        this.apiEndpoint = apiEndpoint;

        LOG.info("Repository Service has been initialized, repository directory: " + artifactHandler.getRepositoryDir());
    }

    /**
     * Retrieves the last version of specific artifact.
     *
     * @param artifact
     *         the name of the artifact
     * @return Response
     * @throws ApiException
     *         if unexpected error occurred
     */
    @GenerateLink(rel = "artifact version")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("version/{artifact}")
    public Response getVersion(@PathParam("artifact") final String artifact) {
        try {
            Map<String, String> value = new HashMap<String, String>() {{
                put("version", artifactHandler.getLatestVersion(artifact));
                put("artifact", artifact);
            }};

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(value)).build();
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Unexpected error. Can't retrieve the latest version of the '" + artifact + "'. " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't retrieve the latest version of the '" + artifact + "'").build();
        }
    }

    /**
     * Downloads artifact of the specific version.
     *
     * @param artifact
     *         the name of the artifact
     * @param version
     *         the version of the artifact
     * @return Response
     */
    @GenerateLink(rel = "download artifact")
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response download(@PathParam("artifact") final String artifact,
                             @PathParam("version") final String version) {
        try {
            String requiredSubscription = artifactHandler.getRequiredSubscription(artifact, version);
            if (requiredSubscription != null && !isValidSubscription(transport, apiEndpoint, requiredSubscription)) {
                return Response.status(Response.Status.FORBIDDEN).entity("User must have valid On-Premises subscription.").build();
            }

            return doDownloadArtifact(artifact, version, false);
        } catch (ArtifactNotFoundException | PropertiesNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "Unexpected error. Can 't download the artifact ' " + artifact + "' version " + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact '" + artifact + "' version " + version +
                                   ". Probably it doesn't exist in the repository").build();
        }
    }

    /**
     * Downloads public artifact of the specific version.
     *
     * @param artifact
     *         the artifact name
     * @param version
     *         the version of the artifact
     * @return Response
     */
    @GenerateLink(rel = "download artifact")
    @GET
    @Path("public/download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPublicArtifact(@PathParam("artifact") String artifact,
                                           @PathParam("version") String version) {
        try {
            return doDownloadArtifact(artifact, version, true);
        } catch (ArtifactNotFoundException | PropertiesNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "Unexpected error. Can 't download the artifact ' " + artifact + "' version " + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact '" + artifact + "' version " + version +
                                   ". Probably it doesn't exist in the repository").build();
        }
    }

    /**
     * Downloads the latest version of the artifact.
     *
     * @param artifact
     *         the name of the artifact
     * @return Response
     */
    @GenerateLink(rel = "download artifact")
    @GET
    @Path("public/download/{artifact}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPublicArtifactLatestVersion(@PathParam("artifact") String artifact) {
        try {
            String version = artifactHandler.getLatestVersion(artifact);
            return doDownloadArtifact(artifact, version, true);
        } catch (ArtifactNotFoundException | PropertiesNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the latest version of artifact '" + artifact).build();
        }
    }

    private Response doDownloadArtifact(String artifact, String version, boolean publicAccess) throws IOException {
        java.nio.file.Path path = artifactHandler.getArtifact(artifact, version);

        if (!Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Unexpected error. Can't download the artifact '" + artifact +
                                   "' version " + version + ". Probably the repository doesn't contain one.").build();
        }

        if (publicAccess && artifactHandler.isAuthenticationRequired(artifact, version)) {
            return Response.status(Response.Status.FORBIDDEN).entity("Artifact '" + artifact + "' is not in public access").build();
        }

        String fileName = artifactHandler.getFileName(artifact, version);

        LOG.info("User '" + EnvironmentContext.getCurrent().getUser() + "' is downloading " + fileName);
        return Response.ok(path.toFile(), MediaType.APPLICATION_OCTET_STREAM)
                       .header("Content-Length", String.valueOf(Files.size(path)))
                       .header("Content-Disposition", "attachment; filename=" + fileName)
                       .build();
    }

    /**
     * Uploads artifact into the repository. If the same artifact exists then it will be replaced.
     * If {@value com.codenvy.cdec.update.ArtifactHandler#AUTHENTICATION_REQUIRED_PROPERTY} isn't set then artifact will be treated as private,
     * which requires user to be authenticated to download it. If {@value com.codenvy.cdec.update.ArtifactHandler#SUBSCRIPTION_REQUIRED_PROPERTY}
     * is set then user has to have specific valid subscription to download artifact. If artifact is public then subscription won't be taken into
     * account.
     *
     * @param artifact
     *         the name of the artifact
     * @param version
     *         the version of the artifact
     * @return Response
     */
    @GenerateLink(rel = "upload artifact")
    @POST
    @Path("upload/{artifact}/{version}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"system/admin"})
    public Response upload(@PathParam("artifact") String artifact,
                           @PathParam("version") String version,
                           @Context HttpServletRequest request,
                           @Context UriInfo uriInfo) {
        if (ServletFileUpload.isMultipartContent(request)) {
            DiskFileItemFactory diskFactory = new DiskFileItemFactory();
            diskFactory.setRepository(new File(System.getProperty("java.io.tmpdir")));

            ServletFileUpload upload = new ServletFileUpload(diskFactory);
            try {
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        String fileName = FilenameUtils.getName(item.getName());

                        if (!isValidVersion(version)) {
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                           .entity("The version format is invalid '" + version + "'").build();
                        }

                        Properties props = new Properties();
                        for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
                            if (!entry.getKey().equals("token")) {
                                props.put(entry.getKey(), entry.getValue().get(0));
                            }
                        }

                        try (InputStream in = item.getInputStream()) {
                            artifactHandler.upload(in, artifact, version, fileName, props);
                            return Response.status(Response.Status.OK).build();
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                           .entity("Unexpected error occurred during uploading.").build();
                        } finally {
                            item.delete();
                        }
                    }
                }

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Can n't upload files. The list is empty.").build();
            } catch (FileUploadException e) {
                LOG.error(e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error occurred during uploading.").build();
            }
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("The request must contain multipart content").build();
        }
    }
}
