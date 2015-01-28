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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestMailTransportConfiguration {

    @Test
    public void testMailTransportConfiguration() {
        MailTransportConfiguration config = new MailTransportConfiguration("emails", "sender");
        assertEquals(config.getTrialSubscriptionInfoReceiverEmails(), "emails");
        assertEquals(config.getMailSender(), "sender");
    }
}
