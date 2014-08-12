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

import com.codenvy.cdec.artifacts.Artifact;

import java.io.IOException;
import java.rmi.Remote;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Anatoliy Bazko
 */
@Path("im")
public interface InstallationManager extends Remote {

    /**
     * Scans all available artifacts and returns their current versions.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Artifact, String> getInstalledArtifacts() throws IOException;

    /**
     * Scans all available artifacts and returns their last versions from Update Server.
     */
    @POST
    @Path("get-available-2-download-artifacts")
    @Produces(MediaType.APPLICATION_JSON)
    Map<Artifact, String> getAvailable2DownloadArtifacts() throws IOException;

    /**
     * Downloads updates.
     */
    @GET
    @Path("download-updates")
    @Produces(MediaType.TEXT_HTML)
    void downloadUpdates() throws IOException;

    /**
     * @return the list of artifacts with newer versions than currently installed
     */
    @POST
    @Path("get-new-versions")
    @Produces(MediaType.APPLICATION_JSON)
    Map<Artifact, String> getNewVersions();

    /**
     * Checks if new versions are available. The retrieved list can be obtained by invoking {@link #getNewVersions()} method.
     *
     * @throws IOException
     *         if I/O error occurred
     */
    @POST
    @Path("check-new-versions")
    @Produces(MediaType.TEXT_HTML)    
    void checkNewVersions() throws IOException, IllegalArgumentException;
}
