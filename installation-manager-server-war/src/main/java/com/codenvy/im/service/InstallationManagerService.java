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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.managers.AdditionalNodesConfigUtil;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.JsonStringMapImpl;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.utils.Commons.createDtoFromJson;
import static com.codenvy.im.utils.Commons.toJson;
import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 *         We deny concurrent access to the services by marking this class as Singleton
 *         so as there are operations which couldn't execute simulteniously: addNode, removeNode, backup, restore
 */
@Singleton
@Path("/")
@RolesAllowed({"system/admin"})
@Api(value = "/im", description = "Installation manager")
// TODO [AB] Response.class
public class InstallationManagerService {

    private static final Logger LOG       = Logger.getLogger(InstallationManagerService.class.getSimpleName());
    private static final int    MAX_USERS = 10;

    protected final InstallationManagerFacade delegate;
    private final ConfigManager configManager;

    /**
     * Cached users' credentials.
     */
    protected final Map<String, SaasUserCredentials> users;

    @Inject
    public InstallationManagerService(InstallationManagerFacade delegate,
                                      ConfigManager configManager) {
        this.delegate = delegate;
        this.configManager = configManager;

        this.users = new LinkedHashMap<String, SaasUserCredentials>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_USERS;
            }
        };
    }

    /** Starts downloading */
    @POST
    @Path("download/start")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Starts downloading artifact from Update Server", notes = "Download all updates of installed artifacts.", response =
            Response.class)
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
    @Path("update")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of actual updates from Update Server", response = Response.class)
    public javax.ws.rs.core.Response getUpdates() {
        return handleInstallationManagerResponse(delegate.getUpdates());
    }

    /** Gets the list of downloaded artifacts" */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of downloaded artifacts", response = Response.class)
    public javax.ws.rs.core.Response getDownloads(@QueryParam(value = "artifact") @ApiParam(value = "default is all artifacts") String artifactName) {
        Request request = new Request().setArtifactName(artifactName);
        return handleInstallationManagerResponse(delegate.getDownloads(request));
    }

    /** Gets the list of installed artifacts. */
    @GET
    @Path("installation")
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
    public javax.ws.rs.core.Response updateCodenvy(@QueryParam(value = "step") @ApiParam(value = "default step is 0") int step) throws IOException {
        final InstallOptions installOptions = new InstallOptions();
        installOptions.setInstallType(configManager.detectInstallationType());
        installOptions.setStep(step);

        final Request request = new Request();
        request.setArtifactName(CDECArtifact.NAME);
        request.setInstallOptions(installOptions);
        request.setVersion(delegate.getVersionToInstall(request));

        Map<String, String> properties = configManager.prepareInstallProperties(null,
                                                                                installOptions.getInstallType(),
                                                                                request.createArtifact(),
                                                                                request.createVersion());

        installOptions.setConfigProperties(properties);

        return handleInstallationManagerResponse(delegate.install(request));
    }

    /** Gets the list of installation steps */
    @GET
    @Path("update/" + CDECArtifact.NAME + "/info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets the list of installation steps of " + CDECArtifact.NAME + " artifact", response = Response.class)
    public javax.ws.rs.core.Response getUpdateCodenvyInfo() throws IOException {
        InstallType installType = configManager.detectInstallationType();
        InstallOptions installOptions = new InstallOptions().setInstallType(installType);

        Request request = new Request().setArtifactName(CDECArtifact.NAME)
                                       .setInstallOptions(installOptions);

        return handleInstallationManagerResponse(delegate.getInstallInfo(request));
    }

    /** Gets Installation Manager configuration */
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets Installation Manager Server configuration", response = Response.class)
    public javax.ws.rs.core.Response getInstallationManagerServerConfig() {
        return handleInstallationManagerResponse(delegate.getConfig());
    }

    /** Gets Codenvy on-prem configuration */
    @GET
    @Path("config/codenvy")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets Codenvy on-prem configuration", response = Response.class)
    public javax.ws.rs.core.Response getCodenvyConfig() {
        try {
            InstallType installType = configManager.detectInstallationType();
            if (InstallType.SINGLE_SERVER.equals(installType)) {
                return javax.ws.rs.core.Response.ok(toJson(new HashMap())).build();
            }

            Config config = configManager.loadInstalledCodenvyConfig();
            Map<String, Object> selectedProperties = new HashMap<>();

            // filter node dns
            List<NodeConfig> nodes = NodeConfig.extractConfigsFrom(config);
            for (NodeConfig node : nodes) {
                String nodeHostPropertyName = node.getType().toString().toLowerCase() + Config.NODE_HOST_PROPERTY_SUFFIX;
                selectedProperties.put(nodeHostPropertyName, node.getHost());
            }

            // get additional nodes dns lists
            AdditionalNodesConfigUtil additionalNodesConfigUtil = new AdditionalNodesConfigUtil(config);
            Map<String, List<String>> additionalRunners = additionalNodesConfigUtil.extractAdditionalNodesDns(NodeConfig.NodeType.RUNNER);
            if (additionalRunners != null) {
                selectedProperties.putAll(additionalRunners);
            }

            Map<String, List<String>> additionalBuilders = additionalNodesConfigUtil.extractAdditionalNodesDns(NodeConfig.NodeType.BUILDER);
            if (additionalBuilders != null) {
                selectedProperties.putAll(additionalBuilders);
            }

            return javax.ws.rs.core.Response.ok(toJson(selectedProperties)).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /** Adds Codenvy node in the multi-node environment */
    @POST
    @Path("node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds Codenvy node in the multi-node environment", response = Response.class)
    public javax.ws.rs.core.Response addNode(@QueryParam(value = "dns name of adding node") @ApiParam(required = true) String dns) {
        return handleInstallationManagerResponse(delegate.addNode(dns));
    }

    /** Removes Codenvy node in the multi-node environment */
    @DELETE
    @Path("node")
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
            @DefaultValue(CDECArtifact.NAME) @QueryParam(value = "artifact") @ApiParam(value = "default artifact is " +
                                                                                               CDECArtifact.NAME) String artifactName,
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
            @DefaultValue(CDECArtifact.NAME) @QueryParam(value = "artifact") @ApiParam(value = "default artifact is " +
                                                                                               CDECArtifact.NAME) String artifactName,
            @QueryParam(value = "backupFile") @ApiParam(value = "path to backup file", required = true) String backupFilePath) throws IOException {

        BackupConfig config = new BackupConfig().setArtifactName(artifactName)
                                                .setBackupFile(backupFilePath);

        return handleInstallationManagerResponse(delegate.restore(config));
    }

    /** Adds trial subscription to account */
    @POST
    @Path("subscription")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds trial subscription to account at the SaaS Codenvy",
            response = Response.class)
    public javax.ws.rs.core.Response addTrialSubscription(@Context SecurityContext context) throws IOException, CloneNotSupportedException {
        String callerName = context.getUserPrincipal().getName();
        if (!users.containsKey(callerName)) {
            return handleException(new RuntimeException("User not authenticated"));
        }

        SaasUserCredentials saasUserCredentials = users.get(callerName);
        Request request = new Request().setSaasUserCredentials(saasUserCredentials.clone());

        return handleInstallationManagerResponse(delegate.addTrialSaasSubscription(request));
    }

    /** Get details of OnPremises subscription */
    @GET
    @Path("subscription")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get description of OnPremises subscription of account of user which has already logged into SaaS Codenvy",
            response = SubscriptionDescriptor.class)
    public javax.ws.rs.core.Response getSaasSubscription(@Context SecurityContext context) throws IOException, CloneNotSupportedException {
        String callerName = context.getUserPrincipal().getName();
        if (!users.containsKey(callerName)) {
            return handleException(new RuntimeException("User not authenticated"));
        }

        SaasUserCredentials saasUserCredentials = users.get(callerName);
        Request request = new Request().setSaasUserCredentials(saasUserCredentials.clone());

        try {
            SubscriptionDescriptor descriptor = delegate.getSubscriptionDescriptor(SaasAccountServiceProxy.ON_PREMISES, request);
            if (descriptor == null) {
                throw new RuntimeException(SaasAccountServiceProxy.CANNOT_OBTAIN_SUBCRIPTION);
            }

            // remove useless info
            descriptor.setLinks(null);

            return javax.ws.rs.core.Response.ok(toJson(descriptor)).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Login to Codenvy SaaS",
            notes = "After login is successful SaaS user credentials will be cached.",
            response = Response.class)
    public javax.ws.rs.core.Response loginToCodenvySaas(Credentials credentials, @Context SecurityContext context) throws IOException {
        try {
            String json = delegate.loginToCodenvySaaS(credentials);
            if (json == null) {
                return javax.ws.rs.core.Response.serverError().entity(SaasAuthServiceProxy.CANNOT_LOGIN).build();
            }
            Token authToken = createDtoFromJson(json, Token.class);
            SaasUserCredentials saasUserCredentials = new SaasUserCredentials(authToken.getValue());

            // get SaaS account id where user is owner
            json = delegate.getAccountReferenceWhereUserIsOwner(null, new Request().setSaasUserCredentials(saasUserCredentials));
            if (json == null) {
                return javax.ws.rs.core.Response.serverError().entity(SaasAccountServiceProxy.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG).build();
            }
            AccountReference accountReference = createDtoFromJson(json, AccountReference.class);
            saasUserCredentials.setAccountId(accountReference.getId());

            // cache SaaS user credentials into the state of service
            users.put(context.getUserPrincipal().getName(), saasUserCredentials);

            String useAccountMessage = format(SaasAccountServiceProxy.USE_ACCOUNT_MESSAGE_TEMPLATE, accountReference.getName());
            String response = new Response().setStatus(ResponseCode.OK)
                                            .setMessage(useAccountMessage)
                                            .toJson();
            return javax.ws.rs.core.Response.ok(response).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /** @return the properties of the specific artifact and version */
    @GET
    @Path("/properties/{artifact}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Artifact not Found"),
            @ApiResponse(code = 500, message = "Unexpected error occurred")})
    @ApiOperation(value = "Gets properties of the specific artifact and version", response = Response.class)
    public javax.ws.rs.core.Response getArtifactProperties(@PathParam("artifact") final String artifactName,
                                                           @PathParam("version") final String versionNumber) {
        try {
            Artifact artifact = createArtifact(artifactName);
            Version version;
            try {
                version = Version.valueOf(versionNumber);
            } catch (IllegalArgumentException e) {
                throw new ArtifactNotFoundException(artifactName, versionNumber);
            }

            Map<String, String> properties = artifact.getProperties(version);
            return javax.ws.rs.core.Response.ok(new JsonStringMapImpl<>(properties)).build();
        } catch (IOException e) {
            return handleException(e);
        }
    }

    /** Reads properties from the storage */
    @GET
    @Path("/property")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Unexpected error occurred")})
    @ApiOperation(value = "Reads properties from the storage", response = Response.class)
    public javax.ws.rs.core.Response readProperty(@QueryParam(value = "name") final List<String> names) {
        Response response = delegate.readProperties(names);

        if (ResponseCode.ERROR == response.getStatus()) {
            String errorMessage = response.getMessage();
            return javax.ws.rs.core.Response.serverError().entity(errorMessage).build();
        } else {
            JsonStringMapImpl<String> properties = new JsonStringMapImpl<>(response.getConfig());
            return javax.ws.rs.core.Response.ok().entity(properties).build();
        }
    }

    /** Stores properties into the storage */
    @POST
    @Path("/property")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Unexpected error occurred")})
    @ApiOperation(value = "Stores properties into the storage", response = Response.class)
    public javax.ws.rs.core.Response storeProperty(Map<String, String> properties) {
        Response response = delegate.storeProperties(properties);

        if (ResponseCode.ERROR == response.getStatus()) {
            String errorMessage = response.getMessage();
            return javax.ws.rs.core.Response.serverError().entity(errorMessage).build();
        } else {
            return javax.ws.rs.core.Response.ok().build();
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

        if (e instanceof ArtifactNotFoundException) {
            return javax.ws.rs.core.Response.serverError()
                                            .status(javax.ws.rs.core.Response.Status.NOT_FOUND)
                                            .entity(e.getMessage())
                                            .build();

        } else if (e instanceof HttpException) {
            // work around error message like "{"message":"Authentication failed. Please check username and password."}"
            try {
                Response response = Response.fromJson(errorMessage);
                errorMessage = response.getMessage();
            } catch (JsonParseException jpe) {
                // ignore so as there is old message in errorMessage variable
            }
        }

        Response response = new Response().setMessage(errorMessage).setStatus(ResponseCode.ERROR);
        return javax.ws.rs.core.Response.serverError().entity(response.toJson()).build();
    }

}
