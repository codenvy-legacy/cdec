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

import com.codenvy.im.artifacts.CDECArtifact;
import com.codenvy.im.config.CdecConfig;
import com.codenvy.im.config.ConfigFactory;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.response.Response;
import com.codenvy.im.restlet.InstallationManagerService;
import com.codenvy.im.user.UserCredentials;

import org.apache.felix.service.command.CommandSession;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 *         Alexander Reshetnyak
 */
public class TestInstallCommand {
    private InstallCommand spyCommand;

    private InstallationManagerService mockInstallationManagerProxy;
    private ConfigFactory configFactory;
    private CommandSession             commandSession;

    private UserCredentials userCredentials;
    private String okServiceResponse = "{\n"
                                       + "  \"artifacts\" : [ {\n"
                                       + "    \"artifact\" : \"cdec\",\n"
                                       + "    \"version\" : \"1.0.1\",\n"
                                       + "    \"status\" : \"SUCCESS\"\n"
                                       + "  } ],\n"
                                       + "  \"status\" : \"OK\"\n"
                                       + "}";

    @BeforeMethod
    public void initMocks() throws Exception {
        mockInstallationManagerProxy = mock(InstallationManagerService.class);
        configFactory = mock(ConfigFactory.class);
        doReturn(new Response.Builder().withInfos(new ArrayList<String>() {
            {
                add("step 1");
                add("step 2");
            }
        }).build().toJson()).when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));
        commandSession = mock(CommandSession.class);

        spyCommand = spy(new InstallCommand(configFactory));
        spyCommand.installationManagerProxy = mockInstallationManagerProxy;

        doNothing().when(spyCommand).init();

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testInstallArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        final String expectedOutput = "step 1 [OK]\n" +
                                      "step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, expectedOutput);
    }

    @Test
    public void testInstallArtifactVersion() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        final String expectedOutput = "step 1 [OK]\n" +
                                      "step 2 [OK]\n" +
                                      "{\n" +
                                      "  \"artifacts\" : [ {\n" +
                                      "    \"artifact\" : \"cdec\",\n" +
                                      "    \"version\" : \"1.0.1\",\n" +
                                      "    \"status\" : \"SUCCESS\"\n" +
                                      "  } ],\n" +
                                      "  \"status\" : \"OK\"\n" +
                                      "}\n";

        doReturn(okServiceResponse).when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);
        commandInvoker.argument("version", "1.0.1");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, expectedOutput);
    }

    @Test
    public void testInstallWhenUnknownArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class));
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        final String serviceErrorResponse = "{\n"
                                      + "  \"message\" : \"Artifact 'any' not found\",\n"
                                      + "  \"status\" : \"ERROR\"\n"
                                      + "}";

        doReturn(serviceErrorResponse).when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", "any");

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, serviceErrorResponse + "\n");
    }

    @Test
    public void testInstallWhenArtifactNameIsAbsent() throws Exception {
        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);

        CommandInvoker.Result result = commandInvoker.invoke();

        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "Argument 'artifact' is required.\n");
    }

    @Test
    public void testInstallErrorStepFailed() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class));
        final String expectedOutput = "step 1 [FAIL]\n" +
                                "{\n"
                                + "  \"message\" : \"step failed\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doReturn("{\n"
                 + "  \"message\" : \"step failed\",\n"
                 + "  \"status\" : \"ERROR\"\n"
                 + "}").when(mockInstallationManagerProxy).install(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testInstallWhenServiceThrowsError2() throws Exception {
        doNothing().when(spyCommand).confirmOrReenterInstallOptions(any(InstallOptions.class));
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class));
        final String expectedOutput = "{\n"
                                + "  \"message\" : \"Property is missed\",\n"
                                + "  \"status\" : \"ERROR\"\n"
                                + "}";
        doReturn(expectedOutput).when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, expectedOutput + "\n");
    }

    @Test
    public void testListOption() throws Exception {
        doReturn(okServiceResponse).when(mockInstallationManagerProxy).getVersions(new JacksonRepresentation<>(userCredentials));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"CLI client version\" : \"1.1.0-SNAPSHOT\",\n"
                             + "  \"artifacts\" : [ {\n"
                             + "    \"artifact\" : \"cdec\",\n"
                             + "    \"version\" : \"1.0.1\",\n"
                             + "    \"status\" : \"SUCCESS\"\n"
                             + "  } ],\n"
                             + "  \"status\" : \"OK\"\n"
                             + "}\n");
    }

    @Test
    public void testListOptionWhenServiceError() throws Exception {
        doThrow(new ResourceException(500, "Server Error Exception", "Description", "localhost"))
                .when(mockInstallationManagerProxy)
                .getVersions(new JacksonRepresentation<>(userCredentials));

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.option("--list", Boolean.TRUE);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();
        assertEquals(output, "{\n"
                             + "  \"message\" : \"Server Error Exception\",\n"
                             + "  \"status\" : \"ERROR\"\n"
                             + "}\n");
    }

    @Test
    public void testEnterInstallOptions() throws Exception {
        // some basic loaded configuration
        doReturn(new CdecConfig(new HashMap<String, String>() {{
            put("mongo_admin_password", "mongoPassword");
            put("mongo_user_password", "mongoUserPassword");
            put("mongo_orgservice_user_password", "mongoOrgServiceUserPassword");
        }})).when(configFactory).loadOrCreateConfig(any(InstallOptions.class));

        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                InstallCommand installCommand = (InstallCommand)invocationOnMock.getMock();
                installCommand.printInfo("some value\n");
                return "some value";
            }
        }).when(spyCommand).readLine(anyString());

        // no installation info provided
        doReturn("{\"infos\":{}}").when(mockInstallationManagerProxy).getInstallInfo(any(JacksonRepresentation.class));

        // first reply [n], then reply [y]
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                InstallCommand installCommand = (InstallCommand)invocationOnMock.getMock();
                installCommand.printInfo(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return false;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                InstallCommand installCommand = (InstallCommand)invocationOnMock.getMock();
                installCommand.printInfo(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return true;
            }
        }).when(spyCommand).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        verify(configFactory, times(1)).writeConfig(any(CdecConfig.class));
        assertEquals(output, "Please, enter CDEC required parameters:\n" +
                             "dns name: some value\n" +
                             "mongo admin password (mongoPassword): some value\n" +
                             "mongo user password (mongoUserPassword): some value\n" +
                             "mongo orgservice user password (mongoOrgServiceUserPassword): some value\n" +
                             "admin ldap user name: some value\n" +
                             "admin ldap password: some value\n" +
                             "mysql root user password: some value\n" +
                             "zabbix database password: some value\n" +
                             "zabbix admin email: some value\n" +
                             "zabbix admin password: some value\n" +
                             "puppet version (puppet-3.4.3-1.el6.noarch): some value\n" +
                             "puppet resource url (http://yum.puppetlabs.com/el/6/products/x86_64/puppetlabs-release-6-7.noarch.rpm): some value\n" +
                             "{\n" +
                             "  \"zabbix_admin_email\" : \"some value\",\n" +
                             "  \"mongo_user_password\" : \"some value\",\n" +
                             "  \"puppet_resource_url\" : \"some value\",\n" +
                             "  \"zabbix_admin_password\" : \"some value\",\n" +
                             "  \"zabbix_database_password\" : \"some value\",\n" +
                             "  \"mysql_root_user_password\" : \"some value\",\n" +
                             "  \"admin_ldap_password\" : \"some value\",\n" +
                             "  \"dns_name\" : \"some value\",\n" +
                             "  \"mongo_orgservice_user_password\" : \"some value\",\n" +
                             "  \"puppet_version\" : \"some value\",\n" +
                             "  \"admin_ldap_user_name\" : \"some value\",\n" +
                             "  \"mongo_admin_password\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "Please, enter CDEC required parameters:\n" +
                             "dns name (some value): some value\n" +
                             "mongo admin password (some value): some value\n" +
                             "mongo user password (some value): some value\n" +
                             "mongo orgservice user password (some value): some value\n" +
                             "admin ldap user name (some value): some value\n" +
                             "admin ldap password (some value): some value\n" +
                             "mysql root user password (some value): some value\n" +
                             "zabbix database password (some value): some value\n" +
                             "zabbix admin email (some value): some value\n" +
                             "zabbix admin password (some value): some value\n" +
                             "puppet version (some value): some value\n" +
                             "puppet resource url (some value): some value\n" +
                             "{\n" +
                             "  \"zabbix_admin_email\" : \"some value\",\n" +
                             "  \"mongo_user_password\" : \"some value\",\n" +
                             "  \"puppet_resource_url\" : \"some value\",\n" +
                             "  \"zabbix_admin_password\" : \"some value\",\n" +
                             "  \"zabbix_database_password\" : \"some value\",\n" +
                             "  \"mysql_root_user_password\" : \"some value\",\n" +
                             "  \"admin_ldap_password\" : \"some value\",\n" +
                             "  \"dns_name\" : \"some value\",\n" +
                             "  \"mongo_orgservice_user_password\" : \"some value\",\n" +
                             "  \"puppet_version\" : \"some value\",\n" +
                             "  \"admin_ldap_user_name\" : \"some value\",\n" +
                             "  \"mongo_admin_password\" : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "{\"infos\":{}}\n");
    }
}
