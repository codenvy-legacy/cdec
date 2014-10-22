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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * @author Alexander Reshetnyak
 */
public class SecureSHell {

    private final Session session;

    public SecureSHell(String host, int port, String user, String password) throws IOException {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
        } catch (JSchException e) {
            throw new IOException("Can't connect to host " + user + "@" + host + ":" + port);
        }
    }

    public String execute(String... commands) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (String s : commands) {
            sb.append(s + ';');
        }
        String command = sb.toString();

        try {
            ChannelExec channel = (ChannelExec)session.openChannel("exec");

            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.setCommand(command);
            channel.connect();

            String msg = null;
            //String output = "";
            StringBuffer output = new StringBuffer();
            while ((msg = in.readLine()) != null) {
                output.append(msg + "\n");
            }


//            channel.getExitStatus() // validate result
            channel.disconnect();
            session.disconnect();

            return output.toString();

        } catch (JSchException | IOException e) {
            throw new IOException("Command execution fail: \"" + command + "\"" );
        }
    }
}
