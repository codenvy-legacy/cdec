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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author Dmytro Nochevnov */
public class TestHttpTransportConfiguration {

    @Test
    public void testConfiguration() throws Exception {
        HttpTransportConfiguration config = new HttpTransportConfiguration("localhost", "80");
        assertEquals(config.getProxyUrl(), "localhost");
        assertEquals(config.getProxyPort(), 80);


        config.setProxyUrl("another  ");
        assertEquals(config.getProxyUrl(), "another");

        config.setProxyPort("8080");
        assertEquals(config.getProxyPort(), 8080);
    }

    @Test
    public void testInvalidPortNumber() throws Exception {
        HttpTransportConfiguration config = new HttpTransportConfiguration("localhost", "a");
        assertEquals(config.getProxyUrl(), "localhost");
        assertEquals(config.getProxyPort(), 0);
    }

    @Test
    public void testIsProxyConfValid() throws Exception {
        HttpTransportConfiguration config = new HttpTransportConfiguration("localhost", "80");
        assertTrue(config.isProxyConfValid());

        config.setProxyUrl(null);
        config.setProxyPort("80");
        assertFalse(config.isProxyConfValid());

        config.setProxyUrl("");
        config.setProxyPort("-20");
        assertFalse(config.isProxyConfValid());
    }
}
