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

import com.codenvy.im.managers.LdapManager;
import com.codenvy.im.utils.HttpTransport;
import org.codenvy.mail.MailSenderClient;
import org.mockito.Mock;

/**
 * @author Dmytro Nochevnov
 */
public class ReportSenderTest {
    @Mock
    private LdapManager ldapManager;

    @Mock
    private MailSenderClient mailClient;

    @Mock
    private HttpTransport httpTransport;

    private String recipients;
    private String sender;
    private String updateServerEndpoint;

}
