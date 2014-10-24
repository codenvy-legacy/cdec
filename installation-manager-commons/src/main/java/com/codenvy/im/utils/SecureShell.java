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
package com.codenvy.im.utils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

/**
 * @author Alexander Reshetnyak
 * @author Dmytro Nochevnov
 */
public class SecureShell {

    private final Session session;
    private final JSch jsch = new JSch();

    public SecureShell(String host, int port, String user) throws IOException {
        try {
            session = getSession(host, port, user);
            session.connect();
        } catch (JSchException e) {
            throw new IOException("Can't connect to host " + user + "@" + host + ":" + port + ": " + e.getMessage());
        }
    }

    public SecureShell(String host, int port, String user, String password) throws IOException {
        try {
            session = getSession(host, port, user);
            session.setPassword(password);
            session.connect();
        } catch (JSchException e) {
            throw new IOException("Can't connect to host " + user + "@" + host + ":" + port + ": " + e.getMessage());
        }
    }

    public SecureShell(String host, int port, String user, String privateKeyFileAbsolutePath, @Nullable String passphrase) throws
                                                                                                                           IOException {
        try {
            session = getSession(host, port, user);

            if (passphrase != null) {
                jsch.addIdentity(privateKeyFileAbsolutePath, passphrase);
            } else {
                jsch.addIdentity(privateKeyFileAbsolutePath);
            }

            session.connect();
        } catch (JSchException e) {
            throw new IOException("Can't connect to host " + user + "@" + host + ":" + port + ": " + e.getMessage());
        }
    }

    private Session getSession(String host, int port, String user) throws JSchException {
        Session session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }

    public String execute(String command) throws IOException {
        return execute(command, 0, new HashMap<String, String>(0));
    }

    public String execute(String command, Map<String, String> variables) throws IOException {
        return execute(command, 0, variables);
    }

    public String execute(String command, int timeoutMillis, @Nonnull Map<String, String> envVars) throws IOException {
        ChannelExec channel = null;

        for (Map.Entry<String, String> variable : envVars.entrySet()) {
            command = format("export %s='%s'; ", variable.getKey(), variable.getValue()) + command;
        }

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
            throw new IOException(format("Command '%s' execution fail. Error message: '%s'.", command, e.getMessage()));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
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
