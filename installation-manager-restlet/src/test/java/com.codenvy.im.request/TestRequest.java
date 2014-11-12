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
package com.codenvy.im.request;


import com.codenvy.im.installer.InstallOptions;
import com.codenvy.im.installer.Installer;
import com.codenvy.im.user.UserCredentials;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestRequest {
    @Test
    public void testJacksonRepresentation() throws Exception {
        String testId = UUID.randomUUID().toString();
        Request testRequest = new Request()
            .setArtifactName("artifact name")
            .setVersion("artifact version")
            .setUserCredentials(new UserCredentials("test token", "test account id"))
            .setInstallOptions(new InstallOptions().setType(Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER).setId(testId));

        JacksonRepresentation<Request> requestRep = testRequest.toRepresentation();
        assertNotNull(requestRep);

        Request restoredRequest = Request.fromRepresentation(requestRep);
        assertNotNull(restoredRequest);

        assertEquals(restoredRequest.getArtifactName(), "artifact name");
        assertEquals(restoredRequest.getVersion(), "artifact version");

        assertEquals(restoredRequest.getUserCredentials().getToken(), "test token");
        assertEquals(restoredRequest.getUserCredentials().getAccountId(), "test account id");

        assertEquals(restoredRequest.getInstallOptions().getType(), Installer.Type.CDEC_SINGLE_NODE_WITH_PUPPET_MASTER);
        assertEquals(restoredRequest.getInstallOptions().getId(), testId);
    }
}
