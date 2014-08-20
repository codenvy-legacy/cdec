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
package com.codenvy.cdec.im.cli.command;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.restlet.resource.ResourceException;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;

/**
 * @author Dmytro Nochevnov
 * TODO check
 */
public abstract class MessageHelper {
    public static final String HELP_LINK            = "Use '<command> --help' for more information.";
    public static final String MISLEADING_ARGUMENTS = "Arguments are misleading. " + HELP_LINK;
    public static final String SERVER_ERROR         = "There was an error '%s'.";
    public static final String INCOMPLETE_RESPONSE  = "Incomplete response.";

    public static void println(ResourceException re) {
        String errorMessage = String.format(SERVER_ERROR, re.getStatus().toString());
        printlnRed(errorMessage);
    }

    public static void printlnGreen(String message) {
        println(GREEN, message);
    }

    public static void printlnRed(String message) {
        println(RED, message);
    }

    public static void println(Color color, String message) {
        Ansi buffer = Ansi.ansi();

        buffer.fg(color);
        buffer.a(message);

        buffer.reset();
        System.out.println(buffer.toString());
    }
}
