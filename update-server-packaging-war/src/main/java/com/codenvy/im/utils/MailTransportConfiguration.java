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

import com.google.inject.Inject;

import javax.inject.Named;
import javax.inject.Singleton;

/** @author Dmytro Nochevnov */
@Singleton
public class MailTransportConfiguration {

    private final String trialSubscriptionInfoReceiverEmails;

    private final String mailSender;

    @Inject
    public MailTransportConfiguration(@Named("mail-transport.trial-subscription-info-receiver-emails") String trialSubscriptionInfoReceiverEmails,
                                      @Named("mail-transport.sender") String mailSender) {
        this.trialSubscriptionInfoReceiverEmails = trialSubscriptionInfoReceiverEmails;
        this.mailSender = mailSender;
    }

    public String getTrialSubscriptionInfoReceiverEmails() {
        return trialSubscriptionInfoReceiverEmails;
    }

    public String getMailSender() {
        return mailSender;
    }
}
