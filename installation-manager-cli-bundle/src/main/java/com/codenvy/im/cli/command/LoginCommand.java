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

import com.codenvy.im.saas.SaasAccountServiceProxy;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.eclipse.che.api.account.shared.dto.AccountReference;

import static java.lang.String.format;

/**
 * Installation manager Login command.
 */
@Command(scope = "codenvy", name = "login", description = "Login to remote Codenvy cloud")
public class LoginCommand extends AbstractIMCommand {

    @Argument(name = "username", description = "The username", required = false, multiValued = false, index = 0)
    private String username;

    @Argument(name = "password", description = "The user's password", required = false, multiValued = false, index = 1)
    private String password;

    @Argument(name = "accountName", description = "The user's account name", required = false, multiValued = false, index = 2)
    private String accountName;

    @Option(name = "--remote", description = "Name of the remote codenvy", required = false)
    private String remoteName;

    @Override
    protected void doExecuteCommand() throws Exception {
        try {
            if (remoteName == null) {
                remoteName = getOrCreateRemoteNameForSaasServer();
            }

            if (username == null) {
                console.print(format("Codenvy user name for remote '%s': ", remoteName));
                username = console.readLine();
            }

            if (password == null) {
                console.print(format("Password for %s: ", username));
                password = console.readPassword();
            }

            if (!getMultiRemoteCodenvy().login(remoteName, username, password)) {
                console.printErrorAndExit(format("Login failed on remote '%s'.", remoteName));
                return;
            }

            if (!isRemoteForSaasServer(remoteName)) {
                console.printSuccess(format("Login success on remote '%s' [%s].",
                                            remoteName,
                                            getRemoteUrlByName(remoteName)));
                return;
            }

            AccountReference accountReference = getAccountReferenceWhereUserIsOwner(accountName);
            if (accountReference == null) {
                preferencesStorage.invalidate();
                if (accountName == null) {
                    console.printErrorAndExit(SaasAccountServiceProxy.CANNOT_RECOGNISE_ACCOUNT_NAME_MSG);
                } else {
                    console.printErrorAndExit("Account '" + accountName + "' is not yours or may be you aren't owner of this account.");
                }
                return;
            }

            if (accountName == null) {
                console.printSuccess(format(SaasAccountServiceProxy.USE_ACCOUNT_MESSAGE_TEMPLATE, accountReference.getName()));
            }

            preferencesStorage.setAccountId(accountReference.getId());
            console.printSuccess("Login success.");

            facade.addTrialSaasSubscription(getCredentials());
        } catch (Exception e) {
            if (preferencesStorage != null) {
                preferencesStorage.invalidate();
            }

            throw e;
        }
    }

    @Override
    protected void validateIfUserLoggedIn() {
        // do nothing
    }
}
