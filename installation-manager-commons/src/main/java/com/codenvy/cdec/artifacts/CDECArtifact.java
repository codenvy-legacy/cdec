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

import com.codenvy.cdec.utils.Commons;
import com.codenvy.cdec.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.util.Map;

import static com.codenvy.cdec.utils.Commons.combinePaths;

/**
 * @author Anatoliy Bazko
 */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    private static final long serialVersionUID = 1L;

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
    public String getCurrentVersion() throws IOException {
        String json = transport.doGetRequest(combinePaths(updateEndpoint, "repository/info/" + NAME));
        Map m = Commons.fromJson(json, Map.class);
        return (String)m.get("version");
    }

    @Override
    public boolean isValidSubscriptionRequired() {
        return true;
    }
}
