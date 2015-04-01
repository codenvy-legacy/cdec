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
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.request.Request;

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

/**
 * @author Dmytro Nochevnov
 */
@Path("/im")
@RolesAllowed({"system/admin", "system/manager"})
public interface InstallationManagerService {

    /** Starts downloading */
    @POST
    @Path("download/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String startDownload(Request request);

    /** Interrupts downloading */
    @POST
    @Path("download/stop")
    @Produces(MediaType.APPLICATION_JSON)
    String stopDownload();

    /** @return the current status of downloading process */
    @POST
    @Path("download/status")
    @Produces(MediaType.APPLICATION_JSON)
    String getDownloadStatus();

    /** @return update list from the server */
    @POST
    @Path("download/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getUpdates(Request request);

    /** @retrun the list of downloaded artifacts */
    @POST
    @Path("download/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getDownloads(Request request);

    /** @return the list of installed artifacts ant theirs versions */
    @POST
    @Path("install/list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getInstalledVersions(Request request) throws IOException;

    /** Installs artifact */
    @POST
    @Path("install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String install(InstallOptions installOptions, Request request) throws IOException;

    /** @return installation info */
    @POST
    @Path("install/info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getInstallInfo(InstallOptions installOptions, Request request) throws IOException;

    /** @return update server url */
    String getUpdateServerEndpoint();

    /** Adds trial subscription for user being logged in */
    @POST
    @Path("subscription/add-trial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String addTrialSubscription(Request request) throws IOException;

    /** Check user's subscription. */
    @POST
    @Path("subscription/{id}/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String checkSubscription(@PathParam(value = "id") String subscription, Request request) throws IOException;

    /** @return the version of the artifact that can be installed */
    @POST
    @Path("install/{step}/version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getVersionToInstall(Request request, @PathParam(value = "step") int installStep) throws IOException;

    /** @return account reference of first valid account of user based on his/her auth token passed into service within the body of request */
    @Nullable
    @POST
    @Path("acount/{accountName}/owner")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String getAccountReferenceWhereUserIsOwner(@Nullable @PathParam(value = "accountName") String accountName, Request request) throws IOException;

    /** @return the configuration of the Installation Manager */
    @POST
    @Path("config/get")
    @Produces(MediaType.APPLICATION_JSON)
    String getConfig();

    /** Add node to multi-server Codenvy */
    @POST
    @Path("node/add")
    @Produces(MediaType.APPLICATION_JSON)
    String addNode(@QueryParam(value = "dns") String dns);

    /** Remove node from multi-server Codenvy */
    @POST
    @Path("node/remove")
    @Produces(MediaType.APPLICATION_JSON)
    String removeNode(@QueryParam(value = "dns") String dns);

    /** Perform backup according to certain backup config */
    @POST
    @Path("backup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String backup(BackupConfig config) throws IOException;

    /** Perform restore according to certain backup config */
    @POST
    @Path("restore")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    String restore(BackupConfig config) throws IOException;
}
