/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
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
package com.codenvy.im.facade;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactFactory;
import com.codenvy.im.artifacts.ArtifactNotFoundException;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.utils.AuthenticationException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetUpdatesFacade {

    @Mock
    private HttpTransport           transport;
    @Mock
    private SaasAuthServiceProxy    saasAuthServiceProxy;
    @Mock
    private SaasAccountServiceProxy saasAccountServiceProxy;
    @Mock
    private PasswordManager         passwordManager;
    @Mock
    private NodeManager             nodeManager;
    @Mock
    private BackupManager           backupManager;
    @Mock
    private StorageManager          storageManager;
    @Mock
    private InstallManager          installManager;
    @Mock
    private DownloadManager         downloadManager;


    private InstallationManagerFacade installationManagerService;
    private Artifact                  installManagerArtifact;
    private Artifact                  cdecArtifact;

    @BeforeMethod
    public void init() throws ArtifactNotFoundException {
        MockitoAnnotations.initMocks(this);
        installationManagerService = new InstallationManagerFacade("target/download",
                                                                   "update/endpoint",
                                                                   transport,
                                                                   saasAuthServiceProxy,
                                                                   saasAccountServiceProxy,
                                                                   passwordManager,
                                                                   nodeManager,
                                                                   backupManager,
                                                                   storageManager,
                                                                   installManager,
                                                                   downloadManager);
        installManagerArtifact = ArtifactFactory.createArtifact(InstallManagerArtifact.NAME);
        cdecArtifact = ArtifactFactory.createArtifact(CDECArtifact.NAME);
    }

    @Test
    public void testGetUpdates() throws Exception {
        final Version version100 = Version.valueOf("1.0.0");
        when(downloadManager.getUpdates()).thenReturn(new LinkedHashMap<Artifact, Version>() {
            {
                put(installManagerArtifact, version100);
                put(cdecArtifact, Version.valueOf("2.10.5"));
            }
        });

        when(downloadManager.getDownloadedVersions(installManagerArtifact)).thenReturn(new TreeMap<Version, Path>() {{
            put(version100, null);
        }});

        when(downloadManager.getDownloadedVersions(cdecArtifact)).thenReturn(new TreeMap<Version, Path>());

        String response = installationManagerService.getUpdates();
        assertEquals(response, "{\n" +
                               "  \"artifacts\" : [ {\n" +
                               "    \"artifact\" : \"" + InstallManagerArtifact.NAME + "\",\n" +
                               "    \"version\" : \"1.0.0\",\n" +
                               "    \"status\" : \"DOWNLOADED\"\n" +
                               "  }, {\n" +
                               "    \"artifact\" : \"codenvy\",\n" +
                               "    \"version\" : \"2.10.5\"\n" +
                               "  } ],\n" +
                               "  \"status\" : \"OK\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesAuthenticationException() throws Exception {
        when(downloadManager.getUpdates()).thenThrow(new AuthenticationException());

        String response = installationManagerService.getUpdates();
        assertEquals(response, "{\n" +
                               "  \"message\" : \"Authentication error. Authentication token might be expired or invalid.\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesArtifactNotFoundException() throws Exception {
        when(downloadManager.getUpdates()).thenThrow(new ArtifactNotFoundException(cdecArtifact));

        String response = installationManagerService.getUpdates();

        assertEquals(response, "{\n" +
                               "  \"message\" : \"Artifact 'codenvy' not found\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }

    @Test
    public void testGetUpdatesCatchesException() throws Exception {
        when(downloadManager.getUpdates()).thenThrow(new IOException("Error"));

        String response = installationManagerService.getUpdates();

        assertEquals(response, "{\n" +
                               "  \"message\" : \"Error\",\n" +
                               "  \"status\" : \"ERROR\"\n" +
                               "}");
    }
}
