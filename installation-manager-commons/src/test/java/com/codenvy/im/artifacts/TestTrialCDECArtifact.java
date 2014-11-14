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
package com.codenvy.im.artifacts;

import com.codenvy.im.utils.HttpTransport;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/** @author Anatoliy Bazko */
public class TestTrialCDECArtifact {
    private Artifact      trialCdecArtifact;
    private HttpTransport mockTransport;

    @BeforeMethod
    public void setUp() throws Exception {
        mockTransport = mock(HttpTransport.class);
        trialCdecArtifact = new TrialCDECArtifact("", mockTransport);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class,
          expectedExceptionsMessageRegExp = "Trial CDEC installation is not supported yet.")
    public void testInstall() throws IOException {
        trialCdecArtifact.install(null, null);
    }
}
