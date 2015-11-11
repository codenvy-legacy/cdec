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

import com.codenvy.im.saas.SaasApiServiceProxy;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.codenvy.mail.MailSenderClient;
import org.eclipse.che.commons.schedule.ScheduleCron;

import javax.inject.Named;
import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Sends reports:
 * <li>to Codenvy with number of users.</li>
 *
 * @author Anatoliy Bazko
 */
@Singleton
public class ReportSender {
    private static final String WEEKLY_ON_PREM_REPORT = "Weekly On-Prem report";

    private final SaasApiServiceProxy apiService;
    private final MailSenderClient    mailClient;
    private final String              recipients;
    private final String              sender;

    @Inject
    public ReportSender(@Named("installation-manager.report-sender.recipients") String recipients,
                        @Named("installation-manager.report-sender.sender") String sender,
                        SaasApiServiceProxy apiService,
                        MailSenderClient mailClient) {
        this.apiService = apiService;
        this.mailClient = mailClient;
        this.recipients = recipients;
        this.sender = sender;
    }

    //    @ScheduleCron(cron = "0 0 1 ? * SUN *") // TODO [AB] every sunday's night
    // @ScheduleCron(cron = "0 0/1 * 1/1 * ? *")
    public void sendWeeklyReports() throws IOException, MessagingException {
        sendNumberOfUsers();
    }

    private void sendNumberOfUsers() throws IOException, MessagingException {
        StringBuilder msg = new StringBuilder();
        msg.append("Hostname: ");
        msg.append(InetAddress.getLocalHost().getHostName());
        msg.append('\n');
        msg.append("Number of users: ");
        msg.append(apiService.getUsersCount());
        msg.append('\n');
        msg.append('\n');

        mailClient.sendMail(sender, recipients, null, WEEKLY_ON_PREM_REPORT, MediaType.TEXT_PLAIN, msg.toString());
    }
}
