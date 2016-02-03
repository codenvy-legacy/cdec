/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
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

import com.codenvy.im.exceptions.InvalidLicenseException;
import com.codenvy.im.exceptions.LicenseException;
import com.codenvy.im.exceptions.LicenseNotFoundException;
import com.codenvy.im.facade.IMCliFilteredFacade;
import com.codenvy.im.managers.CodenvyLicenseManager;
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

import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class LicenseServiceTest {

    @SuppressWarnings("unused")
    protected static ApiExceptionMapper MAPPER = new ApiExceptionMapper();
    @Mock
    private IMCliFilteredFacade facade;

    LicenseService licenseService;

    @BeforeMethod
    public void setUp() throws Exception {
        licenseService = spy(new LicenseService(facade));
    }

    @Test
    public void testGetLicenseShouldReturnOk() throws Exception {
        doReturn("license").when(facade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());
        assertEquals(response.asString(), "license");
    }

    @Test
    public void testGetLicenseShouldReturnNotFoundWhenFacadeThrowLicenseNotFoundException() throws Exception {
        doThrow(new LicenseNotFoundException("error")).when(facade).loadCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetLicenseShouldReturnServerErrorWhenFacadeThrowLicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(facade).loadCodenvyLicense();

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
        doThrow(new LicenseException("error")).when(facade).deleteCodenvyLicense();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .delete(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testPostLicenseShouldReturnCreated() throws Exception {
        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().body("license")
                .post(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.CREATED.getStatusCode());
        verify(facade).storeCodenvyLicense("license");
    }

    @Test
    public void testPostLicenseShouldReturnConflictWhenFacadeThrowInvalidLicenseException() throws Exception {
        doThrow(new InvalidLicenseException("error")).when(facade).storeCodenvyLicense("license");

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().body("license")
                .post(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testPostLicenseShouldReturnServerErrorWhenFacadeThrowLicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(facade).storeCodenvyLicense("license");

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().body("license")
                .post(JettyHttpServer.SECURE_PATH + "/license");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testGetLicensePropertiesShouldReturnOk() throws Exception {
        doReturn(ImmutableMap.of(CodenvyLicenseManager.LicenseFeature.LICENSE_TYPE, "type",
                                 CodenvyLicenseManager.LicenseFeature.EXPIRATION_DATE, "2015/10/10",
                                 CodenvyLicenseManager.LicenseFeature.NUMBER_OF_USERS, "15")).when(facade).getCodenvyLicenseFeatures();
        doReturn(Boolean.FALSE).when(facade).isLicenseExpired();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.OK.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, String> m = response.as(Map.class);

        assertEquals(m.size(), 4);
        assertEquals(m.get(CodenvyLicenseManager.LicenseFeature.LICENSE_TYPE.toString()), "type");
        assertEquals(m.get(CodenvyLicenseManager.LicenseFeature.EXPIRATION_DATE.toString()), "2015/10/10");
        assertEquals(m.get(CodenvyLicenseManager.LicenseFeature.NUMBER_OF_USERS.toString()), "15");
        assertEquals(m.get(LicenseService.CODENVY_LICENSE_PROPERTY_IS_EXPIRED), "false");
    }

    @Test
    public void testGetLicensePropertiesShouldReturnNotFoundWhenFacadeThrowLicenseNotFoundException() throws Exception {
        doThrow(new LicenseNotFoundException("error")).when(facade).getCodenvyLicenseFeatures();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetLicensePropertiesShouldReturnConflictWhenFacadeThrowInvalidLicenseException() throws Exception {
        doThrow(new InvalidLicenseException("error")).when(facade).getCodenvyLicenseFeatures();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    public void testGetLicensePropertiesShouldReturnServerErrorWhenFacadeThrowILicenseException() throws Exception {
        doThrow(new LicenseException("error")).when(facade).getCodenvyLicenseFeatures();

        Response response = given()
                .auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when()
                .get(JettyHttpServer.SECURE_PATH + "/license/properties");

        assertEquals(response.statusCode(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
