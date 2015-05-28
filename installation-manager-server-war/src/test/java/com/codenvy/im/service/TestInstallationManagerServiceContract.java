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

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.ArtifactProperties;
import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.managers.BackupConfig;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.InstallOptions;
import com.codenvy.im.managers.InstallType;
import com.codenvy.im.request.Request;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.Version;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class TestInstallationManagerServiceContract {
    public static final String OK_RESPONSE_BODY = "{\n"
                                                  + "    \n"
                                                  + "}";
    @Mock
    public InstallationManagerFacade facade;
    @Mock
    public ConfigManager       configManager;
    @Mock
    public SaasUserCredentials saasUserCredentials;
    @Mock
    private Artifact                  mockArtifact;

    public InstallationManagerService service;

    enum HttpMethod {GET, POST, PUT, DELETE}

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        service = new InstallationManagerService(facade, configManager);
    }

    @Test(dataProvider = "imApiTestData")
    public void testContract(String path,
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
        com.jayway.restassured.response.Response response = null;

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

    @DataProvider(name ="imApiTestData")
    public Object[][] GetImApiTestData() {
        return new Object[][]{
            // backup()
            {"backup",                                     // path
             ImmutableMap.of("artifact", CDECArtifact.NAME,
                             "backupDir", "test"),         // query parameters
             null,                                         // request body
             null,                                         // consume content type
             ContentType.JSON,                             // produce content type
             HttpMethod.POST,                              // HTTP method
             OK_RESPONSE_BODY,                             // response body
             Response.Status.OK,                           // response status
             new Function<Object, Object>() {              // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                                       .setBackupDirectory("test");
                     doReturn(com.codenvy.im.response.Response.ok()).when(facade).backup(testBackupConfig);
                     return null;
                 }
             },
             null // assertion
            },

            // restore()
            {"restore",                                    // path
             ImmutableMap.of("artifact", CDECArtifact.NAME,
                             "backupFile", "test"),        // query parameters
             null,                                         // request body
             null,                                         // consume content type
             ContentType.JSON,                             // produce content type
             HttpMethod.POST,                              // HTTP method
             OK_RESPONSE_BODY,                             // response body
             Response.Status.OK,                           // response status
             new Function<Object, Object>() {              // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     BackupConfig testBackupConfig = new BackupConfig().setArtifactName(CDECArtifact.NAME)
                                                                       .setBackupFile("test");
                     doReturn(com.codenvy.im.response.Response.ok()).when(facade).restore(testBackupConfig);
                     return null;
                 }
             },
             null // assertion
            },

            // addNode()
            {"node",                          // path
             ImmutableMap.of("dns", "test"),  // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.POST,                 // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(com.codenvy.im.response.Response.ok()).when(facade).addNode("test");
                     return null;
                 }
             },
             null // assertion
            },

            // removeNode()
            {"node",                          // path
             ImmutableMap.of("dns", "test"),  // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.DELETE,               // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                @Nullable
                @Override
                public Object apply(@Nullable Object o) {
                    doReturn(com.codenvy.im.response.Response.ok()).when(facade).removeNode("test");
                    return null;
                }
             },
             null // assertion
            },

            // getDownloads()
            {"download",                      // path
             ImmutableMap.of("artifact", CDECArtifact.NAME),  // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.GET,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     Request testRequest = new Request().setArtifactName(CDECArtifact.NAME);
                     doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).getDownloads(testRequest);
                     return null;
                 }
             },
             null // assertion
            },

            // startDownload()
            {"download/start",                      // path
             ImmutableMap.of("artifact", CDECArtifact.NAME,
                             "version", "1.0.0"),  // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.POST,                 // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     Request testRequest = new Request().setArtifactName(CDECArtifact.NAME)
                                                        .setVersion("1.0.0");
                     doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).startDownload(testRequest);
                     return null;
                 }
             },
             null // assertion
            },

            // stopDownload()
            {"download/stop",                 // path
             null,                            // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.POST,                 // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).stopDownload();
                     return null;
                 }
             },
             null // assertion
            },

            // getDownloadStatus()
            {"download/status",               // path
             null,                            // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.GET,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).getDownloadStatus();
                     return null;
                 }
             },
             null // assertion
            },

            // getUpdateCodenvyInfo()
            {"update/" + CDECArtifact.NAME + "/info",               // path
             null,                            // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.GET,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();

                     try {
                         InstallOptions testInstallOptions = new InstallOptions().setInstallType(InstallType.SINGLE_SERVER);
                         Request testRequest = new Request().setArtifactName(CDECArtifact.NAME)
                                                            .setInstallOptions(testInstallOptions);

                         doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).getInstallInfo(testRequest);
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             },
             null // assertion
            },

            // getInstalledVersions()
            {"installation",                  // path
             null,                            // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.GET,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     try {
                         doReturn(com.codenvy.im.response.Response.ok()).when(facade).getInstalledVersions();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             },
             null // assertion
            },

            // getUpdates()
            {"update",                        // path
             null,                            // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.GET,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).getUpdates();
                     return null;
                 }
             },
             null // assertion
            },

            // updateCodenvy()
            {"update/" + CDECArtifact.NAME,   // path
             ImmutableMap.of("step", 1), // query parameters
             null,                            // request body
             null,                            // consume content type
             ContentType.JSON,                // produce content type
             HttpMethod.POST,                  // HTTP method
             OK_RESPONSE_BODY,                // response body
             Response.Status.OK,              // response status
             new Function<Object, Object>() { // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     try {
                         doReturn(InstallType.SINGLE_SERVER).when(configManager).detectInstallationType();
                         doReturn(null).when(configManager).prepareInstallProperties(anyString(), any(InstallType.class), any(Artifact.class), any(Version.class));
                         doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).install(any(Request.class));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }

                     return null;
                 }
             },
             null // assertion
            },

            // loginToCodenvySaaS()
            {"login",                                           // path
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
                             .when(facade).getAccountWhereUserIsOwner(anyString(), any(Request.class));

                     } catch (Exception e) {
                         e.printStackTrace();
                     }

                     return null;
                 }
             },
             null // assertion
            },

            // getOnPremisesSaasSubscription()
            {"subscription",                                    // path
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
                         doReturn(descriptor).when(facade).getSubscription(anyString(), any(Request.class));
                     } catch (Exception e) {
                         e.printStackTrace();
                     }

                     return null;
                 }
             },
             null // assertion
            },

            // addTrialSubscription()
            {"subscription",                                    // path
             null,                                              // query parameters
             null,                                              // request body
             null,                                              // consume content type
             ContentType.JSON,                                  // produce content type
             HttpMethod.POST,                                   // HTTP method
             OK_RESPONSE_BODY,                                  // response body
             Response.Status.OK,                                // response status
             new Function<Object, Object>() {                   // before test
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     try {
                         service.saasUserCredentials = new SaasUserCredentials("id", "token");
                         doReturn(com.codenvy.im.response.Response.ok().toJson()).when(facade).addTrialSaasSubscription(any(Request.class));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             },
             null // assertion
            },

            // getProperties()
            {"storage/properties",                              // path
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
                         doReturn(ImmutableMap.of("a", "b")).when(facade).loadProperties();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             },
             null // assertion
            },

            // insertProperties()
            {"storage/properties",                              // path
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
                         verify(facade).storeProperties(ImmutableMap.of("a", "b"));
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             }                                                 // assertion
            },

            // getProperty()
            {"storage/properties/a",                           // path
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
                         doReturn("b").when(facade).loadProperty("a");
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             },
             null // assertion
            },

            // updateProperty()
            {"storage/properties/a",                           // path
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
                         verify(facade).storeProperty("a", "b");
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             }                                                 // assertion
            },

            // deleteProperty()
            {"storage/properties/a",                           // path
             null,                                             // query parameters
             "b",                                              // request body
             null,                                             // consume content type
             null,                                             // produce content type
             HttpMethod.DELETE,                                // HTTP method
             "",                                               // response body
             Response.Status.NO_CONTENT,                       // response status
             null,                                             // before test
             new Function<Object, Object>() {
                 @Nullable
                 @Override
                 public Object apply(@Nullable Object o) {
                     try {
                         verify(facade).deleteProperty("a");
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     return null;
                 }
             }                                                 // assertion
            },

            // getArtifactProperties()
            {"artifact/test/version/1.0.0/properties",            // path
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
            },

            // getInstallationManagerServerConfig()
            {"properties",                                        // path
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
            },
        };
    }

    private void assertResponse(com.jayway.restassured.response.Response response,
                                ContentType PRODUCE_CONTENT_TYPE,
                                String RESPONSE_BODY,
                                Response.Status RESPONSE_STATUS) {
        assertEquals(response.statusCode(), RESPONSE_STATUS.getStatusCode());

        if (PRODUCE_CONTENT_TYPE != null) {
            assertEquals(response.getContentType(), PRODUCE_CONTENT_TYPE.toString());
        }

        assertEquals(response.getBody().prettyPrint(), RESPONSE_BODY);
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

