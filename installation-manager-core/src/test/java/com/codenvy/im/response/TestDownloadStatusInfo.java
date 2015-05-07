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
package com.codenvy.im.response;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Anatoliy Bazko
 */
public class TestDownloadStatusInfo {

    @Test
    public void testValueOf() throws Exception {
        DownloadStatusInfo expectedInfo = new DownloadStatusInfo(Status.SUCCESS, 100, new Response().setMessage("result"));
        String json = new Response().setStatus(ResponseCode.OK).setDownloadInfo(expectedInfo).toJson();

        Response response = Response.fromJson(json);
        assertEquals(response.getDownloadInfo(), expectedInfo);
    }
}
