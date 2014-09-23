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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Installation manager Login command.
 */
@Command(scope = "im", name = "login", description = "Login to Codenvy Update Server")
public class LoginCommand extends AbstractIMCommand {

    @Argument(name = "username", description = "The username", required = false, multiValued = false, index = 0)
    private String username;

    @Argument(name = "password", description = "The user's password", required = false, multiValued = false, index = 1)
    private String password;

    @Argument(name = "accountId", description = "The user's account ID", required = false, multiValued = false, index = 2)
    private String accountId;

    @Override
    protected Void doExecute() throws Exception {
        try {
            init();

            String remoteUrl = getUpdateServerUrl();
            String remoteName = getRemoteNameForUpdateServer();
            if (remoteName == null) {
                createUpdateServerRemote(remoteUrl);
            }

            if (username == null) {
                if (isInteractive()) {
                    printInfo("Username for '" + remoteUrl + "':");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        username = reader.readLine();
                    }
                    printLineSeparator();
                } else {
                    username = new ConsoleReader().readLine("Username for '" + remoteUrl + "':");
                }
            }

            if (password == null) {
                if (isInteractive()) {
                    printInfo("Password for " + username + ":");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        password = reader.readLine();
                    }
                    printLineSeparator();
                } else {
                    password = new ConsoleReader().readLine(String.format("Password for %s:", username), '*');
                }
            }

            if (accountId == null) {
                if (isInteractive()) {
                    printInfo("Account ID:");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        accountId = reader.readLine();
                    }
                    printLineSeparator();
                } else {
                    accountId = new ConsoleReader().readLine("ID of account used for subscription:");
                }
            }

            preferencesStorage.setAccountId(accountId);

            if (getMultiRemoteCodenvy().login(remoteName, username, password)) {
                printSuccess("Login succeeded.");
                return null;
            } else {
                printError("Login failed: please check the credentials.");
            }
        } catch (Exception e) {
            printError(e);
        }

        return null;
    }

    @Override
    protected void validateIfUserLoggedIn() {
        // do nothing
    }
}