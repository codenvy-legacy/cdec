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

import com.codenvy.im.facade.InstallationManagerFacade;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TestChangeAdminPasswordCommand extends AbstractTestCommand {

    private ChangeAdminPasswordCommand spyCommand;

    @Mock
    private InstallationManagerFacade installationManagerFacade;
    @Mock
    private CommandSession            commandSession;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new ChangeAdminPasswordCommand());
        spyCommand.facade = installationManagerFacade;

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testCheckDefaultSubscription() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("password", "newPassword");

        commandInvoker.invoke();

        verify(installationManagerFacade).changeAdminPassword("newPassword".getBytes("UTF-8"));
    }
}