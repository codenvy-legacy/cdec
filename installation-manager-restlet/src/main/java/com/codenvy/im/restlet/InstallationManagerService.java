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
package com.codenvy.im.restlet;

import com.codenvy.im.user.UserCredentials;

import org.restlet.ext.jackson.JacksonRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * @author Dmytro Nochevnov
 */
@Path("im")
public interface InstallationManagerService extends DigestAuthSupport {

    /** Starts downloading all latest updates */
    @POST
    @Path("start-download/{download-descriptor-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String startDownload(@PathParam(value = "download-descriptor-id") String downloadDescriptorId,
                                JacksonRepresentation<UserCredentials> userCredentialsRep);

    /** Starts downloading the specific artifact */
    @POST
    @Path("start-download/{artifact}/{download-descriptor-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String startDownload(@PathParam(value = "artifact") String artifactName,
                                @PathParam(value = "download-descriptor-id") String downloadDescriptorId,
                                JacksonRepresentation<UserCredentials> userCredentialsRep);

    /** Starts downloading the specific version of the artifact */
    @POST
    @Path("start-download/{artifact}/{version}/{download-descriptor-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String startDownload(@PathParam(value = "artifact") String artifactName,
                                @PathParam(value = "version") String version,
                                @PathParam(value = "download-descriptor-id") String downloadDescriptorId,
                                JacksonRepresentation<UserCredentials> requestRepresentation);

    @GET
    @Path("download-status/{download-descriptor-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String downloadStatus(@PathParam(value = "download-descriptor-id") String downloadDescriptorId);

    @POST
    @Path("check-updates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getUpdates(JacksonRepresentation<UserCredentials> requestRepresentation);

    @POST
    @Path("download-list/{artifact}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(@PathParam(value = "artifact") String artifactName,
                               JacksonRepresentation<UserCredentials> userCredentialsRep);

    @POST
    @Path("download-list/{artifact}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(@PathParam(value = "artifact") String artifactName,
                               @PathParam(value = "version") String version, JacksonRepresentation<UserCredentials> userCredentialsRep);

    @POST
    @Path("download-list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(JacksonRepresentation<UserCredentials> userCredentialsRep);

    /** Install all latest updates. */
    @POST
    @Path("install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String install(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Get versions of installed artifacts. */
    @POST
    @Path("version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getVersions(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;


    @POST
    @Path("install/{artifact}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String install(@PathParam(value = "artifact") String artifactName,
                          JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    @POST
    @Path("install/{artifact}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String install(@PathParam(value = "artifact") String artifactName,
                          @PathParam(value = "version") String version,
                          JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Get the url of the update server. */
    @GET
    @Path("update-server-url")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUpdateServerEndpoint();

    /** Check user's subscription. */
    @POST
    @Path("check-subscription/{subscription}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String checkSubscription(@PathParam(value = "subscription") String subscription,
                                    JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Returns id of first valid account of user based on his/her auth token passed into service within the body of request */
    @POST
    @Path("account")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getAccountIdWhereUserIsOwner(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;


    /** @see com.codenvy.im.utils.AccountUtils#isValidAccountId(com.codenvy.im.utils.HttpTransport, String, com.codenvy.im.user.UserCredentials) */
    @POST
    @Path("validate-account-id")
    @Consumes(MediaType.APPLICATION_JSON)
    public Boolean isValidAccountId(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Returns the configuration of the Installation Manager */
    @GET
    @Path("get-config")
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfig();

    /** Sets new configuration for installation manager */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String setConfig(JacksonRepresentation<InstallationManagerConfig> configRep);
}
