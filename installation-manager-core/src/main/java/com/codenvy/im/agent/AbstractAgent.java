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
package com.codenvy.im.agent;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.String.format;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractAgent implements Agent {

    protected String processOutput(int exitStatus, InputStream in, InputStream error) throws Exception {
        String output = readOutput(in);
        String errorOutput = readOutput(error);

        if (exitStatus == 0) {
            return output;
        }

        throw new Exception(format("Output: %s; Error: %s.", output, errorOutput));
    }

    protected AgentException makeAgentException(String errorMessage, Exception e) {
        if (e.getMessage() != null) {
            errorMessage += " " + e.getMessage();
        }
        return new AgentException(errorMessage, e);
    }

    private String readOutput(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String output = IOUtils.toString(reader);
            if (output.endsWith("\n")) {
                output = output.substring(0, output.length() - 1);
            }
            return output;
        }
    }
}
