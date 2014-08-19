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

import java.io.IOException;

import org.json.JSONException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;

import com.codenvy.cdec.im.service.response.Response;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author Dmytro Nochevnov
 * TODO check paths
 */
@Path("im")
public interface InstallationManagerService {

    /**
     * Perform request to get unique and transient information from server to build the authentication credentials for the next requests.
     */
    @HEAD
    @Path("empty")
    @Produces(MediaType.TEXT_HTML)
    public void obtainChallengeRequest();
    
    /**
     * Downloads artifact.
     * If version is null, then download latest version of artifact 
     */
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.TEXT_HTML)
    public String download(@PathParam(value="artifact") final String artifactName, 
                           @PathParam(value="version") final String version) throws IOException;
    
    /**
     * Checks if new versions are available. The retrieved list can be obtained by invoking {@link #getNewVersions()} method.
     */
    @GET
    @Path("check")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkUpdates() throws IOException;
}
