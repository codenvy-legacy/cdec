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

import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author Dmytro Nochevnov */
public enum PuppetError {
    COULD_NOT_RETRIEVE_CATALOG(
        new Function<String, Boolean>() {
            final Pattern identifyPattern = Pattern.compile("puppet-agent\\[\\d*\\]: Could not retrieve catalog from remote server");

            @Nullable
            @Override
            public Boolean apply(@Nullable String line) {
                return (line != null)
                       && identifyPattern.matcher(line).find();
            }
        }
    ),

    DEPENDENCY_HAS_FAILURES(
        Pattern.compile("Dependency .* has failures: true"),
        new Function<String, Boolean>() {
            final Pattern identifyPattern = Pattern.compile("puppet-agent\\[\\d*\\]: (.*) Dependency .* has failures: true");

            @Nullable
            @Override
            public Boolean apply(@Nullable String line) {
                return (line != null)
                       && !line.contains("(/Stage[main]/Multi_server::Api_instance::Service_codeassistant/Service[codenvy-codeassistant])")  // issue CDEC-264
                       && identifyPattern.matcher(line).find();
            }
        }
    );

    private final Pattern                   DISPLAY_PATTERN;
    private final Function<String, Boolean> MATCHER;

    PuppetError(Pattern displayPattern, Function<String, Boolean> matcher) {
        this.DISPLAY_PATTERN = displayPattern;
        this.MATCHER = matcher;
    }

    PuppetError(Function<String, Boolean> matcher) {
        this(null, matcher);
    }

    String getLineToDisplay(String logLine) {
        logLine = logLine.replace("\r", "");  // remove end of line symbol

        if (DISPLAY_PATTERN == null) {
            return logLine;
        }

        Matcher m = DISPLAY_PATTERN.matcher(logLine);
        if (m.find()) {
            return m.group();
        }

        return logLine;
    }

    public Boolean match(String line) {
        return MATCHER.apply(line);
    }

}
