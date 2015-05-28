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

import com.codenvy.im.managers.NodeConfig;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 */
public class TestNodeInfo {

    private static final String              TEST_HOST   = "host";
    private static final NodeConfig.NodeType TEST_TYPE   = NodeConfig.NodeType.RUNNER;
    private static final ArtifactStatus TEST_STATUS = ArtifactStatus.SUCCESS;

    @Test
    public void testConstructor() throws Exception {
        NodeInfo info = new NodeInfo(TEST_TYPE, TEST_HOST, TEST_STATUS);
        assertEquals(info.getType(), TEST_TYPE);
        assertEquals(info.getHost(), TEST_HOST);
        assertEquals(info.getStatus(), TEST_STATUS);
    }

    @Test
    public void testType() throws Exception {
        NodeInfo info = new NodeInfo().setType(TEST_TYPE);
        assertEquals(info.getType(), TEST_TYPE);
    }

    @Test
    public void testHost() throws Exception {
        NodeInfo info = new NodeInfo().setHost(TEST_HOST);
        assertEquals(info.getHost(), TEST_HOST);
    }

    @Test
    public void testStatus() throws Exception {
        NodeInfo info = new NodeInfo().setStatus(TEST_STATUS);
        assertEquals(info.getStatus(), TEST_STATUS);
    }

    @Test
    public void testCreateSuccessInfo() throws Exception {
        NodeInfo info = NodeInfo.createSuccessInfo(new NodeConfig(TEST_TYPE, TEST_HOST, null));
        assertEquals(info.getType(), TEST_TYPE);
        assertEquals(info.getHost(), TEST_HOST);
        assertEquals(info.getStatus(), ArtifactStatus.SUCCESS);
    }
}
