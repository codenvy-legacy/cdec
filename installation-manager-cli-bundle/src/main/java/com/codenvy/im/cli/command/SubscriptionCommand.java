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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-subscription", description = "Check subscription")
public class SubscriptionCommand extends AbstractIMCommand {

    /** The default subscription name to check. */
    private final static String DEFAULT_SUBSCRIPTION = "OnPremises";

    @Argument(index = 0, name = "subscription", description = "The name of the subscription to check", required = false, multiValued = false)
    private String subscription;

    @Option(name = "--check", aliases = "--c", description = "To check subscription", required = false)
    private boolean check;

    @Override
    protected Void doExecute() {
        try {
            init();

            if (check) {
                String response = installationManagerProxy.checkSubscription(subscription != null ? subscription : DEFAULT_SUBSCRIPTION,
                                                                             getCredentialsRep());
                printResponse(response);
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }
}
