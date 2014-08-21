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

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.response.*;
import com.codenvy.cdec.restlet.InstallationManager;
import com.codenvy.cdec.restlet.InstallationManagerService;

import org.restlet.resource.ServerResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.codenvy.cdec.utils.InjectorBootstrap.INJECTOR;

/**
 * @author Dmytro Nochevnov
 * @author Anatoliy Bazko
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    protected InstallationManager manager;

    public InstallationManagerServiceImpl() {
        manager = INJECTOR.getInstance(InstallationManagerImpl.class);
    }

    /** {@inheritDoc} */
    @Override
    public String download() {
        Map<Artifact, String> updates;
        try {
            updates = manager.getUpdates();
        } catch (IOException e) {
            return Response.valueOf(e).toJson();
        }

        List<ArtifactInfo> infos = new ArrayList<>();

        for (Map.Entry<Artifact, String> entry : updates.entrySet()) {
            Artifact artifact = entry.getKey();
            String version = entry.getValue();

            try {
                doDownload(artifact, version);
                infos.add(new ArtifactInfoEx(artifact, version, Status.SUCCESS));
            } catch (IOException | IllegalStateException e) {
                infos.add(new ArtifactInfoEx(artifact, version, Status.FAILURE));
                return new Response.Builder().withStatus(ResponseCode.ERROR).withMessage(e.getMessage()).withArtifacts(infos).build().toJson();
            }
        }

        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(infos).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName) {
        return download(artifactName, null);
    }

    /** {@inheritDoc} */
    @Override
    public String download(String artifactName, @Nullable String version) {
        try {
            doDownload(artifactName, version);
        } catch (IOException | IllegalStateException e) {
            return Response.valueOf(e).toJson();
        }

        ArtifactInfo info = new ArtifactInfoEx(artifactName, version, Status.SUCCESS);
        return new Response.Builder().withStatus(ResponseCode.OK).withArtifact(info).build().toJson();
    }

    /** {@inheritDoc} */
    @Override
    public String getUpdates() {
        Map<Artifact, String> updates;

        try {
            updates = manager.getUpdates();
        } catch (IOException e) {
            return Response.valueOf(e).toJson();
        }

        return new Response.Builder().withStatus(ResponseCode.OK).withArtifacts(updates).build().toJson();
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
}