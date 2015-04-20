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
import com.codenvy.im.response.ResponseCode;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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

    private com.codenvy.im.response.Response mockFacadeOkResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.OK);

    private com.codenvy.im.response.Response mockFacadeErrorResponse = new com.codenvy.im.response.Response().setStatus(ResponseCode.ERROR)
                                                                                                             .setMessage("error");

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = spy(new InstallationManagerService(mockFacade));
    }

    @Test
    public void testStartDownload() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).startDownload(mockRequest);
        Response result = service.startDownload(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).startDownload(mockRequest);
        result = service.startDownload(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testStopDownload() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).stopDownload();
        Response result = service.stopDownload();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).stopDownload();
        result = service.stopDownload();
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloadStatus() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloadStatus();
        Response result = service.getDownloadStatus();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloadStatus();
        result = service.getDownloadStatus();
        checkErrorResponse(result);
    }

    @Test
    public void testGetUpdates() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getUpdates(mockRequest);
        Response result = service.getUpdates(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getUpdates(mockRequest);
        result = service.getUpdates(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testGetDownloads() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getDownloads(mockRequest);
        Response result = service.getDownloads(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getDownloads(mockRequest);
        result = service.getDownloads(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testGetInstalledVersions() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstalledVersions(mockRequest);
        Response result = service.getInstalledVersions(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstalledVersions(mockRequest);
        result = service.getInstalledVersions(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testInstall() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).install(mockRequest);
        Response result = service.install(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).install(mockRequest);
        result = service.install(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testGetInstallInfo() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getInstallInfo(mockRequest);
        Response result = service.getInstallInfo(mockRequest);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getInstallInfo(mockRequest);
        result = service.getInstallInfo(mockRequest);
        checkErrorResponse(result);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).getConfig();
        Response result = service.getConfig();
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).getConfig();
        result = service.getConfig();
        checkErrorResponse(result);
    }

    @Test
    public void testAddNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).addNode("dns");
        Response result = service.addNode("dns");
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).addNode("dns");
        result = service.addNode("dns");
        checkErrorResponse(result);
    }

    @Test
    public void testRemoveNode() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).removeNode("dns");
        Response result = service.removeNode("dns");
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).removeNode("dns");
        result = service.removeNode("dns");
        checkErrorResponse(result);
    }

    @Test
    public void testBackup() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).backup(mockBackupConfig);
        Response result = service.backup(mockBackupConfig);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).backup(mockBackupConfig);
        result = service.backup(mockBackupConfig);
        checkErrorResponse(result);
    }

    @Test
    public void testRestore() throws Exception {
        doReturn(mockFacadeOkResponse.toJson()).when(mockFacade).restore(mockBackupConfig);
        Response result = service.restore(mockBackupConfig);
        checkOkResponse(result);

        doReturn(mockFacadeErrorResponse.toJson()).when(mockFacade).restore(mockBackupConfig);
        result = service.restore(mockBackupConfig);
        checkErrorResponse(result);
    }

    @Test
    public void testHandleIncorrectRFacadeResponse() throws Exception {
        doReturn("{").when(mockFacade).startDownload(mockRequest);
        Response result = service.startDownload(mockRequest);

        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        String facadeResponse = (String)result.getEntity();
        assertTrue(facadeResponse.contains("org.eclipse.che.commons.json.JsonParseException: com.fasterxml.jackson.core.JsonParseException: " +
                                           "Unexpected end-of-input: expected close marker for OBJECT"));
    }

    private void checkOkResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.OK.getStatusCode());
        com.codenvy.im.response.Response facadeResponse = (com.codenvy.im.response.Response)result.getEntity();
        assertEquals(facadeResponse.toJson(), mockFacadeOkResponse.toJson());
    }

    private void checkErrorResponse(Response result) {
        assertEquals(result.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        com.codenvy.im.response.Response facadeResponse = (com.codenvy.im.response.Response)result.getEntity();
        assertEquals(facadeResponse.toJson(), mockFacadeErrorResponse.toJson());
    }
}
