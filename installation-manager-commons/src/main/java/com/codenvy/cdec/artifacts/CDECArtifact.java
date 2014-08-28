/*
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
package com.codenvy.cdec.artifacts;

import static com.codenvy.cdec.utils.Commons.combinePaths;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Named;

import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String        updateEndpoint;

    @Inject
    public CDECArtifact(@Named("codenvy.installation-manager.update_endpoint") String updateEndpoint,
                        HttpTransport transport) {
        super(NAME);
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
    }

    @Override
    public void install(Path pathToBinaries) throws IOException {
        // TODO
    }

    // TODO remove duplicate of getCurrentVersion(String accessToken)
    @Override
    public String getCurrentVersion() throws IOException {
        return getCurrentVersion(null);
    }
    
    @Override
    public String getCurrentVersion(String accessToken) throws IOException {
        String json = transport.doGetRequest(combinePaths(updateEndpoint, "repository/info/" + NAME), accessToken);
        Map m = Commons.fromJson(json, Map.class);
        return (String)m.get("version");
    }

    @Override
    public boolean isValidSubscriptionRequired() {
        return true;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        return null;
        // TODO
    }
}