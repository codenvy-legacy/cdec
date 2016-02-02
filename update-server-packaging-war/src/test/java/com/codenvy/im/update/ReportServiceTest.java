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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class ReportServiceTest {
    public static final String TEST_REPORT_TYPE = ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT.name();

    private ReportService testReportService;

    /** @see {update-server-packaging-war/src/test/resourses/report.properties} */
    ReportParameters testReportParameters = new ReportParameters("test title", "test@sender", "test@receiver");

    @BeforeMethod
    public void setup() {
        testReportService = new ReportService();
    }

    @Test
    public void shouldReturnReportParameters() {
        Response response = testReportService.getReportParameters(TEST_REPORT_TYPE);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(response.getEntity(), testReportParameters);
    }

    @Test
    public void shouldReturnErrorWhenGettingErrorParametersFailed() {
        Response response = testReportService.getReportParameters(null);
        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertEquals(response.getEntity(), "Unexpected error. Report type is unknown.");
    }

}