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


import com.codenvy.api.core.rest.annotations.GenerateLink;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.utils.AccountUtils.SubscriptionInfo;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.mongodb.MongoException;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.artifacts.ArtifactProperties.PUBLIC_PROPERTIES;
import static com.codenvy.im.utils.AccountUtils.ON_PREMISES;
import static com.codenvy.im.utils.AccountUtils.SUBSCRIPTION_DATE_FORMAT;
import static com.codenvy.im.utils.AccountUtils.checkIfUserIsOwnerOfAccount;
import static com.codenvy.im.utils.AccountUtils.hasValidSubscription;
import static com.codenvy.im.utils.Commons.asMap;
import static com.codenvy.im.utils.Commons.combinePaths;
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
    public static final String CAN_NOT_ADD_TRIAL_SUBSCRIPTION =
            "You do not have a valid subscription to install Codenvy. You previously had a 30 day trial subscription, but it has " +
            "also expired. Please contact sales@codenvy.com to extend your trial or to make a purchase.";

    private final String apiEndpoint;
    private final ArtifactStorage artifactStorage;
    private final MongoStorage    mongoStorage;
    private final HttpTransport   transport;
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
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact '%s':'%s'. %s",
                                          artifact, version, e.getMessage())).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact '%s':'%s'. %s",
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
    @RolesAllowed({"system/admin", "system/manager"})
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
        if (entity.equalsIgnoreCase("install-codenvy")) {
            return true;
        }

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
            String userId = userManager.getCurrentUser().getId();

            if (!checkIfUserIsOwnerOfAccount(transport,
                                             apiEndpoint,
                                             accessToken,
                                             accountId)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity(format("Unexpected error. Can't download artifact. User '%s' is not owner of the account '%s'.",
                                              userId, accountId)).build();
            }


            if (requiredSubscription != null && !hasValidSubscription(transport,
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
                    "Unexpected error. Can't download the artifact " + artifact + ":" + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact " + artifact + ":" + version + ". " + e.getMessage()).build();
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
                    "Unexpected error. Can't download the artifact " + artifact + ":" + version + ". " + e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the artifact " + artifact + ":" + version + ". " + e.getMessage()).build();
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
                } catch (ClientAbortException e) {
                    // do nothing
                } catch (Exception e) {
                    if (userId != null) {
                        try {
                            mongoStorage.updateDownloadStatistics(userId, artifact, version, false);
                        } catch (MongoException ex) {
                            String errMsg =
                                    format("Can't update download statistics artifact '%s':'%s' for user '%s'", artifact, version, userId);
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

    /** Adds trial subscription to user if it hadn't one. */
    @GenerateLink(rel = "add trial subscription")
    @POST
    @Path("/subscription/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin"})
    public Response addTrialSubscription(@PathParam("accountId") String accountId) {
        final String userId = userManager.getCurrentUser().getId();
        final String accessToken = userManager.getCurrentUser().getToken();

        try {
            if (!checkIfUserIsOwnerOfAccount(transport,
                                             apiEndpoint,
                                             accessToken,
                                             accountId)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity(format("Unexpected error. Can't add trial subscription. User '%s' is not owner of the account '%s'.",
                                              userId, accountId)).build();
            }

            if (mongoStorage.hasSubscription(userId, ON_PREMISES)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            if (hasValidSubscription(transport, apiEndpoint, ON_PREMISES, accessToken, accountId)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }


            SubscriptionInfo subscriptionInfo = doAddTrialSubscription(accountId, accessToken);
            mongoStorage.addSubscriptionInfo(userId, subscriptionInfo);

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage()).build();
        }
    }

    protected SubscriptionInfo doAddTrialSubscription(String accountId, String accessToken) throws IOException, JsonParseException {
        try {
            final DateFormat df = new SimpleDateFormat(SUBSCRIPTION_DATE_FORMAT);
            final int trialDuration = 30;
            final Calendar startDate = Calendar.getInstance();
            final Calendar endDate = Calendar.getInstance();
            endDate.add(Calendar.DAY_OF_MONTH, trialDuration);

            Map<String, Object> billing = new HashMap<>();
            billing.put("usePaymentSystem", "true");
            billing.put("contractTerm", "12");
            billing.put("startDate", df.format(startDate.getTime()));
            billing.put("endDate", df.format(endDate.getTime()));
            billing.put("cycle", "1");
            billing.put("cycleType", "3");
            billing.put("paymentToken", "trial");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("billing", new JsonStringMapImpl<>(billing));
            attributes.put("startDate", df.format(startDate.getTime()));
            attributes.put("endDate", df.format(endDate.getTime()));
            attributes.put("trialDuration", Integer.toString(trialDuration));
            attributes.put("description", ON_PREMISES);

            Map<String, Object> body = new HashMap<>();
            body.put("accountId", accountId);
            body.put("planId", "opm-com-25u-y");
            body.put("subscriptionAttributes", new JsonStringMapImpl<>(attributes));

            Map m = asMap(transport.doPost(combinePaths(apiEndpoint, "/account/subscriptions"), new JsonStringMapImpl<>(body), accessToken));
            if (!m.containsKey("id")) {
                if (m.containsKey("message")) {
                    throw new IOException(CAN_NOT_ADD_TRIAL_SUBSCRIPTION);
                } else {
                    throw new IOException("Malformed response. 'id' key is missed.");
                }
            }
            String subscriptionId = String.valueOf(m.get("id"));
            LOG.info("Trial subscription added. " + body.toString());

            return new SubscriptionInfo(accountId,
                                        subscriptionId,
                                        ON_PREMISES,
                                        startDate,
                                        endDate);
        } catch (IOException | JsonParseException e) {
            throw new IOException("Can't add subscription. " + e.getMessage(), e);
        }
    }
}
