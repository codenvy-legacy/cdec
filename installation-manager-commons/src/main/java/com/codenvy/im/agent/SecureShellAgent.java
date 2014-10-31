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

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
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

    /**
     * Create ssh session object and try to connect to remote host by using password.
     */
    public SecureShellAgent(String host, int port, String user, String password) throws AgentException {
        try {
            session = getSession(host, port, user);
            session.setPassword(password);
            session.connect();
        } catch (JSchException e) {
            String errorMessage = format("Can't connect to host '%s@%s:%s'.", user, host, port);
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += format(" Error: %s", e.getMessage());
            }

            throw new AgentException(errorMessage, e);
        }
    }

    /**
     * Create ssh session object and try to connect to remote host by using auth key.
     */
    public SecureShellAgent(String host, int port, String user, String privateKeyFileAbsolutePath, @Nullable String passphrase) throws
                                                                                                                                AgentException {
        try {
            session = getSession(host, port, user);

            if (passphrase != null) {
                jsch.addIdentity(privateKeyFileAbsolutePath, passphrase);
            } else {
                jsch.addIdentity(privateKeyFileAbsolutePath);
            }

            session.connect();
        } catch (JSchException e) {
            String errorMessage = format("Can't connect to host '%s@%s:%s' by using private key '%s'.", user, host, port, privateKeyFileAbsolutePath);
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += format(" Error: %s", e.getMessage());
            }

            throw new AgentException(errorMessage, e);
        }
    }

    @Override public String execute(String command) throws AgentException {
        return execute(command, 0);
    }

    @Override public String execute(String command, int timeoutMillis) throws AgentException {
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
                throw new Exception(read(bufferedReader));
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                return read(bufferedReader);
            }
        } catch (Exception e) {
            String errorMessage = format("Command '%s' execution fail.", command);
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += format(" Error: %s", e.getMessage());
            }

            throw new AgentException(errorMessage, e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /** for unit testing propose */
    JSch getJSch() {
        return new JSch();
    }

    private Session getSession(String host, int port, String user) throws JSchException {
        Session session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }

    private String read(BufferedReader reader) throws IOException {
        StringBuffer output = new StringBuffer();
        String msg = null;

        while ((msg = reader.readLine()) != null) {
            output.append(msg + "\n");
        }

        return output.toString();
    }

    private void waitForChannelClosed(Channel channel) throws InterruptedException {
        while (!channel.isClosed()) {
            Thread.sleep(100);
        }
    }

    @PreDestroy
    public void disconnect () {
        session.disconnect();
    }
}
