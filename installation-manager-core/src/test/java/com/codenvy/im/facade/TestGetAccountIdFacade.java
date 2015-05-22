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

import com.codenvy.im.managers.BackupManager;
import com.codenvy.im.managers.DownloadManager;
import com.codenvy.im.managers.InstallManager;
import com.codenvy.im.managers.NodeManager;
import com.codenvy.im.managers.PasswordManager;
import com.codenvy.im.managers.StorageManager;
import com.codenvy.im.request.Request;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasAuthServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.AuthenticationException;
import com.codenvy.im.utils.HttpTransport;

import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author Dmytro Nochevnov
 */
public class TestGetAccountIdFacade {
    public static final String TEST_ACCOUNT_ID = "accountId";
    public static final String ACCOUNT_NAME    = "accountName";

    private SaasUserCredentials testCredentials;
    private String              accountApiEndpoint;
    private Request             request;

    @Mock
    private HttpTransport        transport;
    @Mock
    private SaasAuthServiceProxy saasAuthServiceProxy;
    @Mock
    private PasswordManager      passwordManager;
    @Mock
    private NodeManager          nodeManager;
    @Mock
    private BackupManager        backupManager;
    @Mock
    private StorageManager       storageManager;
    @Mock
    private InstallManager       installManager;
    @Mock
    private DownloadManager      downloadManager;

    private InstallationManagerFacade installationManagerService;

    @BeforeMethod
    public void init() {
        initMocks(this);
        SaasAccountServiceProxy saasAccountServiceProxy = new SaasAccountServiceProxy("api/endpoint", transport);
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
        accountApiEndpoint = combinePaths("api/endpoint", "account");
        testCredentials = new SaasUserCredentials("auth token", null);
        request = new Request().setSaasUserCredentials(testCredentials).setSaasUserCredentials(testCredentials);
    }

    @Test
    public void testGetAccountReference() throws Exception {
        doReturn("[{"
                 + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                 + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                 + "}]").when(transport).doGet(accountApiEndpoint, testCredentials.getToken());
        AccountReference accountRef = installationManagerService.getAccountWhereUserIsOwner(null, request);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), TEST_ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }

    @Test
    public void testGetAccountReferenceFromSeveral() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
            .thenReturn("[{"
                        + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                        + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                        + "},{roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"another-account-id\"}"
                            + "}]");
        AccountReference accountRef = installationManagerService.getAccountWhereUserIsOwner(null, request);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), TEST_ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }

    @Test
    public void testGetAccountReferenceReturnNullIfAccountWasNotFound() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"account/member\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\"}"
                            + "}]");
        AccountReference accountRef = installationManagerService.getAccountWhereUserIsOwner(null, request);
        assertNull(accountRef);
    }

    @Test(expectedExceptions = IOException.class)
    public void testGetAccountReferenceErrorIfAuthenticationFailed() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken())).thenThrow(new AuthenticationException());

        installationManagerService.getAccountWhereUserIsOwner(null, request);
    }

    @Test
    public void testGetAccountReferenceWithSpecificName() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "}]");
        AccountReference accountRef = installationManagerService.getAccountWhereUserIsOwner(ACCOUNT_NAME, request);

        assertNotNull(accountRef);
        assertEquals(accountRef.getId(), TEST_ACCOUNT_ID);
        assertEquals(accountRef.getName(), ACCOUNT_NAME);
    }

    @Test
    public void testGetAccountReferenceWithSpecificNameReturnNullIfAccountWasNotFound() throws Exception {
        when(transport.doGet(accountApiEndpoint, testCredentials.getToken()))
                .thenReturn("[{"
                            + "roles:[\"" + SaasAccountServiceProxy.ACCOUNT_OWNER_ROLE + "\"],"
                            + "accountReference:{id:\"" + TEST_ACCOUNT_ID + "\",name:\"" + ACCOUNT_NAME + "\"}"
                            + "}]");
        AccountReference accountRef = installationManagerService.getAccountWhereUserIsOwner("some name", request);
        assertNull(accountRef);
    }

}
