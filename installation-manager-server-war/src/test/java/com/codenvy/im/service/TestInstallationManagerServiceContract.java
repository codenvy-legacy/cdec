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

import com.codenvy.api.subscription.shared.dto.SubscriptionDescriptor;
import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.facade.IMCliFilteredFacade;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.DownloadAlreadyStartedException;
import com.codenvy.im.managers.DownloadNotStartedException;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.response.BackupInfo;
import com.codenvy.im.response.DownloadProgressResponse;
import com.codenvy.im.response.NodeInfo;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;

import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Dmytro Nochevnov
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestInstallationManagerServiceContract {
    public static final String OK_RESPONSE_BODY = "{\n"
                                                  + "    \n"
                                                  + "}";
    @Mock
    public IMCliFilteredFacade facade;
    @Mock
    public ConfigManager       configManager;
    @Mock
    public SaasUserCredentials saasUserCredentials;

    public InstallationManagerService service;

    enum HttpMethod {GET, POST, PUT, DELETE}

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        service = new InstallationManagerService("", facade, configManager);
    }

    @Test
    public void testBackup() {
        testContract(
                "backup",                                     // path
                ImmutableMap.of("artifact", CDECArtifact.NAME,
                                "backupDir", "test"),         // query parameters
                null,                                         // request body
                null,                                         // consume content type
                ContentType.JSON,                             // produce content type
                HttpMethod.POST,                              // HTTP method
                OK_RESPONSE_BODY,                             // response body
                Response.Status.CREATED,                           // response status
                new Function<Object, Object>() {              // before test
                    @Nullable
                    @Override
                    public Object apply(@Nullable Object o) {
                        try {
                            doReturn(new BackupInfo()).when(facade).backup(any(BackupConfig.class));
                        } catch (IOException e) {
                            fail(e.getMessage(), e);
                        }
                        return null;
                    }
                },
                null // assertion
                    );
    }

    @Test
    public void testRestore() {
        testContract(
                "restore",                                    // path
                ImmutableMap.of("artifact", CDECArtifact.NAME,
                                "backupFile", "test"),        // query parameters
                null,                                         // request body
                null,                                         // consume content type
                ContentType.JSON,                             // produce content type
                HttpMethod.POST,                              // HTTP method
                OK_RESPONSE_BODY,                             // response body
                Response.Status.CREATED,                           // response status
                new Function<Object, Object>() {              // before test
                    @Nullable
                    @Override
                    public Object apply(@Nullable Object o) {
                        BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                                          .setBackupFile("test");
                        try {
                            doReturn(new BackupInfo()).when(facade).restore(testBackupConfig);
                        } catch (IOException e) {
                            fail(e.getMessage(), e);
                        }
                        return null;
                    }
                },
                null // assertion
                    );
    }

    @Test
    public void testAddNode() {
        testContract(
             "node",                          // path
             ImmutableMap.of("dns", "test"),  // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.POST,                 // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.CREATED,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     try {
                         doReturn(new NodeInfo()).when(facade).addNode("test");
                     } catch (IOException e) {
                         fail(e.getMessage(), e);
                     }
                     return null;
                 }
             },
             null // assertion
        );
    }

    @Test
    public void testRemoveNode() {
        testContract(
            "node",                          // path
            ImmutableMap.of("dns", "test"),  // query parameters
            null,                            // request body
            null,                            // consume content type
            null,                // produce content type
            HttpMethod.DELETE,               // HTTP method
            null,                // response body
            Response.Status.NO_CONTENT,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(new NodeInfo()).when(facade).removeNode("test");
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetDownloads() {
        testContract(
            "downloads",                                     // path
            ImmutableMap.of("artifact", CDECArtifact.NAME),  // query parameters
            null,                            // request body
            null,                            // consume content type
            ContentType.JSON,                // produce content type
            HttpMethod.GET,                  // HTTP method
            "{\n" +
            "    \"id\": \"id\"\n" +
            "}",                // response body
            Response.Status.OK,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn("id").when(facade).getDownloadIdInProgress();
                    } catch (DownloadNotStartedException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testStartDownload() {
        testContract(
            "downloads",                                  // path
            ImmutableMap.of("artifact", CDECArtifact.NAME,
                            "version", "1.0.0"),          // query parameters
            null,                            // request body
            null,                            // consume content type
            ContentType.JSON,                // produce content type
            HttpMethod.POST,                 // HTTP method
            null,                            // response body
            Response.Status.ACCEPTED,        // response status
            null,                            // before test
            new Function<Object, Object>() { // assertion
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).startDownload(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.0"));
                    } catch (InterruptedException | DownloadAlreadyStartedException | IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            });
    }

    @Test
    public void testStopDownload() {
        testContract(
            "downloads/123",                 // path
            null,                            // query parameters
            null,                            // request body
            null,                            // consume content type
            null,                            // produce content type
            HttpMethod.DELETE,               // HTTP method
            null,                            // response body
            Response.Status.NO_CONTENT,      // response status
            null,                            // before test
            new Function<Object, Object>() { // assertion
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).stopDownload();
                    } catch (InterruptedException | DownloadNotStartedException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            });
    }

    @Test
    public void testGetDownloadStatus() {
        testContract(
            "downloads/123",                 // path
            null,                            // query parameters
            null,                            // request body
            null,                            // consume content type
            ContentType.JSON,                // produce content type
            HttpMethod.GET,                  // HTTP method
            "{\n" +
            "    \"percents\": 0\n" +
            "}",                           // response body
            Response.Status.OK,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        DownloadProgressResponse downloadDescriptor = new DownloadProgressResponse();
                        doReturn(downloadDescriptor).when(facade).getDownloadProgress();
                    } catch (IOException | DownloadNotStartedException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetInstalledVersions() {
        testContract(
                "installations",                  // path
            null,                            // query parameters
            null,                            // request body
            null,                            // consume content type
            ContentType.JSON,                // produce content type
            HttpMethod.GET,                  // HTTP method
            "[\n" +
            "    \n" +
            "]",                // response body
            Response.Status.OK,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(ImmutableList.of()).when(facade).getInstalledVersions();
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetUpdates() {
        testContract(
                "updates",                        // path
            null,                            // query parameters
            null,                            // request body
            null,                            // consume content type
            ContentType.JSON,                // produce content type
            HttpMethod.GET,                  // HTTP method
            "[\n" +
            "    \n" +
            "]",                // response body
            Response.Status.OK,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(Collections.emptyList()).when(facade).getUpdates();
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testUpdateCodenvy() {
        testContract(
                "update",   // path
            ImmutableMap.of("step", "1"),    // query parameters
            null,                            // request body
            null,                            // consume content type
            null,                // produce content type
            HttpMethod.POST,                 // HTTP method
            null,                // response body
            Response.Status.ACCEPTED,              // response status
            new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
                        doReturn(null).when(configManager).prepareInstallProperties(anyString(),
                                                                                    any(InstallType.class),
                                                                                    any(Artifact.class),
                                                                                    any(Version.class),
                                                                                    anyBoolean());
                        doReturn("id").when(facade).update(any(Artifact.class), any(Version.class), any(InstallOptions.class));
                        doReturn(ImmutableList.of("a", "b")).when(facade).getUpdateInfo(any(Artifact.class), any(InstallType.class));
                        doReturn(Version.valueOf("1.0.0")).when(facade).getLatestInstallableVersion(any(Artifact.class));
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }

                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testLoginToCodenvySaaS() {
        testContract(
            "login",                                           // path
            null,                                              // query parameters
            "{\"username\": \"test\", \"password\": \"pwd\"}", // request body
            ContentType.JSON,                                  // consume content type
            null,                                              // produce content type
            HttpMethod.POST,                                   // HTTP method
            "",                                                // response body
            Response.Status.OK,                                // response status
            new Function<Object, Object>() {                   // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(new DtoServerImpls.TokenImpl().withValue("token"))
                                .when(facade)
                                .loginToCodenvySaaS(Commons.createDtoFromJson("{\"username\": \"test\", \"password\": \"pwd\"}", Credentials.class));

                        doReturn(new org.eclipse.che.api.account.server.dto.DtoServerImpls.AccountReferenceImpl().withId("id").withName("name"))
                                .when(facade).getAccountWhereUserIsOwner(anyString(), anyString());

                    } catch (Exception e) {
                        fail(e.getMessage(), e);
                    }

                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetOnPremisesSaasSubscription() {
        testContract(
            "subscription",                                    // path
            null,                                              // query parameters
            null,                                              // request body
            null,                                              // consume content type
            ContentType.JSON,                                  // produce content type
            HttpMethod.GET,                                    // HTTP method
            "{\n"
            + "    \"links\": [\n"
            + "        \n"
            + "    ],\n"
            + "    \"properties\": {\n"
            + "        \n"
            + "    }\n"
            + "}",                                             // response body
            Response.Status.OK,                                // response status
            new Function<Object, Object>() {                   // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        service.saasUserCredentials = new SaasUserCredentials("id", "token");

                        SubscriptionDescriptor descriptor = DtoFactory.getInstance().createDtoFromJson("{}", SubscriptionDescriptor.class);
                        doReturn(descriptor).when(facade).getSaasSubscription(anyString(), any(SaasUserCredentials.class));
                    } catch (Exception e) {
                        fail(e.getMessage(), e);
                    }

                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testAddTrialSubscription() {
        testContract(
            "subscription",                                    // path
            null,                                              // query parameters
            null,                                              // request body
            null,                                              // consume content type
            null,                                  // produce content type
            HttpMethod.POST,                                   // HTTP method
            null,                                  // response body
            Response.Status.CREATED,                                // response status
            new Function<Object, Object>() {                   // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        service.saasUserCredentials = new SaasUserCredentials("id", "token");
                        doNothing().when(facade).addTrialSaasSubscription(any(SaasUserCredentials.class));
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetStorageProperties() {
        testContract(
            "storage/properties",                              // path
            null,                                              // query parameters
            null,                                              // request body
            null,                                              // consume content type
            ContentType.JSON,                                  // produce content type
            HttpMethod.GET,                                    // HTTP method
            "{\n"
            + "    \"a\": \"b\"\n"
            + "}",                                             // response body
            Response.Status.OK,                                // response status
            new Function<Object, Object>() {                   // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn(ImmutableMap.of("a", "b")).when(facade).loadStorageProperties();
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testInsertProperties() {
        testContract(
            "storage/properties",                              // path
            null,                                              // query parameters
            "{\"a\":\"b\"}",                                   // request body
            ContentType.JSON,                                  // consume content type
            null,                                              // produce content type
            HttpMethod.POST,                                   // HTTP method
            "",                                                // response body
            Response.Status.OK,                                // response status
            null,                                              // before test
            new Function<Object, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).storeStorageProperties(ImmutableMap.of("a", "b"));
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            }                                                 // assertion
        );
    }

    @Test
    public void testGetStorageProperty() {
        testContract(
            "storage/properties/a",                           // path
            null,                                             // query parameters
            null,                                             // request body
            null,                                             // consume content type
            ContentType.TEXT,                                 // produce content type
            HttpMethod.GET,                                   // HTTP method
            "b",                                              // response body
            Response.Status.OK,                               // response status
            new Function<Object, Object>() {                  // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        doReturn("b").when(facade).loadStorageProperty("a");
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testUpdateStorageProperty() {
        testContract(
            "storage/properties/a",                           // path
            null,                                             // query parameters
            "b",                                              // request body
            ContentType.TEXT,                                 // consume content type
            null,                                             // produce content type
            HttpMethod.PUT,                                   // HTTP method
            "",                                               // response body
            Response.Status.OK,                               // response status
            null,                                             // before test
            new Function<Object, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).storeStorageProperty("a", "b");
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            }                                                 // assertion
        );
    }

    @Test
    public void testDeleteStorageProperty() {
        testContract(
            "storage/properties/a",                           // path
            null,                                             // query parameters
            null,                                             // request body
            null,                                             // consume content type
            null,                                             // produce content type
            HttpMethod.DELETE,                                // HTTP method
            null,                                             // response body
            Response.Status.NO_CONTENT,                       // response status
            null,                                             // before test
            new Function<Object, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).deleteStorageProperty("a");
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            }                                                 // assertion
        );
    }

    @Test
    public void testGetCodenvyProperties() {
        testContract(
            "codenvy/properties",                              // path
            null,                                              // query parameters
            null,                                              // request body
            null,                                              // consume content type
            ContentType.JSON,                                  // produce content type
            HttpMethod.GET,                                    // HTTP method
            "{\n"
            + "    \"a\": \"b\",\n"
            + "    \"password\": \"*****\"\n"
            + "}",                                             // response body
            Response.Status.OK,                                // response status
            new Function<Object, Object>() {                   // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        Config testConfig = new Config(ImmutableMap.of("a", "b", "password", "123"));
                        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testGetCodenvyProperty() {
        testContract(
            "codenvy/properties/host_url",                    // path
            null,                                             // query parameters
            null,                                             // request body
            null,                                             // consume content type
            ContentType.JSON,                                 // produce content type
            HttpMethod.GET,                                   // HTTP method
            "{\n" +
            "    \"host_url\": \"test.com\"\n" +
            "}",                                       // response body
            Response.Status.OK,                               // response status
            new Function<Object, Object>() {                  // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        Config testConfig = new Config(ImmutableMap.of("host_url", "test.com"));
                        doReturn(testConfig).when(configManager).loadInstalledCodenvyConfig();
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            },
            null // assertion
        );
    }

    @Test
    public void testUpdateCodenvyProperty() {
        testContract(
                "codenvy/properties",                           // path
            null,                                             // query parameters
            "{\"a\":\"b\"}",                                          // request body
            ContentType.JSON,                                 // consume content type
            null,                                             // produce content type
            HttpMethod.PUT,                                   // HTTP method
            null,                                               // response body
            Response.Status.CREATED,                               // response status
            null,                                             // before test
            new Function<Object, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).updateArtifactConfig(createArtifact(CDECArtifact.NAME), ImmutableMap.of("a", "b"));
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            }                                                 // assertion
        );
    }

    @Test
    public void testGetArtifactProperties() {
        testContract(
            "artifact/test/version/1.0.0/properties",            // path
            null,                                                // query parameters
            null,                                                // request body
            null,                                                // consume content type
            ContentType.JSON,                                    // produce content type
            HttpMethod.GET,                                      // HTTP method
            "{\n"
            + "    \"message\": \"Artifact 'test' not found\"\n"
            + "}",                                               // response body
            Response.Status.NOT_FOUND,                           // response status
            null,                                                // before test
            null                                                 // assertion
        );
    }

    @Test
    public void testGetInstallationManagerServerConfig() {
        testContract(
            "properties",                                         // path
             null,                                                // query parameters
             null,                                                // request body
             null,                                                // consume content type
             ContentType.JSON,                                    // produce content type
             HttpMethod.GET,                                      // HTTP method
             "{\n"
             + "    \"a\": \"b\"\n"
             + "}",                                               // response body
             Response.Status.OK,                                  // response status
             new Function<Object, Object>() {                     // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(ImmutableMap.of("a", "b")).when(facade).getInstallationManagerProperties();
                     return null;
                 }
             },
             null                                                 // assertion
        );
    }

    @Test
    public void testDeleteDownload() {
        testContract(
                "downloads/codenvy/1.0.0",       // path
            null,                                             // query parameters
            null,                                             // request body
            null,                                             // consume content type
            null,                                             // produce content type
            HttpMethod.DELETE,                                // HTTP method
            null,                                             // response body
            Response.Status.NO_CONTENT,                       // response status
            null,                                             // before test
            new Function<Object, Object>() {
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    try {
                        verify(facade).deleteDownloadedArtifact(createArtifact(CDECArtifact.NAME), Version.valueOf("1.0.0"));
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                    return null;
                }
            }                                                 // assertion
        );
    }

    @Test
    public void testGetUpdatesInfo() {
        testContract(
                "update/info",       // path
                null,                                             // query parameters
                null,                                             // request body
                null,                                             // consume content type
                ContentType.JSON,                                             // produce content type
                HttpMethod.GET,                                // HTTP method
                "[\n" +
                "    \n" +
                "]",                                             // response body
                Response.Status.OK,                       // response status
                null,                                             // before test
                new Function<Object, Object>() {
                    @Nullable
                    @Override
                    public Object apply(@Nullable Object o) {
                        try {
                            doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
                            doReturn(Collections.emptyList()).when(facade).getUpdateInfo(any(Artifact.class), any(InstallType.class));
                        } catch (IOException e) {
                            fail(e.getMessage(), e);
                        }
                        return null;
                    }
                }                                                 // assertion
                    );
    }

    private void testContract(String path,
                             Map<String, String> queryParameters,
                             String body,
                             ContentType consumeContentType,
                             ContentType produceContentType,
                             HttpMethod httpMethod,
                             String responseBody,
                             Response.Status responseStatus,
                             Function<Object, Object> beforeTest,
                             Function<Object, Object> assertion) {
        if (beforeTest != null) {
            beforeTest.apply(null);
        }

        RequestSpecification requestSpec = getRequestSpecification();
        com.jayway.restassured.response.Response response;

        if (queryParameters != null) {
            requestSpec.queryParameters(queryParameters);
        }

        if (body != null) {
            requestSpec.body(body);
        }

        if (consumeContentType != null) {
            requestSpec.contentType(consumeContentType);
        }

        switch (httpMethod) {
            case GET :
                response = requestSpec.get(getSecurePath(path));
                break;

            case PUT :
                response = requestSpec.put(getSecurePath(path));
                break;

            case POST :
                response = requestSpec.post(getSecurePath(path));
                break;

            case DELETE :
                response = requestSpec.delete(getSecurePath(path));
                break;

            default:
                throw new RuntimeException("Unknown HTTP method");
        }

        assertResponse(response, produceContentType, responseBody, responseStatus);

        if (assertion != null) {
            assertion.apply(null);
        }
    }

    private void assertResponse(com.jayway.restassured.response.Response response,
                                ContentType PRODUCE_CONTENT_TYPE,
                                String RESPONSE_BODY,
                                Response.Status RESPONSE_STATUS) {
        assertEquals(response.statusCode(), RESPONSE_STATUS.getStatusCode());

        if (PRODUCE_CONTENT_TYPE != null) {
            assertEquals(response.getContentType(), PRODUCE_CONTENT_TYPE.toString());
        }

        if (RESPONSE_BODY != null) {
            assertEquals(response.getBody().prettyPrint(), RESPONSE_BODY);
        }
    }

    private String getSecurePath(String path) {
        return format("%s/%s", JettyHttpServer.SECURE_PATH, path);
    }

    private RequestSpecification getRequestSpecification() {
        return given()
            .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
            .when();
    }

}

