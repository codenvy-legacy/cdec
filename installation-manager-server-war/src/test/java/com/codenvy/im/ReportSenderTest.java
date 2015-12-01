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
package com.codenvy.im;

import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.LdapManager;
import com.codenvy.im.report.ReportParameters;
import com.codenvy.im.report.ReportType;
import com.codenvy.im.service.ReportSender;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.google.common.collect.ImmutableMap;
import org.codenvy.mail.MailSenderClient;
import org.eclipse.che.commons.json.JsonParseException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Nochevnov
 */
public class ReportSenderTest {
    public static final String TEST_TITLE = "title";
    public static final String TEST_SENDER = "test@sender";
    public static final String TEST_RECEIVER = "test@receiver";
    private ReportSender spyReportSender;

    @Mock
    private LdapManager mockLdapManager;

    @Mock
    private MailSenderClient mockMailClient;

    @Mock
    private HttpTransport mockHttpTransport;

    @Mock
    private ConfigManager mockConfigManager;

    private String updateServerEndpoint = "update/endpoint";

    @BeforeMethod
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyReportSender = new ReportSender(updateServerEndpoint, mockLdapManager, mockMailClient, mockHttpTransport, mockConfigManager);

        Config testConfig = new Config(ImmutableMap.of(Config.HOST_URL, "codenvy-host"));
        doReturn(testConfig).when(mockConfigManager).loadInstalledCodenvyConfig();
    }

    @Test
    public void shouldSendActiveWeeklyReports() throws IOException, JsonParseException, MessagingException {
        ReportParameters reportParameters = new ReportParameters(TEST_TITLE, TEST_SENDER, TEST_RECEIVER, true);
        doReturn(Commons.toJson(reportParameters)).when(mockHttpTransport).doPost(combinePaths(updateServerEndpoint, "/parameters/" + ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT.name().toLowerCase()), null);
        doReturn("192.168.1.1").when(mockHttpTransport).doGet(combinePaths(updateServerEndpoint, "/util/client-ip"));

        doReturn(150L).when(mockLdapManager).getNumberOfUsers();

        spyReportSender.sendWeeklyReports();

        verify(mockMailClient).sendMail(TEST_SENDER, TEST_RECEIVER, null, TEST_TITLE, MediaType.TEXT_PLAIN, "External IP address: 192.168.1.1\n"
                                                                                                            + "Hostname: codenvy-host\n"
                                                                                                            + "Number of users: 150\n");
    }

    @Test
    public void shouldNotSendInactiveWeeklyReports() throws IOException, JsonParseException, MessagingException {
        ReportParameters reportParameters = new ReportParameters(TEST_TITLE, TEST_SENDER, TEST_RECEIVER, false);

        doReturn(Commons.toJson(reportParameters)).when(mockHttpTransport).doPost(combinePaths(updateServerEndpoint, "/parameters/" + ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT.name().toLowerCase()), null);
        spyReportSender.sendWeeklyReports();

        verify(mockMailClient, never()).sendMail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockLdapManager, never()).getNumberOfUsers();
        verify(mockConfigManager, never()).loadInstalledCodenvyConfig();
        verify(mockHttpTransport, never()).doGet(combinePaths(updateServerEndpoint, "/util/client-ip"));
    }
}
