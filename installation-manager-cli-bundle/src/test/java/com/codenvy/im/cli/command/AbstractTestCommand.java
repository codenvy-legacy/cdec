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

import com.codenvy.im.response.Response;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Anatoliy Bazko
 */
public abstract class AbstractTestCommand {
    void performBaseMocks(AbstractIMCommand spyCommand) {
        doNothing().when(spyCommand).init();
        doReturn(true).when(spyCommand).isInteractive();

        spyCommand.console = getSpyConsole(true);
    }

    private Console getSpyConsole(boolean isInstallable) {
        return spy(new Console(isInstallable) {
            @Override void printError(String message) {
                System.out.println(message);
            }

            @Override void printError(String message, boolean suppressCodenvyPrompt) {
                System.out.println(message);
            }

            @Override void printError(Exception ex) {
                System.out.println(Response.valueOf(ex).toJson());
            }

            @Override void printSuccess(String message) {
                System.out.println(message);
            }

            @Override void printSuccess(String message, boolean suppressCodenvyPrompt) {
                System.out.println(message);
            }

            @Override protected void printProgress(String message) {
                // disable progressor
            }
        });
    }
}
