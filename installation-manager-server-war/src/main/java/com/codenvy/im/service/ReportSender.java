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

import com.codenvy.im.artifacts.Artifact;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.codenvy.mail.MailSenderClient;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.schedule.ScheduleCron;

import javax.inject.Named;
import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codenvy.im.artifacts.UnknownArtifactVersionException.of;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.InjectorBootstrap.INJECTOR;

/**
 * Sends reports:
 * <li>to Codenvy with number of users.</li>
 *
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class ReportSender {
    private static final Logger LOG = Logger.getLogger(ReportSender.class.getSimpleName());

    private final LdapManager               ldapManager;
    private final MailSenderClient          mailClient;
    private final HttpTransport             httpTransport;
    private final String                    updateServerEndpoint;
    private final ConfigManager             configManager;
    private final InstallationManagerFacade facade;

    @Inject
    public ReportSender(@Named("installation-manager.update_server_endpoint") String updateServerEndpoint,
                        LdapManager ldapManager,
                        MailSenderClient mailClient,
                        HttpTransport httpTransport,
                        ConfigManager configManager,
                        InstallationManagerFacade facade) {
        this.ldapManager = ldapManager;
        this.mailClient = mailClient;
        this.httpTransport = httpTransport;
        this.updateServerEndpoint = updateServerEndpoint;
        this.configManager = configManager;
        this.facade = facade;
    }

    @ScheduleCron(cron = "0 0 1 ? * SUN *")  // send each Sunday at 1:00 AM. Use "0 0/1 * 1/1 * ? *" to send every 1 minute.
    public void sendWeeklyReports() {
        try {
            Artifact cdecArtifact = getCdecArtifact();
            Version version = cdecArtifact.getInstalledVersion().orElseThrow(() -> of(cdecArtifact));

            if (version.is4Major()) {
                sendNumberOfUsers();
            }
        } catch (JsonParseException | IOException | MessagingException | UnknownArtifactVersionException | ApiException e) {
            LOG.log(Level.SEVERE, "Error of sending weekly reports.", e);
        }
    }

    private void sendNumberOfUsers() throws IOException, MessagingException, JsonParseException, ApiException {
        try {
            CodenvyLicense codenvyLicense = facade.loadCodenvyLicense();
            if (!codenvyLicense.isExpired()) {
                // don't send report if Codenvy License is valid and isn't expired
                LOG.log(Level.INFO, "Codenvy License is valid and isn't expired.");
                return;
            }
        } catch (LicenseException e) {
            // just log info without interrupting sending report if there is a problem with Codenvy License
            LOG.log(Level.INFO, "There is a problem with Codenvy License.", e);
        }

        ReportParameters parameters = obtainReportParameters(ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT);

        String externalIP = obtainExternalIP();

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("External IP address: %s\n", externalIP));
        msg.append(String.format("Hostname: %s\n", configManager.loadInstalledCodenvyConfig().getValue(Config.HOST_URL)));
        msg.append(String.format("Number of users: %s\n", ldapManager.getNumberOfUsers()));

        mailClient.sendMail(parameters.getSender(), parameters.getReceiver(), null, parameters.getTitle(), MediaType.TEXT_PLAIN, msg.toString());
    }

    private String obtainExternalIP() throws IOException {
        String requestUrl = combinePaths(updateServerEndpoint, "/util/client-ip");
        return httpTransport.doGetTextPlain(requestUrl);
    }

    private ReportParameters obtainReportParameters(ReportType reportType) throws IOException, JsonParseException {
        String requestUrl = combinePaths(updateServerEndpoint, "/report/parameters/" + reportType.name().toLowerCase());

        String response = httpTransport.doGet(requestUrl);
        return Commons.fromJson(response, ReportParameters.class);
    }

    /**
     * has protected access for the testing propose
     */
    protected Artifact getCdecArtifact() {
        return INJECTOR.getInstance(CDECArtifact.class);
    }
}
