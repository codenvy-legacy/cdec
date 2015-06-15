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
package com.codenvy.im.update;


import com.codenvy.api.subscription.shared.dto.NewSubscription;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserServiceProxy;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.IllegalVersionException;
import com.codenvy.im.utils.MailUtil;
import com.codenvy.im.utils.Version;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.server.JsonArrayImpl;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.codenvy.im.artifacts.ArtifactProperties.PUBLIC_PROPERTIES;
import static com.codenvy.im.saas.SaasAccountServiceProxy.ON_PREMISES;
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

    private static final Logger  LOG                            = LoggerFactory.getLogger(RepositoryService.class);
    public static final  Pattern VALID_EMAIL_ADDRESS_RFC822     =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final  String  CAN_NOT_ADD_TRIAL_SUBSCRIPTION =
            "You do not have a valid subscription to install Codenvy. You previously had a 30 day trial subscription, but it has " +
            "also expired. Please contact sales@codenvy.com to extend your trial or to make a purchase.";

    private final String                  saasApiEndpoint;
    private final ArtifactStorage         artifactStorage;
    private final HttpTransport           httpTransport;
    private final UserManager             userManager;
    private final MailUtil                mailUtil;
    private final SaasUserServiceProxy    saasUserServiceProxy;
    private final SaasAccountServiceProxy saasAccountServiceProxy;

    @Inject
    public RepositoryService(@Named("saas.api.endpoint") String saasApiEndpoint,
                             UserManager userManager,
                             ArtifactStorage artifactStorage,
                             HttpTransport httpTransport,
                             MailUtil mailUtil,
                             SaasUserServiceProxy saasUserServiceProxy,
                             SaasAccountServiceProxy saasAccountServiceProxy) {
        this.artifactStorage = artifactStorage;
        this.httpTransport = httpTransport;
        this.saasApiEndpoint = saasApiEndpoint;
        this.userManager = userManager;
        this.mailUtil = mailUtil;
        this.saasUserServiceProxy = saasUserServiceProxy;
        this.saasAccountServiceProxy = saasAccountServiceProxy;
    }

    /**
     * Retrieves the list of available updates.
     *
     * @param artifact
     *         artifact name
     * @param fromVersion
     *         to return the list of updates beginning from the given version, excluded
     */
    @GenerateLink(rel = "updates")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/updates/{artifact}")
    public Response getUpdates(@PathParam("artifact") final String artifact, @QueryParam("fromVersion") final String fromVersion) {
        try {
            Collection<Version> versions = artifactStorage.getVersions(artifact, fromVersion);

            List<String> l = FluentIterable.from(versions).transform(new Function<Version, String>() {
                @Override
                public String apply(Version version) {
                    return version.toString();
                }
            }).toList();

            return Response.status(Response.Status.OK).entity(new JsonArrayImpl<>(l)).build();
        } catch (IllegalVersionException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't retrieve the latest version of the '" + artifact + "'").build();
        }
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
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact %s:%s. %s",
                                          artifact, version, e.getMessage())).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(format("Unexpected error. Can't retrieve the info of the artifact %s:%s. %s",
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

            if (!saasAccountServiceProxy.checkIfUserIsOwnerOfAccount(accessToken, accountId)) {
                return Response.status(Response.Status.FORBIDDEN)
                               .entity(format("Unexpected error. Can't download artifact. User '%s' is not owner of the account '%s'.",
                                              userId, accountId)).build();
            }


            if (requiredSubscription != null && !saasAccountServiceProxy.hasValidSubscription(requiredSubscription,
                                                                                              accessToken,
                                                                                              accountId)) {

                return Response.status(Response.Status.FORBIDDEN)
                               .entity("You do not have a valid subscription. You are not permitted to download '" + artifact +
                                       (version != null ? ":" + version : "") + "'.").build();
            }

            return doDownloadArtifact(artifact, version, userId);
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
            return doDownloadArtifact(artifact, version, null);
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
            return doDownloadArtifact(artifact, version, null);
        } catch (ArtifactNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Unexpected error. Can't download the latest version of artifact '" + artifact).build();
        }
    }

    private Response doDownloadArtifact(final String artifact,
                                        final String version,
                                        @Nullable final String userId) throws IOException {
        final java.nio.file.Path path = artifactStorage.getArtifact(artifact, version);
        final boolean publicURL = userId == null;

        if (!Files.exists(path)) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Unexpected error. Can't download the artifact '" + artifact +
                                   "' version " + version + ". Probably the repository doesn't contain one.").build();
        }

        if (publicURL &&
            (artifactStorage.isAuthenticationRequired(artifact, version)
             || artifactStorage.getRequiredSubscription(artifact, version) != null)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Artifact '" + artifact + "' is not in public access").build();
        }

        final String fileName = artifactStorage.getFileName(artifact, version);

        if (!publicURL) {
            LOG.info(format("User '%s' is downloading %s", userId, fileName));
        }

        StreamingOutput stream = new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try (InputStream input = new FileInputStream(path.toFile())) {
                    IOUtils.copyLarge(input, output);

                    LOG.info("EVENT#im-artifact-downloaded# TIME#{}# USER#{}# ARTIFACT#{}# VERSION#{}#",
                             System.currentTimeMillis(),
                             artifact.toLowerCase(),
                             version);

                } catch (ClientAbortException e) {
                    // do nothing
                    LOG.info(format("User %s aborted downloading %s:%s", userId == null ? "Anonymous" : userId, artifact, version));
                } catch (Exception e) {
                    LOG.info(format("User %s failed to download %s:%s", userId == null ? "Anonymous" : userId, artifact, version), e);
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
            if (!saasAccountServiceProxy.checkIfUserIsOwnerOfAccount(accessToken, accountId)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity(format("Unexpected error. Can't add trial subscription. User '%s' is not owner of the account '%s'.",
                                              userId, accountId)).build();
            }

            if (saasAccountServiceProxy.hasValidSubscription(ON_PREMISES, accessToken, accountId)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }

            doAddTrialSubscription(userId, accountId, accessToken);
            sendNotificationLetter(accountId, userManager.getCurrentUser());

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage()).build();
        }
    }

    protected void doAddTrialSubscription(String userId, String accountId, String accessToken) throws IOException, JsonParseException {
        try {
            final String planId = "opm-com-25u-y";

            NewSubscription newSubscription = DtoFactory.getInstance().createDto(NewSubscription.class);
            newSubscription.setAccountId(accountId);
            newSubscription.setPlanId(planId);
            newSubscription.setUsePaymentSystem(true);

            Map m = asMap(httpTransport.doPost(combinePaths(saasApiEndpoint, "subscription"), newSubscription, accessToken));
            if (!m.containsKey("id")) {
                if (m.containsKey("message")) {
                    throw new IOException(CAN_NOT_ADD_TRIAL_SUBSCRIPTION);
                } else {
                    throw new IOException("Malformed response. 'id' key is missed.");
                }
            }
            LOG.info("EVENT#im-subscription-added# TIME#{}# USER#{}# PLAN#{}#",
                     System.currentTimeMillis(),
                     userId,
                     planId);
        } catch (IOException | JsonParseException e) {
            throw new IOException("Can't add subscription. " + e.getMessage(), e);
        }
    }

    protected void sendNotificationLetter(String accountId, User user) {
        try {
            String userEmail = VALID_EMAIL_ADDRESS_RFC822.matcher(user.getName()).matches()
                               ? user.getName()
                               : saasUserServiceProxy.getUserEmail(user.getToken());

            mailUtil.sendNotificationLetter(accountId, userEmail);
            LOG.info(format("Subscription for %s was provisioned and notification mail was sent", userEmail));
        } catch (IOException | MessagingException e) {
            LOG.error("Error of sending email with subscription info to sales.", e);
        }
    }
}
