/*
 *  [2012] - [2016] Codenvy, S.A.
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

import com.codenvy.im.facade.IMArtifactLabeledFacade;

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
    private IMArtifactLabeledFacade installationManagerFacade;
    @Mock
    private CommandSession          commandSession;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new ChangeAdminPasswordCommand());
        spyCommand.facade = installationManagerFacade;

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void test() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("currentPassword", "currentPassword");
        commandInvoker.argument("newPassword", "newPassword");

        commandInvoker.invoke();

        verify(installationManagerFacade).changeAdminPassword("currentPassword".getBytes("UTF-8"), "newPassword".getBytes("UTF-8"));
    }
}