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


import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.cdec.utils.HttpTransport;
import com.codenvy.cdec.utils.Version;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.google.inject.Singleton;
import com.mongodb.MongoException;

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

import static com.codenvy.cdec.update.ArtifactStorage.PUBLIC_PROPERTIES;
import static com.codenvy.cdec.utils.Commons.isValidSubscription;


/**
 * Repository API.
 *
 * @author Anatoliy Bazko
 */
@Singleton
@Path("repository")
public class RepositoryService {

    public static final String VALID_USER_AGENT = "Installation Manager";

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

    private final ArtifactStorage artifactStorage;
    private final MongoStorage    mongoStorage;
    private final HttpTransport   transport;
    private final String          apiEndpoint;
    private final UserManager userManager;


    @Inject
    public RepositoryService(@Named("api.endpoint") String apiEndpoint,
                             UserManager userManager,
                             ArtifactStorage artifactStorage,
                             MongoStorage mongoStorage,
                             HttpTransport transport) {
        this.artifactStorage = artifactStorage;
        this.mongoStorage = mongoStorage;
        this.transport = transport;
        this.apiEndpoint = apiEndpoint;
        this.userManager = userManager;

        LOG.info("Repository Service has been initialized, repository directory: " + artifactStorage.getRepositoryDir());
    }

    /**
     * Retrieves the last version of specific artifact.
     *
     * @param artifact
     *         the name of the artifact
     * @return Response
     */
    @GenerateLink(rel = "artifact version")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("version/{artifact}")
    public Response getLatestVersion(@PathParam("artifact") final String artifact) {
        try {
            String version = artifactStorage.getLatestVersion(artifact);
            final Properties properties = artifactStorage.loadProperties(artifact, version);

            Map<String, String> m = new HashMap<>(PUBLIC_PROPERTIES.size());
            for (String prop : PUBLIC_PROPERTIES) {
                if (properties.containsKey(prop)) {
                    m.put(prop, properties.getProperty(prop));
                }
            }

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(m)).build();
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
     * Saves info: user, installed artifact and its version. Only trusted agent allowed.
     *
     * @param artifact
     *         the name of the artifact
     * @param version
     *         the version of the artifact
     * @return Response
     * @see com.codenvy.cdec.update.MongoStorage#saveInstalledInfo(String, String, String)
     */
    @GenerateLink(rel = "save installed info")
    @POST
    @Path("info/{artifact}/{version}")
    @RolesAllowed({"user", "system/admin"})
    public Response saveInstalledInfo(@PathParam("artifact") final String artifact,
                                      @PathParam("version") String version,
                                      @HeaderParam("user-agent") String userAgent) {

        if (!VALID_USER_AGENT.equals(userAgent)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else if (!Version.isValidVersion(version)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("The version format is invalid '" + version + "'").build();
        }

        try {
            mongoStorage.saveInstalledInfo(userManager.getCurrentUser().getId(), artifact, version);
            return Response.status(Response.Status.OK).build();
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. Can't save information").build();
        }
    }

    /**
     * Returns info about the latest installed artifact by user.
     *
     * @param artifact
     *         the name of the artifact
     * @return Response
     * @see com.codenvy.cdec.update.MongoStorage#getInstalledInfo(String, String)
     */
    @GenerateLink(rel = "get installed version")
    @GET
    @Path("info/{artifact}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin"})
    public Response getInstalledInfo(@PathParam("artifact") final String artifact) {
        try {
            Map info = mongoStorage.getInstalledInfo(userManager.getCurrentUser().getId(), artifact);
            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<String>(info)).build();
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. Can't get information").build();
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
    @RolesAllowed({"user", "system/admin"})
    public Response download(@PathParam("artifact") final String artifact,
                             @PathParam("version") final String version) {
        try {
            String requiredSubscription = artifactStorage.getRequiredSubscription(artifact, version);
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
            String version = artifactStorage.getLatestVersion(artifact);
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
        java.nio.file.Path path = artifactStorage.getArtifact(artifact, version);

        if (!Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Unexpected error. Can't download the artifact '" + artifact +
                                   "' version " + version + ". Probably the repository doesn't contain one.").build();
        }

        if (publicAccess &&
            (artifactStorage.isAuthenticationRequired(artifact, version)
             || artifactStorage.getRequiredSubscription(artifact, version) != null)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Artifact '" + artifact + "' is not in public access").build();
        }

        String fileName = artifactStorage.getFileName(artifact, version);

        if (!publicAccess) {
            LOG.info("User '" + userManager.getCurrentUser() + "' is downloading " + fileName);
        }
        return Response.ok(path.toFile(), MediaType.APPLICATION_OCTET_STREAM)
                       .header("Content-Length", String.valueOf(Files.size(path)))
                       .header("Content-Disposition", "attachment; filename=" + fileName)
                       .build();
    }

    /**
     * Uploads artifact into the repository. If the same artifact exists then it will be replaced.
     * If {@value ArtifactStorage#AUTHENTICATION_REQUIRED_PROPERTY} isn't set then artifact will be treated as private,
     * which requires user to be authenticated to download it. If {@value ArtifactStorage#SUBSCRIPTION_REQUIRED_PROPERTY}
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

                        Version v;
                        try {
                            v = Version.valueOf(version);
                        } catch (IllegalArgumentException e) {
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                           .entity("The version format is invalid '" + version + "'").build();
                        }

                        Properties props = new Properties();
                        for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
                            if (PUBLIC_PROPERTIES.contains(entry.getKey())) {
                                props.put(entry.getKey(), entry.getValue().get(0));
                            }
                        }

                        try (InputStream in = item.getInputStream()) {
                            artifactStorage.upload(in, artifact, v.getAsString(), fileName, props);
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
