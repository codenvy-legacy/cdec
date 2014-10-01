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

    /** Download all latest updates */
    @POST
    @Path("download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String download(JacksonRepresentation<UserCredentials> userCredentialsRep);

    @POST
    @Path("download/{artifact}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String download(@PathParam(value = "artifact") final String artifactName,
                           JacksonRepresentation<UserCredentials> userCredentialsRep);

    @POST
    @Path("download/{artifact}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String download(@PathParam(value = "artifact") final String artifactName,
                           @PathParam(value = "version") final String version,
                           JacksonRepresentation<UserCredentials> requestRepresentation);

    @POST
    @Path("check-updates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getUpdates(JacksonRepresentation<UserCredentials> requestRepresentation);

    @GET
    @Path("download-list/{artifact}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(@PathParam(value = "artifact") final String artifactName);

    @GET
    @Path("download-list")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads();

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
    public String install(@PathParam(value = "artifact") final String artifactName,
                          JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    @POST
    @Path("install/{artifact}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String install(@PathParam(value = "artifact") final String artifactName,
                          @PathParam(value = "version") final String version,
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
    public String checkSubscription(@PathParam(value = "subscription") final String subscription,
                                    JacksonRepresentation<UserCredentials> userCredentialsRep) throws IOException;

    /** Returns id of first valid account of user based on his/her auth token passed into service within the body of request */
    @POST
    @Path("account/id")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String getAccountId(JacksonRepresentation<UserCredentials> userCredentialsRep);
}
