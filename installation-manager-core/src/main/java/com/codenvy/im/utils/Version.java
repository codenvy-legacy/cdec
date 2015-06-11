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
package com.codenvy.im.utils;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;

/**
 * @author Anatoliy Bazko
 */
public class Version implements Comparable<Version> {

    private static final String  MILESTONE_VERSION_PREFIX = "-M";
    private static final Pattern VERSION =
            compile("^(0|[1-9]+[0-9]*)\\.(0|[1-9]+[0-9]*)\\.(0|[1-9]+[0-9]*)" +
                    "(" + MILESTONE_VERSION_PREFIX + "[1-9]+[0-9]*|)(-SNAPSHOT|)$");

    private final int     major;
    private final int     minor;
    private final int     bugFix;
    private final int     milestone;
    private final boolean snapshot;

    public Version(int major, int minor, int bugFix, int milestone, boolean snapshot) {
        this.major = major;
        this.minor = minor;
        this.bugFix = bugFix;
        this.milestone = milestone;
        this.snapshot = snapshot;
    }

    /**
     * Checks if version format is valid.
     */
    public static boolean isValidVersion(String version) {
        return VERSION.matcher(version).matches();
    }

    /**
     * Parse version in string representation.
     *
     * @throws IllegalVersionException
     */
    public static Version valueOf(@Nonnull String version) throws IllegalVersionException {
        Matcher matcher = VERSION.matcher(version);
        if (!matcher.find()) {
            throw new IllegalVersionException(version);
        }

        int milestone = 0;

        String milestoneGroup = matcher.group(4);
        if (!milestoneGroup.isEmpty()) {
            milestone = parseInt(milestoneGroup.substring(MILESTONE_VERSION_PREFIX.length()));
        }

        return new Version(parseInt(matcher.group(1)),
                           parseInt(matcher.group(2)),
                           parseInt(matcher.group(3)),
                           milestone,
                           !matcher.group(5).isEmpty());
    }

    /**
     * Checks if version suites for pattern.
     * For example 3.1.0 version is suited for 3.1.* or 3.*.0
     */
    public boolean isSuitedFor(String versionRegex) {
        Pattern pattern = compile(versionRegex);
        return pattern.matcher(toString()).matches();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version)o;

        if (bugFix != version.bugFix) return false;
        if (major != version.major) return false;
        if (minor != version.minor) return false;
        if (milestone != version.milestone) return false;
        return snapshot == version.snapshot;

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + bugFix;
        result = 31 * result + milestone;
        result = 31 * result + (snapshot ? 0 : 1);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Version o) {
        if (major > o.major
            || (major == o.major && minor > o.minor)
            || (major == o.major && minor == o.minor && bugFix > o.bugFix)
            || (major == o.major && minor == o.minor && bugFix == o.bugFix && milestone > o.milestone)
            || (major == o.major && minor == o.minor && bugFix == o.bugFix && milestone == o.milestone && !snapshot && o.snapshot)) {
            return 1;
        } else if (major == o.major && minor == o.minor && bugFix == o.bugFix && milestone == o.milestone && snapshot == o.snapshot) {
            return 0;
        } else {
            return -1;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return major + "." + minor + "." + bugFix + (milestone > 0 ? MILESTONE_VERSION_PREFIX + milestone : "") + (snapshot ? "-SNAPSHOT" : "");

    }

    static public class ReverseOrder implements Comparator<Version> {
        @Override public int compare(Version v1, Version v2) {
            return v2.compareTo(v1);
        }
    }
}
