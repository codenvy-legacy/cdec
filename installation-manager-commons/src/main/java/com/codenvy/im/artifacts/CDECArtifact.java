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
package com.codenvy.im.artifacts;

import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

/** @author Anatoliy Bazko */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String        updateEndpoint;

    @Inject
    public CDECArtifact(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                        HttpTransport transport) {
        super(NAME);
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
    }

    @Override
    public void install(Path pathToBinaries) throws IOException {
        throw new UnsupportedOperationException("CDEC installation is not supported yet.");
    }

    @Override
    public String getInstalledVersion(String accessToken) throws IOException {
        return "UNKNOWN";
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override public boolean isInstallable(Version versionToInstall, String accessToken) {
        return false;
    }

    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }
}
