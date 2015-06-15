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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.facade.IMArtifactLabeledFacade;
import com.codenvy.im.managers.Config;
import com.google.common.collect.ImmutableMap;

import org.apache.felix.service.command.CommandSession;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.codenvy.im.artifacts.ArtifactFactory.createArtifact;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/** @author Anatoliy Bazko */
public class TestConfigCommand extends AbstractTestCommand {
    private AbstractIMCommand spyCommand;

    @Mock
    private IMArtifactLabeledFacade managerFacade;
    @Mock
    private CommandSession          commandSession;
    private CommandInvoker          commandInvoker;

    @BeforeMethod
    public void initMocks() throws IOException {
        MockitoAnnotations.initMocks(this);

        spyCommand = spy(new ConfigCommand());
        spyCommand.facade = managerFacade;

        commandInvoker = new CommandInvoker(spyCommand, commandSession);

        performBaseMocks(spyCommand, true);
    }

    @Test
    public void testGetConfig() throws Exception {
        doReturn(Collections.emptyMap()).when(managerFacade).getInstallationManagerProperties();

        CommandInvoker.Result result = commandInvoker.invoke();

        String output = result.getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"properties\" : { },\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }


    @Test
    public void testChangeCodenvyHostUrl() throws Exception {
        String testDns = "test.com";
        Map<String, String> properties = ImmutableMap.of(Config.HOST_URL, testDns);
        doNothing().when(managerFacade).updateArtifactConfig(createArtifact(CDECArtifact.NAME), properties);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--hostname", testDns);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n" +
                             "  \"status\" : \"OK\"\n" +
                             "}\n");
    }

    @Test
    public void testChangeCodenvyHostUrlWhenServiceThrowsError() throws Exception {
        String testDns = "test.com";
        Map<String, String> properties = ImmutableMap.of(Config.HOST_URL, testDns);
        String expectedOutput = "{\n"
                                + "  \"message\" : \"Server Error Exception\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doThrow(new RuntimeException("Server Error Exception"))
                .when(managerFacade).updateArtifactConfig(createArtifact(CDECArtifact.NAME), properties);

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--hostname", testDns);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }
}
