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
package com.codenvy.im.service;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.exceptions.ArtifactNotFoundException;
import com.codenvy.im.exceptions.AuthenticationException;
import com.codenvy.im.request.Request;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetUpdatesInstallationManagerServiceImpl {
    private InstallationManagerService installationManagerService;

    private InstallationManager mockInstallationManager;
    private HttpTransport       transport;
    private Artifact            installManagerArtifact;
    private Artifact            cdecArtifact;
    private Request             request;

    @BeforeMethod
    public void init() throws ArtifactNotFoundException {
        initMocks();
        installationManagerService = new InstallationManagerServiceImpl("update/endpoint", "api/endpoint", mockInstallationManager, transport);
        request = new Request().setUserCredentials(new UserCredentials("auth token", "accountId"));
    }

    public void initMocks() throws ArtifactNotFoundException {
        mockInstallationManager = PowerMockito.mock(InstallationManagerImpl.class);
        transport = Mockito.mock(HttpTransport.class);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        when(mockInstallationManager.getUpdates(anyString())).thenReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(installManagerArtifact, version100);
                put(cdecArtifact, Version.valueOf("2.10.5"));
            }
        });

        when(mockInstallationManager.getDownloadedVersions(installManagerArtifact)).thenReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }});

        when(mockInstallationManager.getDownloadedVersions(cdecArtifact)).thenReturn(new TreeMap<Version, Path>());

        String response = installationManagerService.getUpdates(request);
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"installation-manager\",\n" +
                               "    \"version\" : \"1.0.0\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"cdec\",\n" +
                               "    \"version\" : \"2.10.5\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesAuthenticationException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new AuthenticationException());

        Request request = new Request().setUserCredentials(new UserCredentials("incorrect-token", "accountId"));

        String response = installationManagerService.getUpdates(request);
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Authentication error. Authentication token might be expired or invalid.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesArtifactNotFoundException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new ArtifactNotFoundException(cdecArtifact));

        String response = installationManagerService.getUpdates(request);

        assertEquals(response, "{\n" +
                               "  \"message\" : \"Artifact 'cdec' not found\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesException() throws Exception {
        when(mockInstallationManager.getUpdates(anyString())).thenThrow(new IOException("Error"));

        Request request = new Request().setUserCredentials(new UserCredentials("incorrect-token", "accountId"));

        String response = installationManagerService.getUpdates(request);

        assertEquals(response, "{\n" +
                               "  \"message\" : \"Error\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
