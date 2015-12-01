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
package com.codenvy.im.update;

import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Utils API.
 *
 * @author Dmytro Nochevnov
 */
@Path("util")
public class UtilService {

    static Logger LOG = LoggerFactory.getLogger(UtilService.class);

    @Inject
    public UtilService() {
    }

    /** Log event. */
    @GenerateLink(rel = "return client's external IP")
    @GET
    @Path("/client-ip")
    public Response getClientIp(@Context HttpServletRequest requestContext) {
        try {
            String clientIp = requestContext.getRemoteAddr();
            return Response.status(Response.Status.OK).entity(clientIp).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage()).build();
        }
    }

}
