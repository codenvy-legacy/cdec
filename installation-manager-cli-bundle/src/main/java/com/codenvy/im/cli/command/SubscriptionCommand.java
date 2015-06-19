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
package com.codenvy.im.cli.command;

import com.codenvy.im.response.BasicResponse;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.google.common.collect.ImmutableMap;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Command(scope = "codenvy", name = "im-subscription", description = "Check Codenvy subscription")
public class SubscriptionCommand extends AbstractIMCommand {

    @Option(name = "--check", aliases = "-c", description = "The name of the subscription to check", required = false)
    private String subscription;

    @Override
    protected void doExecuteCommand() throws Exception {
        String subscription2check = subscription != null ? subscription : SaasAccountServiceProxy.ON_PREMISES;

        BasicResponse response;

        boolean isValid = facade.hasValidSaasSubscription(subscription2check, getCredentials());
        if (isValid) {
            response = BasicResponse.ok();
            response.setMessage("Subscription is valid");
            response.setProperties(ImmutableMap.of("subscription", subscription2check));
        } else {
            response = BasicResponse.error("Subscription not found or outdated");
            response.setProperties(ImmutableMap.of("subscription", subscription2check));
        }

        console.printResponseExitInError(response);
    }
}
