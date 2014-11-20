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

import com.codenvy.im.request.Request;
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
public interface InstallationManagerService {

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

    @GET
    @Path("download-stop/{download-descriptor-id}")
    @Produces(MediaType.APPLICATION_JSON)
    String stopDownload(@PathParam(value = "download-descriptor-id") String downloadDescriptorId);

    @POST
    @Path("check-updates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getUpdates(JacksonRepresentation<UserCredentials> requestRepresentation);

    /** Get list of downloaded artifacts */
    @POST
    @Path("download-list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(JacksonRepresentation<Request> requestRep);

    /** Get versions of installed artifacts. */
    @POST
    @Path("version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getVersions(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    @POST
    @Path("install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String install(JacksonRepresentation<Request> requestRep) throws IOException;

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

    /** Returns account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @POST
    @Path("account/{accountName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getAccountReferenceWhereUserIsOwner(@PathParam(value = "accountName") String accountName,
                                                      JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Returns account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @POST
    @Path("account")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getAccountReferenceWhereUserIsOwner(JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;


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
