/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
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
package com.codenvy.im.utils;

import org.codenvy.mail.MailSenderClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.MessagingException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Date;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Dmytro Nochevnov
 */
public class TestMailService {
    public static final String SENDER_EMAIL = "from@com";
    public static final String RECEIVER_EMAILS = "to@com";
    private MailService spyMailService;
    private MailServiceConfiguration mailTransportConfig = new MailServiceConfiguration(RECEIVER_EMAILS, SENDER_EMAIL);

    @Mock
    private MailSenderClient mockMailService;

    @BeforeMethod
    public void init() {
        MockitoAnnotations.initMocks(this);
        new MailServiceConfiguration("", "0");
        spyMailService = spy(new MailService(mockMailService, mailTransportConfig));
    }

    @Test
    public void testSendOnPremSubscriptionInfo() throws IOException, MessagingException {
        String expectedSubscriptionInfo = "Email address of user: userEmail\n"
                                          + "AccountID of user: accountId\n"
                                          + "Date and time of request: " + new Date().toString();

        spyMailService.sendNotificationLetter("accountId", "userEmail");

        verify(mockMailService).sendMail(SENDER_EMAIL,
                                         RECEIVER_EMAILS,
                                         null,
                                         "New On-Prem Trial (IM)",
                                         MediaType.TEXT_PLAIN,
                                         expectedSubscriptionInfo);
    }
}
