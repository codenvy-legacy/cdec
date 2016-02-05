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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.codenvy.im.license.CodenvyLicense;
import com.codenvy.im.license.CodenvyLicenseFactory;
import com.codenvy.im.license.InvalidLicenseException;
import com.codenvy.im.license.LicenseException;
import com.codenvy.im.license.LicenseFeature;
import com.codenvy.im.license.LicenseNotFoundException;
import com.codenvy.im.facade.IMCliFilteredFacade;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.google.inject.Inject;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

/**
 * @author Anatoliy Bazko
 */
@Path("/license")
@RolesAllowed({"system/admin"})
@Api(value = "license", description = "License manager")
public class LicenseService {
    private static final Logger LOG                                 = LoggerFactory.getLogger(LicenseService.class);
    public static final  String CODENVY_LICENSE_PROPERTY_IS_EXPIRED = "isExpired";

    private final InstallationManagerFacade delegate;
    private final CodenvyLicenseFactory licenseFactory;

    @Inject
    public LicenseService(IMCliFilteredFacade delegate, CodenvyLicenseFactory licenseFactory) {
        this.delegate = delegate;
        this.licenseFactory = licenseFactory;
    }

    @DELETE
    @ApiOperation(value = "Deletes Codenvy license")
    @ApiResponses(value = {@ApiResponse(code = 202, message = "OK"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response deleteLicense() throws ApiException {
        try {
            delegate.deleteCodenvyLicense();
            return status(ACCEPTED).build();
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Loads Codenvy license")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 404, message = "License not found"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response loadLicense() throws ApiException {
        try {
            CodenvyLicense codenvyLicense = delegate.loadCodenvyLicense();
            return status(OK).entity(codenvyLicense.getLicenseText()).build();
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Stores valid Codenvy license in the system")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "OK"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response storeLicense(String license) throws ApiException {
        try {
            CodenvyLicense codenvyLicense = licenseFactory.create(license);
            delegate.storeCodenvyLicense(codenvyLicense);
            return status(CREATED).build();
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Loads Codenvy license properties")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 404, message = "License not found"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 500, message = "Server error")})
    @Path("/properties")
    public Response getLicenseProperties() throws ApiException {
        try {
            CodenvyLicense codenvyLicense = delegate.loadCodenvyLicense();
            Map<LicenseFeature, String> features = codenvyLicense.getFeatures();

            Map<String, String> properties = features
                    .entrySet()
                    .stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey().toString(), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            boolean licenseExpired = codenvyLicense.isExpired();
            properties.put(CODENVY_LICENSE_PROPERTY_IS_EXPIRED, String.valueOf(licenseExpired));

            return status(OK).entity(new JsonStringMapImpl<>(properties)).build();
        } catch (LicenseNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }
}
