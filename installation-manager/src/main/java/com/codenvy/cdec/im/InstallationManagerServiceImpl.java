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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.resource.ServerResource;

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

    @Override
    public String downloadUpdates() throws IOException {
        try {
           manager.downloadUpdates();  // download latest version of all artifacts
            
        } catch(RuntimeException e) {
            Response response = new Response(new Status(StatusCode.FAILURE, e.getMessage().toString()));            
            return Commons.getJson(response);
        }
        
        Response response = new Response(new Status(StatusCode.SUCCESS));
        
        return Commons.getJson(response);
    }

    @Override
    public String downloadUpdate(String artifactName) throws IOException {
        String version;
        
        try {
            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            
            version = manager.getNewVersions().get(artifact);
            manager.downloadArtifact(artifact, version);
                            
        } catch(RuntimeException e) {
            Response response = new Response(new Status(StatusCode.FAILURE, e.getMessage().toString()));
            return Commons.getJson(response);
        }
        
        ArtifactInfo artifactInfo = new ArtifactInfo(new Status(StatusCode.DOWNLOADED), artifactName, version);
        Response response = new Response(new Status(StatusCode.SUCCESS), artifactInfo);
        
        return Commons.getJson(response);
    }
    
    @Override
    public String download(String artifactName, String version) throws IOException {
        try {
            Artifact artifact = ArtifactFactory.createArtifact(artifactName);
            manager.downloadArtifact(artifact, version);
                            
        } catch(RuntimeException e) {
            Response response = new Response(new Status(StatusCode.FAILURE, e.getMessage().toString()));            
            return Commons.getJson(response);
        }
        
        ArtifactInfo artifactInfo = new ArtifactInfo(new Status(StatusCode.DOWNLOADED), artifactName, version);
        Response response = new Response(new Status(StatusCode.SUCCESS), artifactInfo);
        
        return Commons.getJson(response);
    }

    @Override
    public String checkUpdates() throws IOException {
        Map<Artifact, String> newVersions = new HashMap<>();
        
        try {
            manager.checkNewVersions();
            newVersions = manager.getNewVersions();

        } catch (Exception e) {
            Response response = new Response(new Status(StatusCode.FAILURE, e.getMessage().toString()));            
            return Commons.getJson(response);
        }
        
        List<ArtifactInfo> artifacts = new ArrayList<>(newVersions.keySet().size());
        for (Entry<Artifact, String> artifactEntry: newVersions.entrySet()) {
            String artifactName = artifactEntry.getKey().getName();
            String version = artifactEntry.getValue();
            
            ArtifactInfo artifactInfo = new ArtifactInfo(artifactName, version);
            
            artifacts.add(artifactInfo);
        }
        
        Response response = new Response(new Status(StatusCode.SUCCESS), artifacts);
        
        return Commons.getJson(response);
    }

    @Override
    public void obtainChallengeRequest() {
    }

}