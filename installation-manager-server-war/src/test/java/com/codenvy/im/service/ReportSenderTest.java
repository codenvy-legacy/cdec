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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.artifacts.UnknownArtifactVersionException;
import com.codenvy.im.facade.InstallationManagerFacade;
import com.codenvy.im.license.CodenvyLicense;
import com.codenvy.im.license.LicenseException;
import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.LdapManager;
import com.codenvy.im.report.ReportParameters;
import com.codenvy.im.report.ReportType;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
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
import java.util.Optional;

import static com.codenvy.im.utils.Commons.combinePaths;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Nochevnov
 */
public class ReportSenderTest {
    public static final String            TEST_TITLE    = "title";
    public static final String            TEST_SENDER   = "test@sender";
    public static final String            TEST_RECEIVER = "test@receiver";
    public static final Optional<Version> VERSION_4_0_0 = Optional.of(Version.valueOf("4.0.0"));
    public static final Optional<Version> VERSION_3_0_0 = Optional.of(Version.valueOf("3.0.0"));

    private ReportSender spyReportSender;

    @Mock
    private LdapManager mockLdapManager;
    @Mock
    private MailSenderClient mockMailClient;
    @Mock
    private HttpTransport mockHttpTransport;
    @Mock
    private ConfigManager mockConfigManager;
    @Mock
    private CDECArtifact mockCdecArtifact;
    @Mock
    private InstallationManagerFacade mockFacade;
    @Mock
    private CodenvyLicense codenvyLicense;

    private String updateServerEndpoint = "update/endpoint";

    @BeforeMethod
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyReportSender = spy(new ReportSender(updateServerEndpoint, mockLdapManager, mockMailClient, mockHttpTransport, mockConfigManager, mockFacade));

        doReturn(mockCdecArtifact).when(spyReportSender).getCdecArtifact();

        Config testConfig = new Config(ImmutableMap.of(Config.HOST_URL, "codenvy-host"));
        doReturn(testConfig).when(mockConfigManager).loadInstalledCodenvyConfig();
    }

    @Test
    public void shouldSendWeeklyReportBecauseOfExpiredLicense() throws IOException, JsonParseException, MessagingException {
        ReportParameters reportParameters = new ReportParameters(TEST_TITLE, TEST_SENDER, TEST_RECEIVER);
        doReturn(Commons.toJson(reportParameters)).when(mockHttpTransport)
                                                  .doGet(combinePaths(updateServerEndpoint, "/report/parameters/" + ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT.name().toLowerCase()));
        doReturn("192.168.1.1").when(mockHttpTransport).doGetTextPlain(combinePaths(updateServerEndpoint, "/util/client-ip"));

        doReturn(150L).when(mockLdapManager).getNumberOfUsers();
        doReturn(VERSION_4_0_0).when(mockCdecArtifact).getInstalledVersion();
        doReturn(codenvyLicense).when(mockFacade).loadCodenvyLicense();
        doReturn(true).when(codenvyLicense).isExpired();

        spyReportSender.sendWeeklyReports();

        verify(mockMailClient).sendMail(TEST_SENDER, TEST_RECEIVER, null, TEST_TITLE, MediaType.TEXT_PLAIN, "External IP address: 192.168.1.1\n"
                                                                                                            + "Hostname: codenvy-host\n"
                                                                                                            + "Number of users: 150\n");
    }

    @Test
    public void shouldSendWeeklyReportBecauseOfLicenseException() throws IOException, JsonParseException, MessagingException {
        ReportParameters reportParameters = new ReportParameters(TEST_TITLE, TEST_SENDER, TEST_RECEIVER);
        doReturn(Commons.toJson(reportParameters)).when(mockHttpTransport)
                                                  .doGet(combinePaths(updateServerEndpoint, "/report/parameters/" + ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT.name().toLowerCase()));
        doReturn("192.168.1.1").when(mockHttpTransport).doGetTextPlain(combinePaths(updateServerEndpoint, "/util/client-ip"));

        doReturn(150L).when(mockLdapManager).getNumberOfUsers();
        doReturn(VERSION_4_0_0).when(mockCdecArtifact).getInstalledVersion();
        doThrow(LicenseException.class).when(mockFacade).loadCodenvyLicense();

        spyReportSender.sendWeeklyReports();

        verify(mockMailClient).sendMail(TEST_SENDER, TEST_RECEIVER, null, TEST_TITLE, MediaType.TEXT_PLAIN, "External IP address: 192.168.1.1\n"
                                                                                                            + "Hostname: codenvy-host\n"
                                                                                                            + "Number of users: 150\n");
    }

    @Test
    public void shouldNotSendWeeklyReportBecauseOfNonExpiredLicense() throws IOException, JsonParseException, MessagingException {
        doReturn(VERSION_4_0_0).when(mockCdecArtifact).getInstalledVersion();
        doReturn(codenvyLicense).when(mockFacade).loadCodenvyLicense();
        doReturn(false).when(codenvyLicense).isExpired();

        spyReportSender.sendWeeklyReports();

        verify(mockMailClient, never()).sendMail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockLdapManager, never()).getNumberOfUsers();
        verify(mockConfigManager, never()).loadInstalledCodenvyConfig();
        verify(mockHttpTransport, never()).doGetTextPlain(combinePaths(updateServerEndpoint, "/util/client-ip"));
        verify(mockHttpTransport, never()).doGet(combinePaths(updateServerEndpoint, "/report/parameters"));
    }

    @Test
    public void shouldNotSendWeeklyReportInCodenvy3() throws IOException, JsonParseException, MessagingException {
        doReturn(VERSION_3_0_0).when(mockCdecArtifact).getInstalledVersion();

        spyReportSender.sendWeeklyReports();
        verify(mockMailClient, never()).sendMail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockLdapManager, never()).getNumberOfUsers();
    }

    @Test
    public void shouldNotSendWeeklyReportBecauseOfUnknownVersion() throws IOException, JsonParseException, MessagingException {
        doThrow(UnknownArtifactVersionException.class).when(mockCdecArtifact).getInstalledVersion();

        spyReportSender.sendWeeklyReports();
        verify(mockMailClient, never()).sendMail(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockLdapManager, never()).getNumberOfUsers();
    }
}
