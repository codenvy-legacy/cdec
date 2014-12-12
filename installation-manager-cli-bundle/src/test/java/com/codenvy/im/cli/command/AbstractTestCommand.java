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

import java.io.IOException;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
public abstract class AbstractTestCommand {
    void performBaseMocks(AbstractIMCommand spyCommand, boolean interactive) throws IOException {
        doNothing().when(spyCommand).init();
        doReturn(interactive).when(spyCommand).isInteractive();

        spyCommand.console = getSpyConsole(interactive);
        doNothing().when(spyCommand.console)
                   .exit(anyInt());  // avoid error "The forked VM terminated without properly saying goodbye. VM crash or System.exit called?"
    }

    private Console getSpyConsole(boolean isInstallable) throws IOException {
        return spy(new Console(isInstallable) {
            @Override
            protected void printProgress(String message) {
                // disable progressor
            }
        });
    }
}
