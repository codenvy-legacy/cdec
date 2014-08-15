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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codenvy.cdec.utils.InjectorBootstrap.INJECTOR;

/**
 * @author Dmytro Nochevnov
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerServiceImpl.class);

    InstallationManager manager;

    public InstallationManagerServiceImpl() {
        manager = INJECTOR.getInstance(InstallationManagerImpl.class);
    }

    @Override
    public JsonRepresentation doGetAvailable2DownloadArtifacts() {
        // TODO
        return null;
    }

    @Override
    public JsonRepresentation doDownloadUpdates() {
        // TODO
        return null;
    }

    @Override
    public JsonRepresentation doGetNewVersions() {
        return null;
        // TODO
//        Map<Artifact, String> newVersions = manager.getNewVersions();

    }

    @Override
    public JsonRepresentation doCheckNewVersions(final String version) throws JSONException {
//        try {
//            manager.checkNewVersions();
//        } catch (IllegalArgumentException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

        JSONArray artifacts = new JSONArray();

        JSONObject artifact1 = new JSONObject();
        artifact1.put("version", version);
        artifact1.put("status", "downloaded");

        artifacts.put(artifact1);

        JsonRepresentation response = new JsonRepresentation(artifacts);

        return response;
    }

    @Override
    public void obtainChallengeRequest() {
    }
}
