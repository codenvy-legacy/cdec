/*
 *  [2012] - [2016] Codenvy, S.A.
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

import com.codenvy.im.facade.IMCliFilteredFacade;
import com.codenvy.im.license.CodenvyLicense;
import com.codenvy.im.license.CodenvyLicenseFactory;
import com.codenvy.im.license.InvalidLicenseException;
import com.codenvy.im.license.LicenseException;
import com.codenvy.im.license.LicenseFeature;
import com.codenvy.im.license.LicenseNotFoundException;
import com.codenvy.im.managers.Config;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class LicenseServiceTest {

    @SuppressWarnings("unused")
    protected static ApiExceptionMapper MAPPER = new ApiExceptionMapper();
    @Mock
    private IMCliFilteredFacade   mockFacade;
    @Mock
    private CodenvyLicense        mockCodenvyLicense;
    @Mock
    private CodenvyLicenseFactory mockLicenseFactory;

    LicenseService licenseService;

    @BeforeMethod
    public void setUp() throws Exception {
        licenseService = spy(new LicenseService(mockFacade, mockLicenseFactory));
    }

    @Test
    public void testGetLicenseShouldReturnOk() throws Exception {
        doReturn(mockCodenvyLicense).when(mockFacade).loadCodenvyLicense();
        doReturn("license").when(mockCodenvyLicense).getLicenseText();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "license");
    }

    @Test
    public void testGetLicenseShouldReturnNotFoundWhenFacadeThrowLicenseNotFoundException() throws Exception {
        doThrow(new LicenseNotFoundException("error")).when(mockFacade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetLicenseShouldReturnServerErrorWhenFacadeThrowLicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(mockFacade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    @Test
    public void testDeleteLicenseShouldReturnOk() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .delete(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.ACCEPTED.getStatusCode());
    }

    @Test
    public void testDeleteLicenseShouldReturnServerErrorWhenFacadeThrowLicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(mockFacade).deleteCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .delete(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testPostLicenseShouldReturnCreated() throws Exception {
        doReturn(mockCodenvyLicense).when(mockLicenseFactory).create(anyString());

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().body("license")
                .post(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.CREATED.getStatusCode());
        verify(mockFacade).storeCodenvyLicense(any(CodenvyLicense.class));
    }

    @Test
    public void testPostLicenseShouldReturnServerErrorWhenFacadeThrowLicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(mockFacade).storeCodenvyLicense(any(CodenvyLicense.class));

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().body("license")
                .post(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetLicensePropertiesShouldReturnOk() throws Exception {
        doReturn(mockCodenvyLicense).when(mockFacade).loadCodenvyLicense();
        doReturn(ImmutableMap.of(LicenseFeature.TYPE, "type",
                                 LicenseFeature.EXPIRATION, "2015/10/10",
                                 LicenseFeature.USERS, "15")).when(mockCodenvyLicense).getFeatures();
        doReturn(Boolean.FALSE).when(mockCodenvyLicense).isExpired();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, String> m = response.as(Map.class);

        assertEquals(m.size(), 4);
        assertEquals(m.get(LicenseFeature.TYPE.toString()), "type");
        assertEquals(m.get(LicenseFeature.EXPIRATION.toString()), "2015/10/10");
        assertEquals(m.get(LicenseFeature.USERS.toString()), "15");
        assertEquals(m.get(LicenseService.CODENVY_LICENSE_PROPERTY_IS_EXPIRED), "false");
    }

    @Test
    public void testGetLicensePropertiesShouldReturnNotFoundWhenLicenseNotFound() throws Exception {
        doThrow(new LicenseNotFoundException("error")).when(mockFacade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetLicensePropertiesShouldReturnConflictWhenLicenseInvalid() throws Exception {
        doThrow(new InvalidLicenseException("error")).when(mockFacade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testIsCodenvyLicenseUsageLegal() throws IOException {
        doReturn(true).when(mockCodenvyLicense).isLicenseUsageLegal(3, 2);
        doReturn(mockCodenvyLicense).when(mockFacade).loadCodenvyLicense();

        setSizeOfAdditionalNodes(2);
        doReturn(3L).when(mockFacade).getNumberOfUsers();

        Response response = given().when().get("/license/legality");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "{\"value\":\"true\"}");
    }

    @Test
    public void testIsCodenvyFreeUsageLegal() throws IOException {
        doThrow(LicenseNotFoundException.class).when(mockFacade).loadCodenvyLicense();

        setSizeOfAdditionalNodes(CodenvyLicense.MAX_NUMBER_OF_FREE_SERVERS);
        doReturn(CodenvyLicense.MAX_NUMBER_OF_FREE_USERS).when(mockFacade).getNumberOfUsers();

        Response response = given().when().get("/license/legality");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "{\"value\":\"true\"}");
    }

    @Test
    public void testIsCodenvyLicenseUsageNotLegal() throws IOException {
        doReturn(false).when(mockCodenvyLicense).isLicenseUsageLegal(anyLong(), anyInt());
        doReturn(mockCodenvyLicense).when(mockFacade).loadCodenvyLicense();

        Response response = given().when().get("/license/legality");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "{\"value\":\"false\"}");
    }

    @Test
    public void testIsCodenvyFreeUsageNotLegal() throws IOException {
        doThrow(LicenseException.class).when(mockFacade).loadCodenvyLicense();

        setSizeOfAdditionalNodes(CodenvyLicense.MAX_NUMBER_OF_FREE_SERVERS + 1);
        doReturn(CodenvyLicense.MAX_NUMBER_OF_FREE_USERS + 1).when(mockFacade).getNumberOfUsers();

        Response response = given().when().get("/license/legality");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "{\"value\":\"false\"}");
    }

    @Test
    public void testIsCodenvyUsageIOException() throws IOException {
        doThrow(IOException.class).when(mockFacade).getNumberOfUsers();

        Response response = given().when().get("/license/legality");
        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void setSizeOfAdditionalNodes(int size) throws IOException {
        Map<String, List<String>> mockNodes = mock(Map.class);
        List<String> mockList = mock(List.class);
        doReturn(mockList).when(mockNodes).get(Config.SWARM_NODES);
        doReturn(size).when(mockList).size();

        doReturn(mockNodes).when(mockFacade).getNodes();
    }

}
