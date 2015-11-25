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
    ReportParameters testReportParameters = new ReportParameters("test title", "test@sender", "test@receiver", true);

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