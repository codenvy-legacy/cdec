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
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.managers.AdditionalNodesConfigUtil;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.managers.NodeConfig;
import com.codenvy.im.request.Request;
import com.codenvy.im.response.ArtifactInfo;
import com.codenvy.im.response.Response;
import com.codenvy.im.response.ResponseCode;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.codenvy.im.utils.Commons.toJson;

/**
 * @author Dmytro Nochevnov
 *         We deny concurrent access to the services by marking this class as Singleton
 *         so as there are operations which couldn't execute simulteniously: addNode, removeNode, backup, restore
 */
@Singleton
@Path("/")
@RolesAllowed({"system/admin"})
@Api(value = "/im", description = "Installation manager")
public class InstallationManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerService.class.getSimpleName());

    protected final InstallationManagerFacade delegate;
    protected final ConfigManager configManager;

    protected SaasUserCredentials saasUserCredentials;

    @Inject
    public InstallationManagerService(InstallationManagerFacade delegate, ConfigManager configManager) {
        this.delegate = delegate;
        this.configManager = configManager;
    }

    /** Starts downloading */
    @POST
    @Path("download/start")
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
        Response response = delegate.getInstalledVersions();

        if (response.getArtifacts() != null) {
            Iterator<ArtifactInfo> iter = response.getArtifacts().iterator();
            while (iter.hasNext()) {
                ArtifactInfo item = iter.next();
                if (item.getArtifact().equals(InstallManagerArtifact.NAME)) {
                    iter.remove();
                }
            }
        }

        return handleInstallationManagerResponse(response.toJson());
    }

    /** Updates codenvy */
    @POST
    @Path("update/" + CDECArtifact.NAME)
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
        Map<String, String> properties = delegate.getInstallationManagerProperties();
        return javax.ws.rs.core.Response.ok(new JsonStringMapImpl<>(properties)).build();
    }

    /** Gets Codenvy nodes configuration */
    @GET
    @Path("nodes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gets Codenvy nodes configuration", response = Response.class)
    public javax.ws.rs.core.Response getNodesList() {
        try {
            InstallType installType = configManager.detectInstallationType();
            Config config = configManager.loadInstalledCodenvyConfig(installType);

            if (InstallType.SINGLE_SERVER.equals(installType)) {
                // TODO [AB] test
                ImmutableMap<String, String> properties = ImmutableMap.of(Config.HOST_URL, config.getHostUrl());
                return javax.ws.rs.core.Response.ok(toJson(properties)).build();
            }

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
        Response response = delegate.addNode(dns);
        return handleInstallationManagerResponse(response);
    }

    /** Removes Codenvy node in the multi-node environment */
    @DELETE
    @Path("node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Removes Codenvy node in the multi-node environment", response = Response.class)
    public javax.ws.rs.core.Response removeNode(@QueryParam(value = "dns name of removing node") @ApiParam(required = true) String dns) {
        Response response = delegate.removeNode(dns);
        return handleInstallationManagerResponse(response);
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

        Response response = delegate.backup(config);
        return handleInstallationManagerResponse(response);
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

        Response restore = delegate.restore(config);
        return handleInstallationManagerResponse(restore);
    }

    /** Adds trial subscription to account */
    @POST
    @Path("subscription")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Adds trial subscription to account at the SaaS Codenvy",
            response = Response.class)
    public javax.ws.rs.core.Response addTrialSubscription() throws IOException, CloneNotSupportedException {
        if (saasUserCredentials == null) {
            return handleException(new RuntimeException("User not authenticated"));
        }

        SaasUserCredentials saasUserCredentials = this.saasUserCredentials;
        Request request = new Request().setSaasUserCredentials(saasUserCredentials.clone());

        return handleInstallationManagerResponse(delegate.addTrialSaasSubscription(request));
    }

    /**
     * Gets OnPremises subscription for Codenvy SaaS user.
     * User has to be logged into Codenvy SaaS using {@link #loginToCodenvySaaS} method.
     */
    @GET
    @Path("subscription")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 403, message = "SaaS User is not authenticated or authentication token is expired"),
                           @ApiResponse(code = 404, message = "Subscription not found"),
                           @ApiResponse(code = 500, message = "Server error")})
    @ApiOperation(value = "Gets OnPremises subscription for Codenvy SaaS user", response = SubscriptionDescriptor.class)
    public javax.ws.rs.core.Response getOnPremisesSaasSubscription() {
        if (saasUserCredentials == null) {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN).build();
        }

        try {
            Request request = new Request().setSaasUserCredentials(saasUserCredentials.clone());

            SubscriptionDescriptor descriptor = delegate.getSubscription(SaasAccountServiceProxy.ON_PREMISES, request);
            if (descriptor == null) {
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
            }

            // remove useless info, since links point to Codenvy SaaS API
            descriptor.setLinks(null);

            return javax.ws.rs.core.Response.ok(toJson(descriptor)).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Authentication failed"),
            @ApiResponse(code = 500, message = "Unexpected error occurred")})
    @ApiOperation(value = "Login to Codenvy SaaS",
            notes = "After login is successful SaaS user credentials will be cached.")
    public javax.ws.rs.core.Response loginToCodenvySaaS(Credentials credentials) {
        try {
            credentials = new DtoServerImpls.CredentialsImpl(credentials);

            Token token = delegate.loginToCodenvySaaS(credentials);
            SaasUserCredentials saasUserCredentials = new SaasUserCredentials(token.getValue());

            // get SaaS account id where user is owner
            Request request = new Request().setSaasUserCredentials(saasUserCredentials);
            AccountReference accountRef = delegate.getAccountWhereUserIsOwner(null, request);
            if (accountRef == null) {
                throw new ApiException(SaasAccountServiceProxy.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG);
            }
            saasUserCredentials.setAccountId(accountRef.getId());

            // cache SaaS user credentials into the state of service
            this.saasUserCredentials = saasUserCredentials;

            return javax.ws.rs.core.Response.ok().build();
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
    @ApiOperation(value = "Gets properties of the specific artifact and version")
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
    public javax.ws.rs.core.Response loadProperty(@QueryParam(value = "name") final List<String> names) {
        try {
            Map<String, String> properties = delegate.loadProperties(names);
            return javax.ws.rs.core.Response.ok(new JsonStringMapImpl<>(properties)).build();
        } catch (Exception e) {
            return handleException(e);
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
        try {
            delegate.storeProperties(properties);
            return javax.ws.rs.core.Response.ok().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private javax.ws.rs.core.Response handleInstallationManagerResponse(String responseString) {
        try {
            return handleInstallationManagerResponse(Response.fromJson(responseString));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private javax.ws.rs.core.Response handleInstallationManagerResponse(Response response) {
        ResponseCode responseCode = response.getStatus();
        response.setStatus(null);
        if (ResponseCode.OK == responseCode) {
            return javax.ws.rs.core.Response.ok(response.toJson(), MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            return javax.ws.rs.core.Response.serverError().entity(response.toJson()).build();
        }
    }

    private javax.ws.rs.core.Response handleException(Exception e) {
        LOG.error(e.getMessage(), e);

        javax.ws.rs.core.Response.Status status;

        if (e instanceof ArtifactNotFoundException) {
            status = javax.ws.rs.core.Response.Status.NOT_FOUND;
        } else if (e instanceof HttpException) {
            status = javax.ws.rs.core.Response.Status.fromStatusCode(((HttpException)e).getStatus());
        } else if (e instanceof AuthenticationException) {
            status = javax.ws.rs.core.Response.Status.BAD_REQUEST;
        } else {
            status = javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
        }

        JsonStringMapImpl<String> msgBody = new JsonStringMapImpl<>(ImmutableMap.of("message", e.getMessage()));
        return javax.ws.rs.core.Response.status(status)
                                        .entity(msgBody)
                                        .type(MediaType.APPLICATION_JSON_TYPE)
                                        .build();
    }
}
