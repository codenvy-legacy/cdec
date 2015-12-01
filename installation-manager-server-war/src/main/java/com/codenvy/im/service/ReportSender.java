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
package com.codenvy.im.service;

import com.codenvy.im.managers.Config;
import com.codenvy.im.managers.ConfigManager;
import com.codenvy.im.managers.LdapManager;
import com.codenvy.im.report.ReportParameters;
import com.codenvy.im.report.ReportType;
import com.codenvy.im.utils.Commons;
import com.codenvy.im.utils.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.codenvy.mail.MailSenderClient;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.schedule.ScheduleCron;

import javax.inject.Named;
import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static com.codenvy.im.utils.Commons.combinePaths;

/**
 * Sends reports:
 * <li>to Codenvy with number of users.</li>
 *
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class ReportSender {
    private final LdapManager      ldapManager;
    private final MailSenderClient mailClient;
    private final HttpTransport    httpTransport;
    private final String           updateServerEndpoint;
    private final ConfigManager    configManager;

    @Inject
    public ReportSender(@Named("installation-manager.update_server_endpoint") String updateServerEndpoint,
                        LdapManager ldapManager,
                        MailSenderClient mailClient,
                        HttpTransport httpTransport,
                        ConfigManager configManager) {
        this.ldapManager = ldapManager;
        this.mailClient = mailClient;
        this.httpTransport = httpTransport;
        this.updateServerEndpoint = updateServerEndpoint;
        this.configManager = configManager;
    }

//    @ScheduleCron(cron = "0 0 1 ? * SUN *")  // TODO [ndp] CDEC-376
    // @ScheduleCron(cron = "0 0/1 * 1/1 * ? *")  // send every 1 minute
    public void sendWeeklyReports() throws IOException, MessagingException, JsonParseException {
        sendNumberOfUsers();
    }

    private void sendNumberOfUsers() throws IOException, MessagingException, JsonParseException {
        ReportParameters parameters = obtainReportParameters(ReportType.CODENVY_ONPREM_USER_NUMBER_REPORT);

        if (! parameters.isActive()) {
            return;
        }

        String externalIP = obtainExternalIP();

        StringBuilder msg = new StringBuilder();
        msg.append(String.format("External IP address: %s\n", externalIP));
        msg.append(String.format("Hostname: %s\n", configManager.loadInstalledCodenvyConfig().getValue(Config.HOST_URL)));
        msg.append(String.format("Number of users: %s\n", ldapManager.getNumberOfUsers()));

        mailClient.sendMail(parameters.getSender(), parameters.getReceiver(), null, parameters.getTitle(), MediaType.TEXT_PLAIN, msg.toString());
    }

    private String obtainExternalIP() throws IOException {
        String requestUrl = combinePaths(updateServerEndpoint, "/util/client-ip");
        return httpTransport.doGet(requestUrl);
    }

    private ReportParameters obtainReportParameters(ReportType reportType) throws IOException, JsonParseException {
        String requestUrl = combinePaths(updateServerEndpoint, "/parameters/" + reportType.name().toLowerCase());

        String response = httpTransport.doPost(requestUrl, null);
        return Commons.fromJson(response, ReportParameters.class);
    }
}
