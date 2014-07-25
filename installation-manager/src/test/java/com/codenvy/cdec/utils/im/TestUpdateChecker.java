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
package com.codenvy.cdec.utils.im;

import com.codenvy.cdec.Artifact;
import com.codenvy.cdec.im.UpdateChecker;
import com.codenvy.cdec.utils.HttpTransport;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Anatoliy Bazko
 */
public class TestUpdateChecker {

    private UpdateChecker              updateChecker;
    private HttpTransport              transport;
    private UpdateChecker.CheckUpdates checkUpdates;

    @BeforeMethod
    public void setUp() throws Exception {
        transport = mock(HttpTransport.class);
        updateChecker = new UpdateChecker("update-server-endpoint", "", "", false, transport);
        checkUpdates = updateChecker.new CheckUpdates();
    }

    @Test
    public void testGetAvailable2DownloadArtifacts() throws Exception {
        when(transport.doGetRequest("update-server-endpoint/repository/version/" + Artifact.INSTALL_MANAGER)).thenReturn("{value:1.0.1}");
        when(transport.doGetRequest("update-server-endpoint/repository/version/" + Artifact.CDEC)).thenReturn("{value:2.1.12}");
        when(transport.doGetRequest("update-server-endpoint/repository/version/" + Artifact.PUPPET_CLIENT)).thenThrow(IOException.class);
        Map<String, String> m = checkUpdates.getAvailable2DownloadArtifacts();

        assertEquals(m.size(), 2);
        assertEquals(m.get(Artifact.INSTALL_MANAGER.toString()), "1.0.1");
        assertEquals(m.get(Artifact.CDEC.toString()), "2.1.12");
    }

    @Test
    public void testGetExistedArtifacts() throws Exception {
        Map<String, String> m = checkUpdates.getExistedArtifacts();
        assertNotNull(m.get(Artifact.INSTALL_MANAGER.toString()));
    }
}
