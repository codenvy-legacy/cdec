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
package com.codenvy.im.commands;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.interrupter.Context;
import com.codenvy.im.interrupter.Interruptable;
import com.codenvy.im.interrupter.Interrupter;
import com.codenvy.im.interrupter.NullInterrupter;
import com.codenvy.im.utils.Version;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Command runs until expected version is installed.
 *
 * @author Anatoliy Bazko
 */
public class CheckInstalledVersionCommand implements Command, Interruptable {
    public static final int CHECK_VERSION_TIMEOUT_MILLIS = 500;
    private final Artifact artifact;
    private final Version expectedVersion;

    private Interrupter interrupter = new NullInterrupter();

    private static final Logger LOG = Logger.getLogger(CheckInstalledVersionCommand.class.getSimpleName());


    public CheckInstalledVersionCommand(Artifact artifact, Version expectedVersion) {
        this(artifact, expectedVersion, null);
    }

    public CheckInstalledVersionCommand(Artifact artifact, Version expectedVersion, Class<? extends Interrupter> interrupterClass) {
        this.artifact = artifact;
        this.expectedVersion = expectedVersion;

        if (interrupterClass != null) {
            try {
                interrupter = interrupterClass.getConstructor(Interruptable.class).newInstance(this);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        LOG.log(Level.INFO, toString());

        interrupter.start();

        for (; ; ) {
            if (interrupter.hasInterrupted()) {
                throw new CommandException(format("Interrupted: %s", interrupter.getContext().getMessage()));
            }

            try {
                if (checkExpectedVersion()) {
                    break;
                }
            } catch (IOException e) {
                // ignore
            }

            try {
                Thread.sleep(CHECK_VERSION_TIMEOUT_MILLIS);
            } catch (InterruptedException e) {
                interrupter.stop();
                throw new RuntimeException(e);
            }
        }

        interrupter.stop();
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
        return format("Expected to be installed '%s' of the version '%s'", artifact, expectedVersion);
    }

    @Override
    public void interrupt(Context context) {
        // do nothing
    }
}
