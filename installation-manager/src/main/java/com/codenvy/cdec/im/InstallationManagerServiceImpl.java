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

import java.io.IOException;
import java.util.Map;

import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codenvy.cdec.artifacts.Artifact;
import com.codenvy.cdec.server.InstallationManager;
import com.codenvy.cdec.server.InstallationManagerService;
import com.codenvy.cdec.utils.BasedInjector;

/**
 * @author Dmytro Nochevnov
 */
public class InstallationManagerServiceImpl extends ServerResource implements InstallationManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(InstallationManagerServiceImpl.class);

    InstallationManager manager;

    public InstallationManagerServiceImpl() {
        manager = BasedInjector.getInstance().getInstance(InstallationManagerImpl.class);
    }    
    
    public void doGetAvailable2DownloadArtifacts() {
        // TODO
    }
    
    public void doDownloadUpdates() {
        // TODO
    }

    public String doGetNewVersions() {
        // TODO
        Map<Artifact, String> newVersions = manager.getNewVersions();
        return newVersions.toString();
    }

    public String doCheckNewVersions() {
        try {
            manager.checkNewVersions();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return "OK";  // TODO
    }

    @Override
    public void empty() {}
}
