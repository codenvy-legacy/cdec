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

import com.codenvy.cdec.InstallationManager;
import com.codenvy.cdec.InstallationManagerService;
import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.artifacts.ArtifactFactory;
import com.codenvy.cdec.im.service.response.ArtifactInfo;
import com.codenvy.cdec.im.service.response.Response;
import com.codenvy.cdec.im.service.response.Response.Status;
import com.codenvy.cdec.im.service.response.StatusCode;
import com.codenvy.cdec.utils.Commons;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static com.codenvy.cdec.utils.Commons.getJson;
import static com.codenvy.cdec.utils.InjectorBootstrap.INJECTOR;

/**
 * @author Dmytro Nochevnov
 * TODO check
 */
@Singleton
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {   
    protected InstallationManager manager;

    @Inject
    public InstallationManagerServiceImpl() {
        manager = INJECTOR.getInstance(InstallationManagerImpl.class);
    }

    /** {@inheritDoc} */
    @Override
    public String download() throws IOException {

        return null;
    }

    /** {@inheritDoc} */
    // TODO
    @Override
    public String download(String artifactName) throws IOException {
        try {
            manager.download();

        } catch (RuntimeException e) {
            Response response = new Response(new Status(StatusCode.ERROR, e.getMessage()));
            return Commons.getJson(response);
        }

        Response response = new Response(new Status(StatusCode.OK));
        return Commons.getJson(response);
    }

    /** {@inheritDoc} */
    @Override
    //  TODO shouldn't throw exception
    // TODO if no need to download, what need to return?
    public String download(String artifactName, String version) throws IOException {
        try {
            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            
            if (version == null) {
                // download latest version of artifact
                version = manager.getUpdates().get(artifact);
            }

            manager.download(artifact, version);

        } catch(RuntimeException e) {
            Response response = new Response(new Status(StatusCode.ERROR, e.getMessage()));
            return getJson(response);
        }
        
        ArtifactInfo artifactInfo = new ArtifactInfo(new Status(StatusCode.DOWNLOADED), artifactName, version);
        List<ArtifactInfo> artifacts = Arrays.asList(artifactInfo);
        Response response = new Response(new Status(StatusCode.OK), artifacts);

        return getJson(response);
    }

    /** {@inheritDoc} */
    @Override
    // TODO rename
    public String checkUpdates() throws IOException {
        Map<Artifact, String> newVersions;

        try {
            newVersions = manager.getUpdates();
        } catch (IOException e) {
            Response response = new Response(new Status(StatusCode.ERROR, e.getMessage()));
            return getJson(response);
        }

        Set<Entry<Artifact, String>> entries = newVersions.entrySet();
        List<ArtifactInfo> artifacts = new ArrayList<>(entries.size());

        for (Entry<Artifact, String> artifactEntry : entries) {
            String artifactName = artifactEntry.getKey().getName();
            String version = artifactEntry.getValue();

            ArtifactInfo artifactInfo = new ArtifactInfo(artifactName, version);

            artifacts.add(artifactInfo);
        }

        Response response = new Response(new Status(StatusCode.OK), artifacts);
        return getJson(response);
    }

    /** {@inheritDoc} */
    @Override
    public void obtainChallengeRequest() {
        // do nothing
    }
}