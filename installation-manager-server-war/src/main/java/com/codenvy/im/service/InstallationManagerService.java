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
import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
@Api(value = "/im",
     description = "Installation manager")
public class InstallationManagerService {

    private static final Logger LOG = Logger.getLogger(InstallationManagerService.class.getSimpleName());

    protected final InstallationManagerFacade facade;

    @Inject
    public InstallationManagerService(InstallationManagerFacade facade) {
        this.facade = facade;
    }

    /** Starts downloading */
    @POST
    @Path("download/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Starts downloading artifact from Update Server.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response startDownload(Request request) {
        return handleInstallationManagerResponse(facade.startDownload(request));
    }

    /** Interrupts downloading */
    @POST
    @Path("download/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Interrupts downloading artifact from Update Server.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response stopDownload() {
        return handleInstallationManagerResponse(facade.stopDownload());
    }

    /** @return the current status of downloading process */
    @POST
    @Path("download/status")
    @ApiOperation(value = "Get download artifact status.",
                  notes = "Get already started download status.",
                  response = Response.class)
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getDownloadStatus() {
        return handleInstallationManagerResponse(facade.getDownloadStatus());
    }

    /** @return list of updates from update server */
    @POST
    @Path("download/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of actual updates from Update Server.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getUpdates(Request request) {
        return handleInstallationManagerResponse(facade.getUpdates(request));
    }

    /** @return the list of downloaded artifacts */
    @POST
    @Path("download/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of downloaded artifacts, which are presence in the upload directory of Installation Manager Server.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getDownloads(Request request) {
        return handleInstallationManagerResponse(facade.getDownloads(request));
    }

    /** @return the list of installed artifacts and theirs versions */
    @POST
    @Path("install/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of installed artifacts.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getInstalledVersions(Request request) throws IOException {
        return handleInstallationManagerResponse(facade.getInstalledVersions(request));
    }

    /** Installs artifact */
    @POST
    @Path("install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Install artifact.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response install(Request request) throws IOException {
        return handleInstallationManagerResponse(facade.install(request));
    }

    /** @return installation info */
    @POST
    @Path("install/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get list of installation steps.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getInstallInfo(Request request) throws IOException {
        return handleInstallationManagerResponse(facade.getInstallInfo(request));
    }

    /** Adds trial subscription for user being logged in */
    @POST
    @Path("subscription/add-trial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds trial subscription for user being logged into SaaS Codenvy.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response addTrialSubscription(Request request) throws IOException {
        return handleInstallationManagerResponse(facade.addTrialSubscription(request));
    }

    /** Check user's subscription. */
    @POST
    @Path("subscription/{id}/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check user's subscription at the SaaS Codenvy.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response checkSubscription(@PathParam(value = "id") String subscription, Request request) throws IOException {
        return handleInstallationManagerResponse(facade.checkSubscription(subscription, request));
    }

    /** @return the version of the artifact that can be installed */
    @POST
    @Path("install/version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get version of the artifact that can be installed.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getVersionToInstall(Request request) throws IOException {
        return handleInstallationManagerResponse(facade.getVersionToInstall(request));
    }

    /** @return account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @Nullable
    @POST
    @Path("account/{accountName}/owner")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get account reference of first valid account of user based on his/her auth token.",
                  notes = "Get account reference of first valid account of user at the SaaS Codenvy based on his/her auth token passed into service within the body of request.",
                  response = Response.class)
    public javax.ws.rs.core.Response getAccountReferenceWhereUserIsOwner(@Nullable @PathParam(value = "accountName") String accountName, Request request) throws IOException {
        return handleInstallationManagerResponse(facade.getAccountReferenceWhereUserIsOwner(accountName, request));
    }

    /** @return the configuration of the Installation Manager */
    @POST
    @Path("config/get")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get configuration of Installation Manager.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response getConfig() {
        return handleInstallationManagerResponse(facade.getConfig());
    }

    /** Add node to multi-server Codenvy */
    @POST
    @Path("node/add")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add node to multi-server Codenvy.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response addNode(@QueryParam(value = "dns") String dns) {
        return handleInstallationManagerResponse(facade.addNode(dns));
    }

    /** Remove node from multi-server Codenvy */
    @POST
    @Path("node/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove node from multi-server Codenvy",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response removeNode(@QueryParam(value = "dns") String dns) {
        return handleInstallationManagerResponse(facade.removeNode(dns));
    }

    /** Perform backup according to certain backup config */
    @POST
    @Path("backup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Backup on-prem Codenvy.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response backup(BackupConfig config) throws IOException {
        return handleInstallationManagerResponse(facade.backup(config));
    }

    /** Perform restore according to certain backup config */
    @POST
    @Path("restore")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Restore on-prem Codenvy.",
                  notes = "",
                  response = Response.class)
    public javax.ws.rs.core.Response restore(BackupConfig config) throws IOException {
        return handleInstallationManagerResponse(facade.restore(config));
    }

    private javax.ws.rs.core.Response handleInstallationManagerResponse(String facadeResponseString) {
        try {
            Response facadeResponse = Response.fromJson(facadeResponseString);
            if (facadeResponse.getStatus() == ResponseCode.OK) {
                return javax.ws.rs.core.Response.ok(facadeResponse, MediaType.APPLICATION_JSON_TYPE).build();
            } else {
                return javax.ws.rs.core.Response.serverError().entity(facadeResponse).build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return javax.ws.rs.core.Response.serverError().entity(e.toString()).build();
        }
    }
}
