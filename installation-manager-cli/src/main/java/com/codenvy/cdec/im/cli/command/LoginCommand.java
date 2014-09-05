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
package com.codenvy.cdec.im.cli.command;

import jline.console.ConsoleReader;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static java.lang.String.format;

/**
 * Allow to login in the default remote or a given remote
 * @author Florent Benoit
 */
@Command(scope = "im", name = "login", description = "Login to Codenvy Update Server")
public class LoginCommand extends AbstractIMCommand {

    @Argument(name = "username", description = "Username of the update server", required = false, multiValued = false, index = 0)
    private String username;

    @Argument(name = "password", description = "Password of the update server", required = false, multiValued = false, index = 1)
    private String password;

    @Argument(name = "accountId", description = "ID of Codenvy account which had been used for subscription", required = false, multiValued = false, index = 2)
    private String accountId;

    @Override
    protected Object doExecute() throws Exception {
        try {
            init();
    
            String remoteUrl = getUpdateServerUrl();
            String remoteName = getUpdateServerRemote();
            
            if (username == null) {
                if (isInteractive()) {
                    System.out.print("Username for '" + remoteUrl + "':");
                    System.out.flush();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        username = reader.readLine();
                    }
                    System.out.println(System.lineSeparator());
                } else {
                    username = new ConsoleReader().readLine("Username for '" + remoteUrl + "':");
                }
            }
    
            if (password == null) {
                if (isInteractive()) {
                    System.out.print("Password for " + username + ":");
                    System.out.flush();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        password = reader.readLine();
                    }
                    System.out.println(System.lineSeparator());
                } else {
                    password = new ConsoleReader().readLine(String.format("Password for %s:", username), Character.valueOf('*'));
                }
            }
    
            if (accountId == null) {
                if (isInteractive()) {
                    System.out.print("ID of account used for subscription:");
                    System.out.flush();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(session.getKeyboard(), Charset.defaultCharset()))) {
                        accountId = reader.readLine();
                    }
                    System.out.println(System.lineSeparator());
                } else {
                    accountId = new ConsoleReader().readLine("ID of account used for subscription:");
                }
            }
            
            preferencesStorage.setAccountId(accountId);
            
            if (getMultiRemoteCodenvy().login(remoteName, username, password)) {
                System.out.println(format("Login success!"));
                return null;
            } else {
                System.out.println("Login failed: please check the credentials.");
            }
        } catch(Exception e) {
            printError(e);
        }

        return null;
    }
}