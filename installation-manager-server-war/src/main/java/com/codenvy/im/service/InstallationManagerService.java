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
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.che.AccountUtils;
import com.codenvy.im.utils.che.CodenvyUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.utils.Commons.createDtoFromJson;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 * We deny concurrent access to the services by marking this class as Singleton
 * so as there are operations which couldn't execute simulteniously: addNode, removeNode, backup, restore
 */
@Singleton
@Path("/")
@RolesAllowed({"system/admin"})
@Api(value = "/im", description = "Installation manager")
public class InstallationManagerService {

    private static final Logger LOG = Logger.getLogger(InstallationManagerService.class.getSimpleName());

    protected final InstallationManagerFacade delegate;

    protected ConfigUtil configUtil;

    // map <on-prem user name, saas user credentials>
    protected Map<String, UserCredentials> usersCredentials = new HashMap<>();

    @Inject
    public InstallationManagerService(InstallationManagerFacade delegate,
                                      ConfigUtil configUtil) {
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
        @QueryParam(value = "version") @ApiParam(value = "default version is the latest one at Update Server which is newer than installed one")
        String artifactVersion) {

        Request request = new Request().setArtifactName(artifactName)
                                       .setVersion(artifactVersion);

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
    public javax.ws.rs.core.Response getDownloads(@QueryParam(value = "artifact") @ApiParam(value = "default is all artifacts") String artifactName) {
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
    @Path("subscription")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds trial subscription to account at the SaaS Codenvy",
                  response = Response.class)
    public javax.ws.rs.core.Response addSaasTrialSubscription(
        @Context SecurityContext context) throws IOException {

        String callerName = context.getUserPrincipal().getName();
        if (!usersCredentials.containsKey(callerName)) {
            return handleException(new RuntimeException("User not authenticated"));
        }

        UserCredentials credentials = usersCredentials.get(callerName);
        Request request = new Request().setUserCredentials(credentials.clone());

        return handleInstallationManagerResponse(delegate.addTrialSubscription(request));
    }

    /** Get details of OnPremises subscription */
    @GET
    @Path("subscription")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get description of OnPremises subscription of account of user which has already logged into SaaS Codenvy",
                  response = SubscriptionDescriptor.class)
    public javax.ws.rs.core.Response getSaasSubscription(@Context SecurityContext context) throws IOException {
        String callerName = context.getUserPrincipal().getName();
        if (!usersCredentials.containsKey(callerName)) {
            return handleException(new RuntimeException("User not authenticated"));
        }

        UserCredentials credentials = usersCredentials.get(callerName);
        Request request = new Request().setUserCredentials(credentials.clone());

        try {
            String descriptorJson = delegate.getSubscriptionDescriptor(AccountUtils.ON_PREMISES, request);
            SubscriptionDescriptor descriptor = createDtoFromJson(descriptorJson, SubscriptionDescriptor.class);
            if (descriptor == null) {
                throw new RuntimeException();
            }

            // remove useless info
            descriptor.setLinks(null);

            return javax.ws.rs.core.Response.ok(Commons.toJson(descriptor)).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login to SaaS Codenvy",
                  notes = "After successful login service requests id of account where user is owner " +
                          "and then stores SaaS Codenvy auth token and account id as linked to On-Prem Codenvy user name. " +
                          "They will be used to work around SaaS Codenvy subscription.",
                  response = Response.class)
    public javax.ws.rs.core.Response loginToSaas(Credentials saasUsernameAndPassword,
                                                 @Context SecurityContext context) throws IOException {
        try {
            // get SaaS auth token
            String json = delegate.login(saasUsernameAndPassword);
            Token authToken = createDtoFromJson(json, Token.class);
            if (authToken == null) {
                throw new RuntimeException(CodenvyUtils.CANNOT_LOGIN);
            }
            UserCredentials saasUserCredentials = new UserCredentials(authToken.getValue());

            // get SaaS account id where user is owner
            json = delegate.getAccountReferenceWhereUserIsOwner(null, new Request().setUserCredentials(saasUserCredentials));
            AccountReference accountReference = createDtoFromJson(json, AccountReference.class);
            if (accountReference == null) {
                throw new RuntimeException(AccountUtils.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG);
            }
            saasUserCredentials.setAccountId(accountReference.getId());

            // save SaaS user credentials into the state of service
            usersCredentials.put(context.getUserPrincipal().getName(), saasUserCredentials);

            String useAccountMessage = format(AccountUtils.USE_ACCOUNT_MESSAGE_TEMPLATE, accountReference.getName());
            String response = new Response().setStatus(ResponseCode.OK)
                                            .setMessage(useAccountMessage)
                                            .toJson();
            return javax.ws.rs.core.Response.ok(response).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private javax.ws.rs.core.Response handleInstallationManagerResponse(String responseString) {
        try {
            if (!Response.isError(responseString)) {
                return javax.ws.rs.core.Response.ok(responseString, MediaType.APPLICATION_JSON_TYPE).build();
            } else {
                return javax.ws.rs.core.Response.serverError().entity(responseString).build();
            }
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private javax.ws.rs.core.Response handleException(Exception e) {
        String errorMessage = e.getMessage();
        LOG.log(Level.SEVERE, errorMessage, e);

        if (e instanceof HttpException) {
            // work around error message like "{"message":"Authentication failed. Please check username and password."}"
            try {
                Response response = Response.fromJson(errorMessage);
                errorMessage = response.getMessage();
            } catch (JsonParseException jpe) {
                // ignore so as there is old message in errorMessage variable
            }
        }

        Response response = new Response().setMessage(errorMessage)
                                          .setStatus(ResponseCode.ERROR);
        return javax.ws.rs.core.Response.serverError().entity(response.toJson()).build();
    }

}
