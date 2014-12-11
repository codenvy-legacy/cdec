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
package com.codenvy.im.cli.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-subscription", description = "Check Codenvy subscription")
public class SubscriptionCommand extends AbstractIMCommand {

    /** The default subscription name to check. */
    protected final static String DEFAULT_SUBSCRIPTION = "OnPremises";

    @Option(name = "--check", aliases = "-c", description = "The name of the subscription to check", required = false)
    private String subscription;

    @Override
    protected Void execute() {
        try {
            init();

            printResponse(installationManagerProxy.checkSubscription(subscription != null ? subscription : DEFAULT_SUBSCRIPTION,
                                                                     getCredentialsRep()));
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}
