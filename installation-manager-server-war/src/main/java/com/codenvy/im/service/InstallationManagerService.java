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
package com.codenvy.im.service;

import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dmytro Nochevnov
 */
@Path("/")
@RolesAllowed({"system/admin"})
@Api(value = "/im", description = "Installation manager")
public class InstallationManagerService {

    private static final Logger LOG = Logger.getLogger(InstallationManagerService.class.getSimpleName());

    protected final InstallationManagerFacade delegate;

    @Inject
    public InstallationManagerService(InstallationManagerFacade delegate) {
        this.delegate = delegate;
    }

    /** Starts downloading */
    @POST
    @Path("download/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Starts downloading artifact from Update Server.", response = Response.class)
    public javax.ws.rs.core.Response startDownload(Request request) {
        return handleInstallationManagerResponse(delegate.startDownload(request));
    }

    /** Interrupts downloading */
    @POST
    @Path("download/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Interrupts downloading artifact from Update Server.", response = Response.class)
    public javax.ws.rs.core.Response stopDownload() {
        return handleInstallationManagerResponse(delegate.stopDownload());
    }

    /** Gets already started download status */
    @POST
    @Path("download/status")
    @ApiOperation(value = "Gets already started download status", response = Response.class)
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getDownloadStatus() {
        return handleInstallationManagerResponse(delegate.getDownloadStatus());
    }

    /** Get the list of actual updates from Update Server */
    @POST
    @Path("download/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of actual updates from Update Server", response = Response.class)
    public javax.ws.rs.core.Response getUpdates(Request request) {
        return handleInstallationManagerResponse(delegate.getUpdates(request));
    }

    /** Gets the list of downloaded artifacts" */
    @POST
    @Path("download/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of downloaded artifacts", response = Response.class)
    public javax.ws.rs.core.Response getDownloads(Request request) {
        return handleInstallationManagerResponse(delegate.getDownloads(request));
    }

    /** Gets the list of installed artifacts. */
    @POST
    @Path("install/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of installed artifacts.", response = Response.class)
    public javax.ws.rs.core.Response getInstalledVersions(Request request) throws IOException {
        return handleInstallationManagerResponse(delegate.getInstalledVersions(request));
    }

    /** Installs or updates artifact */
    @POST
    @Path("install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Installs or updates artifact", response = Response.class)
    public javax.ws.rs.core.Response install(Request request) throws IOException {
        return handleInstallationManagerResponse(delegate.install(request));
    }

    /** Get the list of installation steps */
    @POST
    @Path("install/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the list of installation steps", response = Response.class)
    public javax.ws.rs.core.Response getInstallInfo(Request request) throws IOException {
        return handleInstallationManagerResponse(delegate.getInstallInfo(request));
    }

    /** Gets Installation Manager configuration */
    @POST
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets Installation Manager configuration", response = Response.class)
    public javax.ws.rs.core.Response getConfig() {
        return handleInstallationManagerResponse(delegate.getConfig());
    }

    /** Adds Codenvy node in the multi-node environment */
    @POST
    @Path("node/add")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds Codenvy node in the multi-node environment", response = Response.class)
    public javax.ws.rs.core.Response addNode(@QueryParam(value = "dns") @ApiParam(required = true) String dns) {
        return handleInstallationManagerResponse(delegate.addNode(dns));
    }

    /** Removes Codenvy node in the multi-node environment */
    @DELETE
    @Path("node/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Removes Codenvy node in the multi-node environment ", response = Response.class)
    public javax.ws.rs.core.Response removeNode(@QueryParam(value = "DNS") @ApiParam(required = true) String dns) {
        return handleInstallationManagerResponse(delegate.removeNode(dns));
    }

    /** Performs Codenvy backup according to given backup config */
    @POST
    @Path("backup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Performs Codenvy backup according to given backup config", response = Response.class)
    public javax.ws.rs.core.Response backup(@ApiParam(required = true, value = "backup config") BackupConfig config) throws IOException {
        return handleInstallationManagerResponse(delegate.backup(config));
    }

    /** Performs Codenvy restore according to given backup config */
    @POST
    @Path("restore")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Performs Codenvy restore according to given backup config", response = Response.class)
    public javax.ws.rs.core.Response restore(@ApiParam(required = true, value = "backup config") BackupConfig config)
            throws IOException {
        return handleInstallationManagerResponse(delegate.restore(config));
    }

    private javax.ws.rs.core.Response handleInstallationManagerResponse(String responseString) {
        try {
            Response response = Response.fromJson(responseString);
            if (response.getStatus() == ResponseCode.OK) {
                return javax.ws.rs.core.Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
            } else {
                return javax.ws.rs.core.Response.serverError().entity(response).build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return javax.ws.rs.core.Response.serverError().entity(e.toString()).build();
        }
    }
}
