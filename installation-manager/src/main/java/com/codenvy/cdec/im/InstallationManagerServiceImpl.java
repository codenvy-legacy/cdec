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
package com.codenvy.cdec.im;

import static com.codenvy.cdec.utils.InjectorBootstrap.INJECTOR;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.ArtifactNotFoundException;
import com.codenvy.cdec.AuthenticationException;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.response.ArtifactInfo;
import com.codenvy.cdec.response.ArtifactInfoEx;
import com.codenvy.cdec.response.Response;
import com.codenvy.cdec.response.ResponseCode;
import com.codenvy.cdec.response.Status;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerServiceImpl.class);
    
    protected InstallationManager manager;

    public InstallationManagerServiceImpl() {
        manager = INJECTOR.getInstance(InstallationManagerImpl.class);
    }

    protected final static String COMMON_ERROR_MESSAGE = "Failed to execute operation.";
    protected final static String WRONG_TOKEN_ERROR_MESSAGE = "Incorrect login.";
    
    /** {@inheritDoc} */
    @Override
    public String download() throws IOException {
        Map<Artifact, String> updates = manager.getUpdates();

        List<ArtifactInfo> infos = new ArrayList<>();

        for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            try {
                doDownload(artifact, version);
                infos.add(new ArtifactInfoEx(artifact, version, Status.SUCCESS));
            } catch (Exception e) {
                infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
            }
        }

        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName) throws IOException {
        return download(artifactName, null);
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName, @Nullable String version) throws IOException {
        doDownload(artifactName, version);
        ArtifactInfo info = new ArtifactInfoEx(artifactName, version, Status.SUCCESS);
        return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
    }
    
    /** {@inheritDoc} */
    @Override
    public String getUpdates(String token) {
        try {
            Map<Artifact, String> updates = manager.getUpdates(token);
            return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(updates).build().toJson();
        } catch(Exception e) {
            return handleException(e);
        }
    }

    private String handleException(Exception e) {        
        if (e instanceof ArtifactNotFoundException) {
            LOG.info("Artifact not found.", e);
            return new Response.Builder().withStatus(ResponseCode.OK).withMessage(e.getMessage()).build().toJson();

        } else if (e instanceof AuthenticationException) {
            LOG.info("Authentication error.", e);
            return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(WRONG_TOKEN_ERROR_MESSAGE).build().toJson();
            
        } else {
            LOG.error("Installation manager error.", e);
            return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(COMMON_ERROR_MESSAGE).build().toJson();                            
        }
    }

    /** {@inheritDoc} */
    @Override
    public void obtainChallengeRequest() {
        // do nothing
    }

    protected void doDownload(String artifactName, @Nullable String version) throws IOException, IllegalStateException {
        doDownload(ArtifactFactory.createArtifact(artifactName), version);
    }

    protected void doDownload(Artifact artifact, @Nullable String version) throws IOException, IllegalStateException {
        if (version == null) {
            version = manager.getUpdates().get(artifact);
        }

        manager.download(artifact, version);
    }

    /** {@inheritDoc} */
    @Override
    public String install() throws IOException {
        Map<Artifact, String> updates = manager.getUpdates();

        List<ArtifactInfo> infos = new ArrayList();

        for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            try {
                doInstall(artifact, version);
                infos.add(new ArtifactInfoEx(artifact, version, Status.SUCCESS));
            } catch (Exception e) {
                infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
            }
        }

        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String install(String artifactName) throws IOException {
        return install(artifactName, null);
    }

    /** {@inheritDoc} */
    @Override
    public String install(String artifactName, @Nullable String version) throws IOException {
        doInstall(artifactName, version);
        ArtifactInfo info = new ArtifactInfoEx(artifactName, version, Status.SUCCESS);
        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(asList(new ArtifactInfo[]{info})).build().toJson();
    }

    protected void doInstall(String artifactName, @Nullable String version) throws IOException, IllegalStateException {
        doInstall(ArtifactFactory.createArtifact(artifactName), version);
    }

    protected void doInstall(Artifact artifact, @Nullable String version) throws IOException, IllegalStateException {
        if (version == null) {
            version = manager.getUpdates().get(artifact);
        }

        manager.install(artifact, version);
    }
}