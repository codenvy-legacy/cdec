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

import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.saas.SaasAccountServiceProxy;
import com.codenvy.im.saas.SaasUserCredentials;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Anatoliy Bazko */
public class TestSubscriptionCommand extends AbstractTestCommand {
    private SubscriptionCommand spyCommand;

    @Mock
    private IMArtifactLabeledFacade mockInstallationManagerProxy;
    @Mock
    private CommandSession          commandSession;

    private SaasUserCredentials credentials;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new SubscriptionCommand());
        spyCommand.facade = mockInstallationManagerProxy;

        performBaseMocks(spyCommand, true);

        credentials = new SaasUserCredentials("token", "accountId");
        doReturn(credentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testCheckDefaultSubscription() throws Exception {
        doReturn(true).when(mockInstallationManagerProxy).hasValidSaasSubscription(SaasAccountServiceProxy.ON_PREMISES, credentials);
        doNothing().when(spyCommand).validateIfUserLoggedIn();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"properties\" : {\n" +
                             "    \"subscription\" : \"OnPremises\"\n" +
                             "  },\n" +
                             "  \"message\" : \"Subscription is valid\",\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testCheckSubscriptionReturnOkResponse() throws Exception {
        doReturn(true).when(mockInstallationManagerProxy).hasValidSaasSubscription("AnotherSubscription", credentials);
        doNothing().when(spyCommand).validateIfUserLoggedIn();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--check", "AnotherSubscription");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"properties\" : {\n" +
                             "    \"subscription\" : \"AnotherSubscription\"\n" +
                             "  },\n" +
                             "  \"message\" : \"Subscription is valid\",\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testCheckSubscriptionWhenServiceThrowsError() throws Exception {
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception"))
                .when(mockInstallationManagerProxy).hasValidSaasSubscription(anyString(), eq(credentials));
        doNothing().when(spyCommand).validateIfUserLoggedIn();

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }
}
