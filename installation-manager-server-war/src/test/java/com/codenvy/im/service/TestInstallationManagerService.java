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
package com.codenvy.im.service;

import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.request.Request;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerService {

    private InstallationManagerService service;

    @Mock
    private Request mockRequest;

    @Mock
    private BackupConfig mockBackupConfig;

    @Mock
    private InstallationManagerFacade mockFacade;

    private String mockResponse = "{}";

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = spy(new InstallationManagerService(mockFacade));
    }

    @Test
    public void testStartDownload() throws Exception {
        doReturn(mockResponse).when(mockFacade).startDownload(mockRequest);
        String result = service.startDownload(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testStopDownload() throws Exception {
        doReturn(mockResponse).when(mockFacade).stopDownload();
        String result = service.stopDownload();
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetDownloadStatus() throws Exception {
        doReturn(mockResponse).when(mockFacade).getDownloadStatus();
        String result = service.getDownloadStatus();
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(mockResponse).when(mockFacade).getUpdates(mockRequest);
        String result = service.getUpdates(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetDownloads() throws Exception {
        doReturn(mockResponse).when(mockFacade).getDownloads(mockRequest);
        String result = service.getDownloads(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetInstalledVersions() throws Exception {
        doReturn(mockResponse).when(mockFacade).getInstalledVersions(mockRequest);
        String result = service.getInstalledVersions(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testInstall() throws Exception {
        doReturn(mockResponse).when(mockFacade).install(mockRequest);
        String result = service.install(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        doReturn(mockResponse).when(mockFacade).getInstallInfo(mockRequest);
        String result = service.getInstallInfo(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testAddTrialSubscription() throws Exception {
        doReturn(mockResponse).when(mockFacade).addTrialSubscription(mockRequest);
        String result = service.addTrialSubscription(mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testCheckSubscription() throws Exception {
        doReturn(mockResponse).when(mockFacade).checkSubscription("id", mockRequest);
        String result = service.checkSubscription("id", mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetVersionToInstall() throws Exception {
        doReturn(mockResponse).when(mockFacade).getVersionToInstall(mockRequest, 1);
        String result = service.getVersionToInstall(mockRequest, 1);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetAccountReferenceWhereUserIsOwner() throws Exception {
        doReturn(mockResponse).when(mockFacade).getAccountReferenceWhereUserIsOwner("account", mockRequest);
        String result = service.getAccountReferenceWhereUserIsOwner("account", mockRequest);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(mockResponse).when(mockFacade).getConfig();
        String result = service.getConfig();
        assertEquals(result, mockResponse);
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(mockResponse).when(mockFacade).addNode("dns");
        String result = service.addNode("dns");
        assertEquals(result, mockResponse);
    }

    @Test
    public void testRemoveNode() throws Exception {
        doReturn(mockResponse).when(mockFacade).removeNode("dns");
        String result = service.removeNode("dns");
        assertEquals(result, mockResponse);
    }

    @Test
    public void testBackup() throws Exception {
        doReturn(mockResponse).when(mockFacade).backup(mockBackupConfig);
        String result = service.backup(mockBackupConfig);
        assertEquals(result, mockResponse);
    }

    @Test
    public void testRestore() throws Exception {
        doReturn(mockResponse).when(mockFacade).restore(mockBackupConfig);
        String result = service.restore(mockBackupConfig);
        assertEquals(result, mockResponse);
    }
}
