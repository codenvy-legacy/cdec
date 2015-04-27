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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.config.ConfigUtil;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.facade.UserCredentials;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.InstallType;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    protected ConfigUtil configUtil;

    @Inject
    public InstallationManagerService(InstallationManagerFacade delegate, ConfigUtil configUtil) {
        this.delegate = delegate;
        this.configUtil = configUtil;
    }

    /** Starts downloading */
    @POST
    @Path("download/start")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Starts downloading artifact from Update Server", notes = "Download all updates of installed artifacts.", response = Response.class)
    public javax.ws.rs.core.Response startDownload(
        @QueryParam(value = "artifact") @ApiParam(value = "review all artifacts by default") String artifactName,
        @QueryParam(value = "version") @ApiParam(value = "default version is the latest one at Update Server which is newer than installed one") String artifactVersion,
        @ApiParam(value = "Token to access SaaS Codenvy server. It's needed to download artifacts which require authentication at SaaS Codenvy")
        String accessToken) {

        UserCredentials userCredentials = new UserCredentials(accessToken);

        Request request = new Request().setArtifactName(artifactName)
                                       .setVersion(artifactVersion)
                                       .setUserCredentials(userCredentials);

        return handleInstallationManagerResponse(delegate.startDownload(request));
    }

    /** Interrupts downloading */
    @POST
    @Path("download/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Interrupts downloading artifact from Update Server", response = Response.class)
    public javax.ws.rs.core.Response stopDownload() {
        return handleInstallationManagerResponse(delegate.stopDownload());
    }

    /** Gets already started download status */
    @GET
    @Path("download/status")
    @ApiOperation(value = "Gets already started download status", response = Response.class)
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getDownloadStatus() {
        return handleInstallationManagerResponse(delegate.getDownloadStatus());
    }

    /** Get the list of actual updates from Update Server */
    @GET
    @Path("download/check")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of actual updates from Update Server", response = Response.class)
    public javax.ws.rs.core.Response getUpdates() {
        return handleInstallationManagerResponse(delegate.getUpdates());
    }

    /** Gets the list of downloaded artifacts" */
    @GET
    @Path("download/list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of downloaded artifacts", response = Response.class)
    public javax.ws.rs.core.Response getDownloads(@QueryParam(value = "artifact") @ApiParam(value="default is all artifacts") String artifactName) {
        Request request = new Request().setArtifactName(artifactName);
        return handleInstallationManagerResponse(delegate.getDownloads(request));
    }

    /** Gets the list of installed artifacts. */
    @GET
    @Path("install/list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of installed artifacts", response = Response.class)
    public javax.ws.rs.core.Response getInstalledVersions() throws IOException {
        return handleInstallationManagerResponse(delegate.getInstalledVersions());
    }

    /** Updates codenvy */
    @POST
    @Path("update/" + CDECArtifact.NAME)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates " + CDECArtifact.NAME,
                  notes = "Install " + CDECArtifact.NAME + " update which has already been downloaded from Update Server and is ready to install. " +
                          "Use request body to pass install configuration properties",
                  response = Response.class)
    public javax.ws.rs.core.Response updateCodenvy(
        @QueryParam(value = "step") @ApiParam(value = "default step is 0") int step,
        Map<String, String> configProperties) throws IOException {

        if (configProperties == null) {
            configProperties = new HashMap<>();   // init install config properties
        }

        InstallType installType = configUtil.detectInstallationType();
        InstallOptions installOptions = new InstallOptions().setInstallType(installType)
                                                            .setStep(step)
                                                            .setConfigProperties(configProperties);

        Request request = new Request().setArtifactName(CDECArtifact.NAME)
                                       .setInstallOptions(installOptions);

        return handleInstallationManagerResponse(delegate.install(request));
    }

    /** Updates installation-manager CLI client */
    @POST
    @Path("update/" + InstallManagerArtifact.NAME)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Updates " + InstallManagerArtifact.NAME,
                  notes = "Install " + InstallManagerArtifact.NAME + " update which has already been downloaded from Update Server and is ready to install. " +
                          "User should launch Installation Manager CLI client after all to complete update.",
                  response = Response.class)
    public javax.ws.rs.core.Response updateImCliClient(
        @QueryParam(value = "cliClientUserHomeDir")
        @ApiParam(value = "path to home directory of system user who installed Installation Manager CLI client") String cliUserHomeDir) throws IOException {
        
        InstallOptions installOptions = new InstallOptions().setCliUserHomeDir(cliUserHomeDir);

        Request request = new Request().setArtifactName(InstallManagerArtifact.NAME)
                                       .setInstallOptions(installOptions);

        return handleInstallationManagerResponse(delegate.install(request));
    }

    /** Gets the list of installation steps */
    @GET
    @Path("update/" + CDECArtifact.NAME + "/info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of installation steps of " + CDECArtifact.NAME + " artifact", response = Response.class)
    public javax.ws.rs.core.Response getUpdateCodenvyInfo() throws IOException {
        InstallType installType = configUtil.detectInstallationType();
        InstallOptions installOptions = new InstallOptions().setInstallType(installType);

        Request request = new Request().setArtifactName(CDECArtifact.NAME)
                                       .setInstallOptions(installOptions);

        return handleInstallationManagerResponse(delegate.getInstallInfo(request));
    }

    /** Gets Installation Manager configuration */
    @GET
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
    public javax.ws.rs.core.Response addNode(@QueryParam(value = "dns name of adding node") @ApiParam(required = true) String dns) {
        return handleInstallationManagerResponse(delegate.addNode(dns));
    }

    /** Removes Codenvy node in the multi-node environment */
    @DELETE
    @Path("node/remove")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Removes Codenvy node in the multi-node environment", response = Response.class)
    public javax.ws.rs.core.Response removeNode(@QueryParam(value = "dns name of removing node") @ApiParam(required = true) String dns) {
        return handleInstallationManagerResponse(delegate.removeNode(dns));
    }

    /** Performs Codenvy backup according to given backup config */
    @POST
    @Path("backup")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Performs backup of given artifact", response = Response.class)
    public javax.ws.rs.core.Response backup(
        @DefaultValue(CDECArtifact.NAME) @QueryParam(value = "artifact") @ApiParam(value = "default artifact is " + CDECArtifact.NAME) String artifactName,
        @QueryParam(value = "backupDir") @ApiParam(value = "path to backup directory") String backupDirectoryPath) throws IOException {

        BackupConfig config = new BackupConfig().setArtifactName(artifactName)
                                                .setBackupDirectory(backupDirectoryPath);

        return handleInstallationManagerResponse(delegate.backup(config));
    }

    /** Performs Codenvy restore according to given backup config */
    @POST
    @Path("restore")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Performs restore of given artifact from the given backup file", response = Response.class)
    public javax.ws.rs.core.Response restore(
        @DefaultValue(CDECArtifact.NAME) @QueryParam(value = "artifact") @ApiParam(value = "default artifact is " + CDECArtifact.NAME) String artifactName,
        @QueryParam(value = "backupFile") @ApiParam(value = "path to backup file", required = true) String backupFilePath) throws IOException {

        BackupConfig config = new BackupConfig().setArtifactName(artifactName)
                                                .setBackupFile(backupFilePath);

        return handleInstallationManagerResponse(delegate.restore(config));
    }

    /** Adds trial subscription to account */
    @POST
    @Path("subscription/{accountId}/add/trial")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds trial subscription to account at the SaaS Codenvy",
                  response = Response.class)
    public javax.ws.rs.core.Response addTrialSubscription(
        @PathParam(value = "accountId") @ApiParam(required = true, value = "SaaS Codenvy account id") String accountId,
        @ApiParam(required = true, value = "Token to access SaaS Codenvy server") String accessToken) throws IOException {

        UserCredentials userCredentials = new UserCredentials(accountId, accessToken);
        Request request = new Request().setUserCredentials(userCredentials);

        return handleInstallationManagerResponse(delegate.addTrialSubscription(request));
    }

    /** Check user's subscription. */
    @POST
    @Path("subscription/{accountId}/check")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if user has a subscription of certain type in the SaaS Codenvy",
                  response = Response.class)
    public javax.ws.rs.core.Response checkSubscription(
        @QueryParam(value = "subscriptionName") @ApiParam(value = "default subscription name is 'OnPremises'") String subscriptionName,
        @PathParam(value = "accountId") @ApiParam(required = true, value = "SaaS Codenvy account id") String accountId,
        @ApiParam(required = true, value = "Token to access SaaS Codenvy server") String accessToken) throws IOException {

        UserCredentials userCredentials = new UserCredentials(accountId, accessToken);
        Request request = new Request().setUserCredentials(userCredentials);

        return handleInstallationManagerResponse(delegate.checkSubscription(subscriptionName, request));
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
