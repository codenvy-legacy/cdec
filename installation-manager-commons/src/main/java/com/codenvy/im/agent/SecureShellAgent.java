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
package com.codenvy.im.agent;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class SecureShellAgent implements Agent {

    private final Session session;
    private final JSch jsch = getJSch();

    /** Create ssh session object and try to connect to remote host using password. */
    public SecureShellAgent(final String host,
                            final int port,
                            final String user,
                            final String password) throws AgentException {
        try {
            session = getSession(host, port, user);
            session.setPassword(password);
            session.connect();
        } catch (JSchException e) {
            String errorMessage = format("Can't connect to host '%s@%s:%s'.", user, host, port);
            throw makeAgentException(errorMessage, e);
        }
    }

    /** Create ssh session object and try to connect to remote host by using auth key. */
    public SecureShellAgent(final String host,
                            final int port,
                            final String user,
                            final String privateKeyFileAbsolutePath,
                            final @Nullable String passPhrase) throws AgentException {
        try {
            session = getSession(host, port, user);

            if (passPhrase != null) {
                jsch.addIdentity(privateKeyFileAbsolutePath, passPhrase);
            } else {
                jsch.addIdentity(privateKeyFileAbsolutePath);
            }

            session.connect();
        } catch (JSchException e) {
            String errorMessage = format("Can't connect to host '%s@%s:%s' by using private key '%s'.", user, host, port, privateKeyFileAbsolutePath);
            throw makeAgentException(errorMessage, e);
        }
    }

    /** for unit testing propose */
    @Deprecated
    JSch getJSch() {
        return new JSch();
    } // TODO

    /** {@inheritDoc} */
    @Override
    public String execute(String command) throws AgentException {
        return execute(command, 0);
    }

    /** {@inheritDoc} */
    @Override
    public String execute(String command, int timeoutMillis) throws AgentException {
        ChannelExec channel = null;

        try {
            channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            InputStream error = channel.getErrStream();

            channel.connect(timeoutMillis);

            waitForChannelClosed(channel);

            int exitStatus = channel.getExitStatus();
            if (exitStatus != 0) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(error));
                throw new Exception(IOUtils.toString(bufferedReader));
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                return IOUtils.toString(bufferedReader);
            }
        } catch (Exception e) {
            throw new AgentException(e.getMessage(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private Session getSession(String host, int port, String user) throws JSchException {
        Session session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }

    private void waitForChannelClosed(Channel channel) throws InterruptedException {
        while (!channel.isClosed()) {
            Thread.sleep(100);
        }
    }

    @PreDestroy
    public void disconnect() {
        session.disconnect();
    } // TODO

    private AgentException makeAgentException(String errorMessage, JSchException e) {
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorMessage += format(" Error: %s", e.getMessage());
        }
        return new AgentException(errorMessage, e);
    }
}
