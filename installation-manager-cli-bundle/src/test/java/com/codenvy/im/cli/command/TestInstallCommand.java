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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;

/**
 * @author Dmytro Nochevnov
 *         Alexander Reshetnyak
 */
public class TestInstallCommand extends AbstractTestCommand {
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

        performBaseMocks(spyCommand);

        userCredentials = new UserCredentials("token", "accountId");
        doReturn(userCredentials).when(spyCommand).getCredentials();
    }

    @Test
    public void testInstallArtifact() throws Exception {
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doReturn(new InstallOptions()).when(spyCommand).enterInstallOptions(any(InstallOptions.class), anyBoolean());
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
        doReturn(new HashMap<String, String>() {{
            put("mongo_admin_password", "mongoPassword");
            put("mongo_user_password", "mongoUserPassword");
            put("mongo_orgservice_user_password", "mongoOrgServiceUserPassword");
        }}).when(configFactory).loadConfigProperties(anyString());

        // user always enter "some value" as property value
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                InstallCommand installCommand = (InstallCommand)invocationOnMock.getMock();
                installCommand.print(": some value\n");
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
                installCommand.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return false;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                InstallCommand installCommand = (InstallCommand)invocationOnMock.getMock();
                installCommand.print(invocationOnMock.getArguments()[0].toString() + " [y/N]\n");
                return true;
            }
        }).when(spyCommand).askUser(anyString());

        CommandInvoker commandInvoker = new CommandInvoker(spyCommand, commandSession);
        commandInvoker.argument("artifact", CDECArtifact.NAME);

        CommandInvoker.Result result = commandInvoker.invoke();
        String output = result.disableAnsi().getOutputStream();

        assertEquals(output, "Please, enter CDEC required parameters:\n" +
                             "host_url: some value\n" +
                             "mongo_admin_pass: some value\n" +
                             "mongo_user_pass: some value\n" +
                             "mongo_orgservice_user_pwd: some value\n" +
                             "user_ldap_password: some value\n" +
                             "admin_ldap_user_name: some value\n" +
                             "admin_ldap_password: some value\n" +
                             "mysql_root_password: some value\n" +
                             "zabbix_db_pass: some value\n" +
                             "zabbix_time_zone: some value\n" +
                             "zabbix_admin_email: some value\n" +
                             "zabbix_admin_password: some value\n" +
                             "haproxy_statistic_pass: some value\n" +
                             "jmx_username: some value\n" +
                             "jmx_password: some value\n" +
                             "builder_max_execution_time: some value\n" +
                             "builder_waiting_time: some value\n" +
                             "builder_keep_result_time: some value\n" +
                             "builder_queue_size: some value\n" +
                             "runner_default_app_mem_size: some value\n" +
                             "runner_workspace_max_memsize: some value\n" +
                             "runner_app_lifetime: some value\n" +
                             "runner_waiting_time: some value\n" +
                             "workspace_inactive_temporary_stop_time: some value\n" +
                             "workspace_inactive_persistent_stop_time: some value\n" +
                             "codenvy_server_xmx: some value\n" +
                             "google_client_id: some value\n" +
                             "google_secret: some value\n" +
                             "github_client_id: some value\n" +
                             "github_secret: some value\n" +
                             "bitbucket_client_id: some value\n" +
                             "bitbucket_secret: some value\n" +
                             "wso2_client_id: some value\n" +
                             "wso2_secret: some value\n" +
                             "projectlocker_client_id: some value\n" +
                             "projectlocker_secret: some value\n" +
                             "puppet_agent_version: some value\n" +
                             "puppet_server_version: some value\n" +
                             "puppet_resource_url: some value\n" +
                             "{\n" +
                             "  \"admin_ldap_password\"                     : \"some value\",\n" +
                             "  \"admin_ldap_user_name\"                    : \"some value\",\n" +
                             "  \"bitbucket_client_id\"                     : \"some value\",\n" +
                             "  \"bitbucket_secret\"                        : \"some value\",\n" +
                             "  \"builder_keep_result_time\"                : \"some value\",\n" +
                             "  \"builder_max_execution_time\"              : \"some value\",\n" +
                             "  \"builder_queue_size\"                      : \"some value\",\n" +
                             "  \"builder_waiting_time\"                    : \"some value\",\n" +
                             "  \"codenvy_server_xmx\"                      : \"some value\",\n" +
                             "  \"github_client_id\"                        : \"some value\",\n" +
                             "  \"github_secret\"                           : \"some value\",\n" +
                             "  \"google_client_id\"                        : \"some value\",\n" +
                             "  \"google_secret\"                           : \"some value\",\n" +
                             "  \"haproxy_statistic_pass\"                  : \"some value\",\n" +
                             "  \"host_url\"                                : \"some value\",\n" +
                             "  \"jmx_password\"                            : \"some value\",\n" +
                             "  \"jmx_username\"                            : \"some value\",\n" +
                             "  \"mongo_admin_pass\"                        : \"some value\",\n" +
                             "  \"mongo_orgservice_user_pwd\"               : \"some value\",\n" +
                             "  \"mongo_user_pass\"                         : \"some value\",\n" +
                             "  \"mysql_root_password\"                     : \"some value\",\n" +
                             "  \"projectlocker_client_id\"                 : \"some value\",\n" +
                             "  \"projectlocker_secret\"                    : \"some value\",\n" +
                             "  \"puppet_agent_version\"                    : \"some value\",\n" +
                             "  \"puppet_resource_url\"                     : \"some value\",\n" +
                             "  \"puppet_server_version\"                   : \"some value\",\n" +
                             "  \"runner_app_lifetime\"                     : \"some value\",\n" +
                             "  \"runner_default_app_mem_size\"             : \"some value\",\n" +
                             "  \"runner_waiting_time\"                     : \"some value\",\n" +
                             "  \"runner_workspace_max_memsize\"            : \"some value\",\n" +
                             "  \"user_ldap_password\"                      : \"some value\",\n" +
                             "  \"workspace_inactive_persistent_stop_time\" : \"some value\",\n" +
                             "  \"workspace_inactive_temporary_stop_time\"  : \"some value\",\n" +
                             "  \"wso2_client_id\"                          : \"some value\",\n" +
                             "  \"wso2_secret\"                             : \"some value\",\n" +
                             "  \"zabbix_admin_email\"                      : \"some value\",\n" +
                             "  \"zabbix_admin_password\"                   : \"some value\",\n" +
                             "  \"zabbix_db_pass\"                          : \"some value\",\n" +
                             "  \"zabbix_time_zone\"                        : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "Please, enter CDEC required parameters:\n" +
                             "host_url (some value): some value\n" +
                             "mongo_admin_pass (some value): some value\n" +
                             "mongo_user_pass (some value): some value\n" +
                             "mongo_orgservice_user_pwd (some value): some value\n" +
                             "user_ldap_password (some value): some value\n" +
                             "admin_ldap_user_name (some value): some value\n" +
                             "admin_ldap_password (some value): some value\n" +
                             "mysql_root_password (some value): some value\n" +
                             "zabbix_db_pass (some value): some value\n" +
                             "zabbix_time_zone (some value): some value\n" +
                             "zabbix_admin_email (some value): some value\n" +
                             "zabbix_admin_password (some value): some value\n" +
                             "haproxy_statistic_pass (some value): some value\n" +
                             "jmx_username (some value): some value\n" +
                             "jmx_password (some value): some value\n" +
                             "builder_max_execution_time (some value): some value\n" +
                             "builder_waiting_time (some value): some value\n" +
                             "builder_keep_result_time (some value): some value\n" +
                             "builder_queue_size (some value): some value\n" +
                             "runner_default_app_mem_size (some value): some value\n" +
                             "runner_workspace_max_memsize (some value): some value\n" +
                             "runner_app_lifetime (some value): some value\n" +
                             "runner_waiting_time (some value): some value\n" +
                             "workspace_inactive_temporary_stop_time (some value): some value\n" +
                             "workspace_inactive_persistent_stop_time (some value): some value\n" +
                             "codenvy_server_xmx (some value): some value\n" +
                             "google_client_id (some value): some value\n" +
                             "google_secret (some value): some value\n" +
                             "github_client_id (some value): some value\n" +
                             "github_secret (some value): some value\n" +
                             "bitbucket_client_id (some value): some value\n" +
                             "bitbucket_secret (some value): some value\n" +
                             "wso2_client_id (some value): some value\n" +
                             "wso2_secret (some value): some value\n" +
                             "projectlocker_client_id (some value): some value\n" +
                             "projectlocker_secret (some value): some value\n" +
                             "puppet_agent_version (some value): some value\n" +
                             "puppet_server_version (some value): some value\n" +
                             "puppet_resource_url (some value): some value\n" +
                             "{\n" +
                             "  \"admin_ldap_password\"                     : \"some value\",\n" +
                             "  \"admin_ldap_user_name\"                    : \"some value\",\n" +
                             "  \"bitbucket_client_id\"                     : \"some value\",\n" +
                             "  \"bitbucket_secret\"                        : \"some value\",\n" +
                             "  \"builder_keep_result_time\"                : \"some value\",\n" +
                             "  \"builder_max_execution_time\"              : \"some value\",\n" +
                             "  \"builder_queue_size\"                      : \"some value\",\n" +
                             "  \"builder_waiting_time\"                    : \"some value\",\n" +
                             "  \"codenvy_server_xmx\"                      : \"some value\",\n" +
                             "  \"github_client_id\"                        : \"some value\",\n" +
                             "  \"github_secret\"                           : \"some value\",\n" +
                             "  \"google_client_id\"                        : \"some value\",\n" +
                             "  \"google_secret\"                           : \"some value\",\n" +
                             "  \"haproxy_statistic_pass\"                  : \"some value\",\n" +
                             "  \"host_url\"                                : \"some value\",\n" +
                             "  \"jmx_password\"                            : \"some value\",\n" +
                             "  \"jmx_username\"                            : \"some value\",\n" +
                             "  \"mongo_admin_pass\"                        : \"some value\",\n" +
                             "  \"mongo_orgservice_user_pwd\"               : \"some value\",\n" +
                             "  \"mongo_user_pass\"                         : \"some value\",\n" +
                             "  \"mysql_root_password\"                     : \"some value\",\n" +
                             "  \"projectlocker_client_id\"                 : \"some value\",\n" +
                             "  \"projectlocker_secret\"                    : \"some value\",\n" +
                             "  \"puppet_agent_version\"                    : \"some value\",\n" +
                             "  \"puppet_resource_url\"                     : \"some value\",\n" +
                             "  \"puppet_server_version\"                   : \"some value\",\n" +
                             "  \"runner_app_lifetime\"                     : \"some value\",\n" +
                             "  \"runner_default_app_mem_size\"             : \"some value\",\n" +
                             "  \"runner_waiting_time\"                     : \"some value\",\n" +
                             "  \"runner_workspace_max_memsize\"            : \"some value\",\n" +
                             "  \"user_ldap_password\"                      : \"some value\",\n" +
                             "  \"workspace_inactive_persistent_stop_time\" : \"some value\",\n" +
                             "  \"workspace_inactive_temporary_stop_time\"  : \"some value\",\n" +
                             "  \"wso2_client_id\"                          : \"some value\",\n" +
                             "  \"wso2_secret\"                             : \"some value\",\n" +
                             "  \"zabbix_admin_email\"                      : \"some value\",\n" +
                             "  \"zabbix_admin_password\"                   : \"some value\",\n" +
                             "  \"zabbix_db_pass\"                          : \"some value\",\n" +
                             "  \"zabbix_time_zone\"                        : \"some value\"\n" +
                             "}\n" +
                             "Continue installation [y/N]\n" +
                             "{\"infos\":{}}\n");
    }
}
