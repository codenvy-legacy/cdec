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
package com.codenvy.cdec.restlet;

import javax.ws.rs.GET;
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
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_JSON)
    public String download() throws IOException;


    /** @see InstallationManager#download(com.codenvy.cdec.artifacts.Artifact, String) */
    @GET
    @Path("download/{artifact}")
    @Produces(MediaType.APPLICATION_JSON)
    public String download(@PathParam(value = "artifact") final String artifactName) throws IOException;

    /** @see InstallationManager#download(com.codenvy.cdec.artifacts.Artifact, String) */
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public String download(@PathParam(value = "artifact") final String artifactName,
                           @PathParam(value = "version") final String version) throws IOException;
    
    /** @see InstallationManager#getUpdates() */
    @GET
    @Path("check-updates/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getUpdates(@PathParam(value = "token") String token) throws IOException;

    /**
     * Install all latest updates.
     */
    @GET
    @Path("install")
    @Produces(MediaType.TEXT_HTML)
    public String install() throws IOException;

    /** @see InstallationManager#install(com.codenvy.cdec.artifacts.Artifact) */
    @GET
    @Path("install")
    @Produces(MediaType.TEXT_HTML)
    public String install(@PathParam(value="artifact") final String artifactName) throws IOException;

    /**
     * Install artifact .
     */
    /** @see com.codenvy.cdec.restlet.InstallationManager#install(com.codenvy.cdec.artifacts.Artifact, String) */
    @GET
    @Path("install")
    @Produces(MediaType.TEXT_HTML)
    public String install(@PathParam(value="artifact") final String artifactName,
                          @PathParam(value="version") final String version) throws IOException;
}
