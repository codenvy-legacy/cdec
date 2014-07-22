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
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.rest.InvalidArgumentException;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.cdec.utils.VersionUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * API.
 *
 * @author Anatoliy Bazko
 */
@Singleton
@Path("repository")
public class RepositoryService {

    private static final Logger LOG             = LoggerFactory.getLogger(RepositoryService.class);
    public static final  String INSTALL_MANAGER = "install-manager";

    private final ArtifactHandler artifactHandler;

    @Inject
    public RepositoryService(ArtifactHandler artifactHandler) {
        this.artifactHandler = artifactHandler;
        LOG.info("Update Service has been started, download directory '" + artifactHandler.getDirectory() + "'");
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
    @Path("version/{artifact}")
    public Response getVersion(@PathParam("artifact") String artifact) throws ApiException {
        try {
            String version = artifactHandler.getLastVersion(artifact);
            return Response.status(Response.Status.OK).entity(version).build();
        } catch (ArtifactNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new NotFoundException("Unexpected error. Can't retrieve the last version of the '" + artifact +
                                        "'. Probably the repository doesn't contain anyone.");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ApiException("Unexpected error. Can't retrieve the last version of the '" + artifact + "'");
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
     * @throws ApiException
     *         if unexpected error occurred
     */
    @GenerateLink(rel = "download artifact")
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response download(@PathParam("artifact") final String artifact, @PathParam("version") final String version) throws ApiException {
        try {
            return doDownloadArtifact(artifact, version);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ApiException("Unexpected error. Can't download the artifact '" + artifact + "' of the version '" + version +
                                   "'. Probably it doesn't exist in the repository");
        }
    }

    /**
     * Downloads 'install-manager' artifact of the specific version.
     *
     * @param version
     *         the version of the artifact
     * @return Response
     * @throws ApiException
     *         if unexpected error occurred
     */
    @GenerateLink(rel = "download artifact")
    @GET
    @Path("download/install-manager/{version}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadInstallManager(@PathParam("version") final String version) throws ApiException {
        try {
            return doDownloadArtifact(INSTALL_MANAGER, version);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ApiException("Unexpected error. Can't download the artifact 'install-manager of the version '" + version +
                                   "'. Probably it doesn't exist in the repository");
        }
    }

    private Response doDownloadArtifact(String artifact, String version) throws IOException {
        if (!artifactHandler.exists(artifact, version)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Unexpected error. Can't download the artifact '" + artifact +
                                   "' of version '" + version + "'. Probably the repository doesn't contain one.").build();
        }

        String fileName = artifactHandler.getFileName(artifact, version);
        java.nio.file.Path path = artifactHandler.getArtifact(fileName, artifact, version);

        return Response.ok(path.toFile(), MediaType.APPLICATION_OCTET_STREAM)
                       .header("Content-Disposition", "attachment; filename=" + fileName)
                       .header("Content-Length", String.valueOf(Files.size(path)))
                       .build();
    }

    /**
     * Uploads artifact into the repository.
     *
     * @param artifact
     *         the name of the artifact
     * @param version
     *         the version of the artifact
     * @return Response
     * @throws ApiException
     *         if unexpected error occurred
     */
    @GenerateLink(rel = "upload artifact")
    @POST
    @Path("upload/{artifact}/{version}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed({"system/admin"})
    public Response upload(@PathParam("artifact") String artifact,
                           @PathParam("version") String version,
                           @Context HttpServletRequest request,
                           @Context UriInfo uriInfo) throws ApiException {
        if (ServletFileUpload.isMultipartContent(request)) {
            DiskFileItemFactory diskFactory = new DiskFileItemFactory();
            diskFactory.setRepository(new File(System.getProperty("java.io.tmpdir")));

            ServletFileUpload upload = new ServletFileUpload(diskFactory);
            try {
                List<FileItem> items = upload.parseRequest(request);
                for (FileItem item : items) {
                    if (!item.isFormField()) {
                        String fileName = FilenameUtils.getName(item.getName());

                        if (!VersionUtil.isValidVersion(version)) {
                            throw new ApiException("The version format is invalid '" + version + "'");
                        } else if (!fileName.contains(version)) {
                            throw new ApiException("The file name '" + fileName + "' doesn't contain the version of artifact");
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
                            throw new ApiException("Unexpected error occurred during uploading.");
                        } finally {
                            item.delete();
                        }
                    }
                }

                throw new NotFoundException("Can n't upload files. The list is empty.");
            } catch (FileUploadException e) {
                LOG.error(e.getMessage(), e);
                throw new ApiException("Unexpected error occurred during uploading.");
            }
        } else {
            throw new InvalidArgumentException("The request must contain multipart content");
        }
    }
}
