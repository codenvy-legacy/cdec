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
package com.codenvy.im.commands.decorators;

import com.codenvy.im.managers.NodeConfig;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author Dmytro Nochevnov */
public class PuppetError {

    private NodeConfig node;

    private String shortMessage;

    private enum Type {
        COULD_NOT_RETRIEVE_CATALOG("Could not retrieve catalog from remote server.*"),

        DEPENDENCY_HAS_FAILURES("Dependency .* has failures: true") {
            @Override
            boolean match(String line, Pattern messagePattern) {
                       return !line.contains("/Stage[main]/Multi_server::Api_instance::Service_codeassistant/Service[codenvy-codeassistant]")   // issue CDEC-264
                              && messagePattern.matcher(line).find();
            }
        };

        private Pattern messagePattern;

        private Type(String messagePattern) {
            this.messagePattern = Pattern.compile(messagePattern);
        }

        private String extractShortMessage(String logLine) {
            if (messagePattern == null) {
                return logLine;
            }

            Matcher m = messagePattern.matcher(logLine);
            if (m.find()) {
                return m.group();
            }

            return logLine;
        }

        boolean match(@Nullable String line) {
            return line != null && match(line, messagePattern);
        }

        boolean match(String line, Pattern messagePattern) {
            return messagePattern.matcher(line).find();
        }
    }

    PuppetError(NodeConfig node, String shortMessage) {
        this.node = node;
        this.shortMessage = shortMessage;
    }

    public NodeConfig getNode() {
        return node;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PuppetError that = (PuppetError)o;

        if (shortMessage != null ? !shortMessage.equals(that.shortMessage) : that.shortMessage != null) {
            return false;
        }
        if (node != null ? !node.equals(that.node) : that.node != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (shortMessage != null ? shortMessage.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "PuppetError{" +
               "node=" + node +
               ", message='" + shortMessage + '\'' +
               '}';
    }

    @Nullable
    public static PuppetError match(String logLine, NodeConfig node) {
        for (Type type : Type.values()) {
            Boolean match = type.match(logLine);
            if (match != null && match) {
                return new PuppetError(node, type.extractShortMessage(logLine));
            }
        }

        return null;
    }
}
