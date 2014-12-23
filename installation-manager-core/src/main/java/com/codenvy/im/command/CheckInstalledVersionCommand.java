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
package com.codenvy.im.command;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.utils.Version;

import java.io.IOException;

/**
 * Command runs until expected version is installed.
 *
 * @author Anatoliy Bazko
 */
public class CheckInstalledVersionCommand implements Command {
    private final Artifact artifact;
    private final Version  expectedVersion;

    public CheckInstalledVersionCommand(Artifact artifact, Version expectedVersion) {
        this.artifact = artifact;
        this.expectedVersion = expectedVersion;
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        for (; ; ) {
            try {
                if (checkExpectedVersion()) {
                    break;
                }
            } catch (IOException e) {
                continue;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        return null;
    }

    protected boolean checkExpectedVersion() throws IOException {
        Version installedVersion = artifact.getInstalledVersion();
        return expectedVersion.equals(installedVersion);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Expected to be installed '%s' of the version '%s'", artifact, expectedVersion);
    }
}
