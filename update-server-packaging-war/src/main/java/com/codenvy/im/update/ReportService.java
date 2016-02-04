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
package com.codenvy.im.update;

import com.codenvy.im.report.ReportParameters;
import com.codenvy.im.report.ReportType;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Utils API.
 *
 * @author Dmytro Nochevnov
 */
@Path("report")
public class ReportService {

    static Logger LOG = LoggerFactory.getLogger(ReportService.class);

    @Inject
    public ReportService() {
    }

    /** Get parameters of certain report. */
    @GenerateLink(rel = "return certain report parameters")
    @GET
    @Path("/parameters/{report_type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReportParameters(@PathParam("report_type") String reportType) {
        try {
            if (reportType == null || ReportType.valueOf(reportType.toUpperCase()) == null) {
                throw new RuntimeException("Report type is unknown.");
            }

            ReportParameters parameters = ReportType.valueOf(reportType.toUpperCase()).getParameters();
            return Response.ok(parameters).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unexpected error. " + e.getMessage()).build();
        }
    }

}
