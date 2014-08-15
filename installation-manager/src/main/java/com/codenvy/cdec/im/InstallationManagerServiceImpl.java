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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.server.InstallationManagerService;
import com.codenvy.cdec.utils.BasedInjector;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Dmytro Nochevnov
 */
@Singleton
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerServiceImpl.class);
    
    protected InstallationManager manager;

    @Inject
    public InstallationManagerServiceImpl() {
        manager = BasedInjector.getInstance().getInstance(InstallationManagerImpl.class);
    }        
    
    public void doGetAvailable2DownloadArtifacts() {
        // TODO
//        return null;
    }

    public void doGetNewVersions() {
        // TODO
//        Map<Artifact, String> newVersions = manager.getNewVersions();
        
    }
    
    @Override
    public void downloadUpdate(String artifactName, String version) {
        // TODO
//        return null;
    }


    @Override
    public JsonRepresentation checkUpdates() throws JSONException {
        Map<Artifact, String> newVersions = new HashMap<>();
        
        try {
            manager.checkNewVersions();
            newVersions = manager.getNewVersions();

        } catch (Exception e) {
            throw new RuntimeException(e);  // TODO
        }
        
        JSONArray updates = new JSONArray();
        for (Entry<Artifact, String> update: newVersions.entrySet()) {
            Artifact artifact = update.getKey();
            String version = update.getValue();
            
            JSONObject updateDescription = new JSONObject();
            updateDescription.put("artifact", artifact.getName());
            updateDescription.put("version", version);

            updates.put(updateDescription);
        }
        
        JsonRepresentation response = new JsonRepresentation(updates);        
        return response;
    }

    @Override
    public void empty() {}
}
