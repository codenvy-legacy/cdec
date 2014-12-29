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
package com.codenvy.im.update;


import com.codenvy.api.account.shared.dto.SubscriptionAttributesDescriptor;
import com.codenvy.api.account.shared.dto.SubscriptionDescriptor;
import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.mongodb.MongoException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.artifacts.ArtifactProperties.PUBLIC_PROPERTIES;
import static com.codenvy.im.utils.AccountUtils.ON_PREMISES;
import static com.codenvy.im.utils.AccountUtils.checkIfUserIsOwnerOfAccount;
import static com.codenvy.im.utils.AccountUtils.getSubscriptionAttributes;
import static com.codenvy.im.utils.AccountUtils.getSubscriptionEndDate;
import static com.codenvy.im.utils.AccountUtils.getSubscriptionStartDate;
import static com.codenvy.im.utils.AccountUtils.isValidSubscription;
import static java.lang.String.format;


/**
 * Repository API.
 *
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Path("repository")
public class RepositoryService {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

    private final String apiEndpoint;
    private final String apiUserName;
    private final String apiUserPassword;
    private final ArtifactStorage artifactStorage;
    private final MongoStorage    mongoStorage;
    private final HttpTransport   transport;
    private final UserManager userManager;
    private final Thread subscriptionInvalidator;

    @Inject
    public RepositoryService(@Named("api.endpoint") String apiEndpoint,
                             @Named("api.user_name") String apiUserName,
                             @Named("api.user_password") String apiUserPassword,
                             UserManager userManager,
                             ArtifactStorage artifactStorage,
                             MongoStorage mongoStorage,
                             HttpTransport transport) {
        this.apiUserName = apiUserName;
        this.apiUserPassword = apiUserPassword;
        this.artifactStorage = artifactStorage;
        this.mongoStorage = mongoStorage;
        this.transport = transport;
        this.apiEndpoint = apiEndpoint;
        this.userManager = userManager;

        this.subscriptionInvalidator = new SubscriptionInvalidator("Subscription invalidator");
        this.subscriptionInvalidator.setDaemon(true);


        LOG.info("Repository Service has been initialized, repository directory: " + artifactStorage.getRepositoryDir());
    }

    @PostConstruct
    public void init() {
        this.subscriptionInvalidator.start();
    }

    @PreDestroy
    public void destroy() {
        this.subscriptionInvalidator.interrupt();
    }

    /**
     * Retrieves properties of the latest version of the artifact.
     *
     * @param artifact
     *         the name of the artifact
     * @return Response
     */
    @GenerateLink(rel = "artifact properties")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/properties/{artifact}")
    public Response getArtifactProperties(@PathParam("artifact") final String artifact) {
        try {
            String version = artifactStorage.getLatestVersion(artifact);
            Map<String, String> properties = doGetArtifactProperties(artifact, version);

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(properties)).build();
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
     * Retrieves properties of the specific version of the artifact.
     *
     * @param artifact
     *         the name of the artifact
     * @param version
     *         the version of the artifact
     * @return Response
     */
    @GenerateLink(rel = "artifact properties")
    @GET
    @Path("/properties/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArtifactProperties(@PathParam("artifact") final String artifact,
                                          @PathParam("version") final String version) {
        try {
            Map<String, String> properties = doGetArtifactProperties(artifact, version);

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(properties)).build();
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact '%s', version '%s'. %s",
                                          artifact, version, e.getMessage())).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact '%s', version '%s'. %s",
                                          artifact, version, e.getMessage())).build();
        }
    }

    private Map<String, String> doGetArtifactProperties(final String artifact, String version) throws IOException {
        final Properties properties = artifactStorage.loadProperties(artifact, version);

        Map<String, String> m = new HashMap<>(PUBLIC_PROPERTIES.size());
        for (String prop : PUBLIC_PROPERTIES) {
            if (properties.containsKey(prop)) {
                m.put(prop, properties.getProperty(prop));
            }
        }
        return m;
    }

    /**
     * Gets download statistic.
     *
     * @return Response
     * @see com.codenvy.im.update.MongoStorage#getDownloadsInfoByArtifact(String)
     * @see com.codenvy.im.update.MongoStorage#getDownloadsInfoByUserId(String)
     */
    @GenerateLink(rel = "get download statistic by users for specific artifact")
    @GET
    @Path("/download/statistic/{entity}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"system/admin"})
    public Response getDownloadStatistic(@PathParam("entity") final String entity) {
        try {
            Map<String, Object> response;
            if (isArtifactName(entity)) {
                response = mongoStorage.getDownloadsInfoByArtifact(entity);
            } else {
                response = mongoStorage.getDownloadsInfoByUserId(entity);
            }

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(response)).build();
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (MongoException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. Can't get information").build();
        }
    }

    private boolean isArtifactName(String entity) {
        try {
            createArtifact(entity);
            return true;
        } catch (ArtifactNotFoundException e) {
            return false;
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
    @Path("/download/{artifact}/{version}/{accountId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed({"user", "system/admin"})
    public Response download(@PathParam("artifact") final String artifact,
                             @PathParam("version") final String version,
                             @PathParam("accountId") final String accountId) {
        try {
            String requiredSubscription = artifactStorage.getRequiredSubscription(artifact, version);
            String accessToken = userManager.getCurrentUser().getToken();
            if (requiredSubscription != null && !isValidSubscription(transport,
                                                                     apiEndpoint,
                                                                     requiredSubscription,
                                                                     accessToken,
                                                                     accountId)) {

                return Response.status(Response.Status.FORBIDDEN)
                               .entity("You do not have a valid subscription. You are not permitted to download '" + artifact +
                                       (version != null ? ":" + version : "") + "'.").build();
            }

            return doDownloadArtifact(artifact, version, false);
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "Unexpected error. Can 't download the artifact ' " + artifact + "' version " + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact '" + artifact + "' version " + version +
                                   ". " + e.getMessage()).build();
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
    @Path("/public/download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPublicArtifact(@PathParam("artifact") String artifact,
                                           @PathParam("version") String version) {
        try {
            return doDownloadArtifact(artifact, version, true);
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(
                    "Unexpected error. Can 't download the artifact ' " + artifact + "' version " + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact '" + artifact + "' version " + version +
                                   ". " + e.getMessage()).build();
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
    @Path("/public/download/{artifact}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadPublicArtifactLatestVersion(@PathParam("artifact") String artifact) {
        try {
            String version = artifactStorage.getLatestVersion(artifact);
            return doDownloadArtifact(artifact, version, true);
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the latest version of artifact '" + artifact).build();
        }
    }

    private Response doDownloadArtifact(final String artifact, final String version, boolean publicAccess) throws IOException {
        final java.nio.file.Path path = artifactStorage.getArtifact(artifact, version);

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

        final String userId = userManager.getCurrentUser() != null ? userManager.getCurrentUser().getId() : null;

        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try (InputStream input = new FileInputStream(path.toFile())) {
                    IOUtils.copyLarge(input, output);

                    if (userId != null) {
                        try {
                            mongoStorage.updateDownloadStatistics(userId, artifact, version, true);
                        } catch (MongoException ex) {
                            String errMsg = format("Can't update download statistics artifact '%s':'%s' for user '%s'", artifact, version, userId);
                            LOG.error(errMsg, ex);
                        }
                    }
                } catch (Exception e) {
                    if (userId != null) {
                        try {
                            mongoStorage.updateDownloadStatistics(userId, artifact, version, false);
                        } catch (MongoException ex) {
                            String errMsg = format("Can't update download statistics artifact '%s':'%s' for user '%s'", artifact, version, userId);
                            LOG.error(errMsg, ex);
                        }
                    }

                    LOG.error("Can't send an artifact " + artifact + ":" + version, e);
                    throw new IOException(e.getMessage(), e);
                }
            }
        };

        return Response.ok(stream)
                       .header("Content-Length", String.valueOf(Files.size(path)))
                       .header("Content-Disposition", "attachment; filename=" + fileName)
                       .build();
    }

    /**
     * Uploads artifact into the repository. If the same artifact exists then it will be replaced.
     * If {@value com.codenvy.im.artifacts.ArtifactProperties#AUTHENTICATION_REQUIRED_PROPERTY} isn't set then artifact will be treated as private,
     * which requires user to be authenticated to download it. If {@value com.codenvy.im.artifacts.ArtifactProperties#SUBSCRIPTION_PROPERTY}
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
    @Path("/upload/{artifact}/{version}")
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
                            artifactStorage.upload(in, artifact, v.toString(), fileName, props);
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

    @GenerateLink(rel = "add trial subscription")
    @POST
    @Path("/subscription/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin"})
    public Response addTrialSubscription(@PathParam("accountId") String accountId,
                                         @Context HttpServletRequest request,
                                         @Context UriInfo uriInfo) {
        final String userId = userManager.getCurrentUser().getId();
        final String accessToken = userManager.getCurrentUser().getToken();

        try {
            if (!checkIfUserIsOwnerOfAccount(transport,
                                             apiEndpoint,
                                             accessToken,
                                             accountId)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity(format("Unexpected error. Can't add trial subscription. User '%s' is not owner of the account %s",
                                              userId, accountId)).build();
            }

            if (mongoStorage.hasSubscription(userId, ON_PREMISES)) {
                return Response.status(Response.Status.OK).build();
            }

            SubscriptionDescriptor subscriptionDescriptor = addSubscription(userId, ON_PREMISES);
            SubscriptionAttributesDescriptor attributes = getSubscriptionAttributes(subscriptionDescriptor.getId(),
                                                                                    transport,
                                                                                    apiEndpoint,
                                                                                    accessToken);

            mongoStorage.addSubscriptionInfo(userId,
                                             subscriptionDescriptor.getAccountId(),
                                             subscriptionDescriptor.getServiceId(),
                                             subscriptionDescriptor.getId(),
                                             getSubscriptionStartDate(attributes),
                                             getSubscriptionEndDate(attributes));

            LOG.info(format("%s subscription added for %s", ON_PREMISES, userId));
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. Can't add subscription.").build();
        }
    }

    protected SubscriptionDescriptor addSubscription(String userId, String subscription) {
        return null; // TODO
    }

    private static class SubscriptionInvalidator extends Thread {
        public SubscriptionInvalidator(String name) {
            super(name);
        }

        @Override
        public void run() {
            LOG.info("Subscription invalidator has been started");

            while (!isInterrupted()) {
                // TODO
            }

            LOG.info("Subscription invalidator has been stopped");
        }
    }
}
