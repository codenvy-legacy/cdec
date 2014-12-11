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

import com.codenvy.api.account.shared.dto.AccountReference;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

/**
 * Installation manager Login command.
 */
@Command(scope = "codenvy", name = "login", description = "Login to remote Codenvy cloud")
public class LoginCommand extends AbstractIMCommand {

    public static final String CANNOT_RECOGNISE_ACCOUNT_NAME_MSG =
            "You are logged as a user which does not have an account/owner role in any account. " +
            "This likely means that you used the wrong credentials to access Codenvy.";

    @Argument(name = "username", description = "The username", required = false, multiValued = false, index = 0)
    private String username;

    @Argument(name = "password", description = "The user's password", required = false, multiValued = false, index = 1)
    private String password;

    @Argument(name = "accountName", description = "The user's account name", required = false, multiValued = false, index = 2)
    private String accountName;

    @Option(name = "--remote", description = "Name of the remote codenvy", required = false)
    private String remoteName;

    @Override
    protected Void execute() throws Exception {
        try {
            init();

            if (remoteName == null) {
                remoteName = getOrCreateRemoteNameForUpdateServer();
            }

            if (username == null) {
                console.print(String.format("Codenvy user name for remote '%s': ", remoteName));
                username = console.readLine();
            }

            if (password == null) {
                console.print(String.format("Password for %s: ", username));
                password = console.readPassword();
            }

            if (!getMultiRemoteCodenvy().login(remoteName, username, password)) {
                printError(String.format("Login failed on remote '%s'.", remoteName));
                return null;
            }

            if (!isRemoteForUpdateServer(remoteName)) {
                console.printSuccess(String.format("Login success on remote '%s' [%s].",
                                           remoteName,
                                           getRemoteUrlByName(remoteName)));
                return null;
            }

            AccountReference accountReference = getAccountReferenceWhereUserIsOwner(accountName);
            if (accountReference == null) {
                preferencesStorage.invalidate();
                if (accountName == null) {
                    printError(CANNOT_RECOGNISE_ACCOUNT_NAME_MSG);
                } else {
                    printError("Account '" + accountName + "' is not yours or may be you aren't owner of this account.");
                }
                return null;
            }

            if (accountName == null) {
                console.printSuccess("Your Codenvy account '" + accountReference.getName() + "' will be used to verify on-premises subscription.");
            }

            preferencesStorage.setAccountId(accountReference.getId());
            console.printSuccess("Login success.");

        } catch (Exception e) {
            if (preferencesStorage != null) {
                preferencesStorage.invalidate();
            }
            printError(e);
        }

        return null;
    }

    @Override
    protected void validateIfUserLoggedIn() {
        // do nothing
    }
}
