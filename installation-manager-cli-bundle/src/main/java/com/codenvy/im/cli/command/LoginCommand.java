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

import jline.console.ConsoleReader;

import com.codenvy.api.account.shared.dto.AccountReference;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Installation manager Login command.
 */
@Command(scope = "codenvy", name = "login", description = "Login to Codenvy Update Server or another remote Codenvy cloud")
public class LoginCommand extends AbstractIMCommand {

    public static final String CANNOT_RECOGNISE_ACCOUNT_NAME_MSG =
            "You are logged as a user which does not have an account/owner role in any account. This likely means that you used the wrong credentials to access Codenvy.";

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
                if (isInteractive()) {
                    printInfo(String.format("Username for remote '%s': ", remoteName));
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        username = reader.readLine();
                    }
                    printLineSeparator();
                } else {
                    username = new ConsoleReader().readLine(String.format("Username for remote '%s': ", remoteName));
                }
            }

            if (password == null) {
                if (isInteractive()) {
                    printInfo(String.format("Password for %s: ", username));
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        password = reader.readLine();
                    }
                    printLineSeparator();
                } else {
                    password = new ConsoleReader().readLine(String.format("Password for %s: ", username), '*');
                }
            }

            if (!getMultiRemoteCodenvy().login(remoteName, username, password)) {
                printError(String.format("Login failed on remote '%s'.", remoteName));
                return null;
            }

            if (!isRemoteForUpdateServer(remoteName)) {
                printSuccess(String.format("Login success on remote '%s' [%s].",
                                           remoteName,
                                           getRemoteUrlByName(remoteName)));
                return null;
            }

            AccountReference accountReference = getAccountReferenceWhereUserIsOwner();
            if (accountReference == null || accountReference.getId() == null || accountReference.getId().isEmpty()) {
                printError(CANNOT_RECOGNISE_ACCOUNT_NAME_MSG);
                preferencesStorage.invalidate();
                return null;
            }

            preferencesStorage.setAccountId(accountReference.getId());

            if (accountName == null) {
                printSuccess("Your Codenvy account '" + accountReference.getName() + "' has been obtained and will be used to verify subscription.");
            } else {
                if (!isValidAccount() || !accountName.equals(accountReference.getName())) {
                    printError("Account '" + accountName + "' is not yours or may be you aren't owner of this account.");
                    preferencesStorage.invalidate();
                    return null;
                }
            }

            printSuccess(String.format("Login success on remote '%s' [%s] which is used by installation manager commands.",
                                       remoteName,
                                       getRemoteUrlByName(remoteName)));

        } catch (Exception e) {
            printError(e);
            if (preferencesStorage != null) {
                preferencesStorage.invalidate();
            }
        }

        return null;
    }

    @Override
    protected void validateIfUserLoggedIn() {
        // do nothing
    }
}
