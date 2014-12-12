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
package com.codenvy.im.agent;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractAgent implements Agent {

    protected String processExistCode(int exitStatus, InputStream in, InputStream error) throws Exception {
        if (exitStatus != 0) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(error));
            throw new Exception(IOUtils.toString(bufferedReader));
        } else {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            return IOUtils.toString(bufferedReader);
        }
    }

    protected AgentException makeAgentException(String errorMessage, Exception e) {
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorMessage += format(" Error: %s", e.getMessage());
        }
        return new AgentException(errorMessage, e);
    }
}
