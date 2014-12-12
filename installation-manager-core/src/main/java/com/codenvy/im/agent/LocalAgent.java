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

import java.io.InputStream;

/** @author Anatoliy Bazko */
public class LocalAgent extends AbstractAgent {
    public LocalAgent() {
    }

    /** {@inheritDoc} */
    @Override
    public String execute(String command) throws AgentException {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);

        try {
            Process process = pb.start();

            int exitStatus = process.waitFor();
            InputStream in = process.getInputStream();
            InputStream err = process.getErrorStream();

            return processExistCode(exitStatus, in, err);
        } catch (Exception e) {
            String errMessage = String.format("Can't execute command '%s'.", command);
            throw makeAgentException(errMessage, e);
        }
    }
}
