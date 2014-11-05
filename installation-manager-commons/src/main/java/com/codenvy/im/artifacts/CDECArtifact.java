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

import com.codenvy.api.core.rest.shared.dto.ApiInfo;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.createDtoFromJson;

/** @author Anatoliy Bazko */
@Singleton
public class CDECArtifact extends AbstractArtifact {
    public static final String NAME = "cdec";

    private final HttpTransport transport;
    private final String apiNodeUrl;

    @Inject
    public CDECArtifact(@Named("cdec.api-node.url") String apiNodeUrl, HttpTransport transport) {
        super(NAME);
        this.transport = transport;
        this.apiNodeUrl = apiNodeUrl;
    }

    /** {@inheritDoc} */
    @Override
    public void install(Path pathToBinaries) throws IOException {
        throw new UnsupportedOperationException("CDEC installation is not supported yet.");
    }

    /** {@inheritDoc} */
    @Override
    public String getInstalledVersion(String authToken) throws IOException {
        ApiInfo apiInfo = createDtoFromJson(transport.doOption(combinePaths(apiNodeUrl, "api/"), authToken), ApiInfo.class);
        return apiInfo.getIdeVersion();
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstallable(Version versionToInstall, String accessToken) {
        return false; // temporarily
    }

    /** {@inheritDoc} */
    @Override
    protected Path getInstalledPath() throws URISyntaxException {
        throw new UnsupportedOperationException();
    }
}
