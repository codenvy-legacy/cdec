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
package com.codenvy.im.command;

import com.codenvy.im.service.InstallationManagerConfig;

import java.io.IOException;

import static java.lang.String.format;

/**
 * Store property value into the installation-manager config file.
 *
 * @author Dmytro Nochevnov
 */
public class StoreIMConfigPropertyCommand implements Command {
    private final String propertyName;
    private final String propertyValue;

    public StoreIMConfigPropertyCommand(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /** {@inheritDoc} */
    @Override
    public String execute() throws CommandException {
        try {
            InstallationManagerConfig.storeProperty(propertyName, propertyValue);
        } catch (IOException e) {
            throw new CommandException(format("It is impossible to store %s", toString()), e);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return format("Save property %s = %s into the installation manager config", propertyName, propertyValue);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return format("{'propertyName':'%s','propertyValue':'%s'}", propertyName, propertyValue);
    }

    /** Factory */
    public static Command createSaveCodenvyHostDnsCommand(final String hostDns) {
        return new StoreIMConfigPropertyCommand(InstallationManagerConfig.CODENVY_HOST_DNS, hostDns);
    }
}
