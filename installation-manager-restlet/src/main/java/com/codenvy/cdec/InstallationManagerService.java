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
package com.codenvy.cdec;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * @author Dmytro Nochevnov
 * TODO check paths
 */
@Path("im")
public interface InstallationManagerService extends DigestAuthSupport {

    /**
     * Perform request to get unique and transient information from server to build the authentication credentials for the next requests.
     */
    @HEAD
    @Path("empty")
    @Produces(MediaType.TEXT_HTML)
    public void obtainChallengeRequest();

    /**
     * Downloads available updates. 
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_JSON)
    public String downloadUpdates() throws IOException;

    
    /**
     * Downloads available update of artifact. 
     */
    @GET
    @Path("download/{artifact}")
    @Produces(MediaType.APPLICATION_JSON)
    public String downloadUpdate(@PathParam(value="artifact") final String artifactName) throws IOException;
    
    /**
     * Downloads artifact.
     * If version is null, then download latest version of artifact 
     */
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public String download(@PathParam(value="artifact") final String artifactName, 
                           @PathParam(value="version") final String version) throws IOException;

    
    /**
     * Checks if new versions are available. The retrieved list can be obtained by invoking {@link #getNewVersions()} method.
     */
    @GET
    @Path("check-updates")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkUpdates() throws IOException;
}
