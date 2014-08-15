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
package com.codenvy.cdec.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.restlet.ext.json.JsonRepresentation;

/**
 * @author Dmytro Nochevnov
 */
@Path("im")
public interface InstallationManagerService extends EmptyService {

    /**
     * Scans all available artifacts and returns their current versions.
     */
    @GET
    @Path("get-available-2-download-artifacts")
    @Produces(MediaType.APPLICATION_JSON)
    public void doGetAvailable2DownloadArtifacts();
    
    /**
     * Downloads update.
     */
    @GET
    @Path("download/{artifact}/{version}")
    @Produces(MediaType.TEXT_HTML)
    public void downloadUpdate(@PathParam(value="artifact") final String artifactName, 
                               @PathParam(value="version") final String version);
    
    /**
     * Checks if new versions are available. The retrieved list can be obtained by invoking {@link #getNewVersions()} method.
     */
    @GET
    @Path("check")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonRepresentation checkUpdates()  throws JSONException;
}
