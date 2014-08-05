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
package com.codenvy.cdec.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anatoliy Bazko
 */
public class Version implements Comparable<Version> {

    private static final Pattern VERSION = Pattern.compile("^([1-9]+[0-9]*)\\.(0|[1-9]+[0-9]*)\\.(0|[1-9]+[0-9]*)(-SNAPSHOT|)$");

    private final int     major;
    private final int     minor;
    private final int     bugFix;
    private final boolean shapshot;

    public Version(int major, int minor, int bugFix, boolean shapshot) {
        this.major = major;
        this.minor = minor;
        this.bugFix = bugFix;
        this.shapshot = shapshot;
    }

    /**
     * Compares two versions.
     *
     * @see java.lang.Comparable#compareTo(Object)
     */
    public static int compare(String version1, String version2) {
        return valueOf(version1).compareTo(valueOf(version2));
    }

    /**
     * Compares two versions.
     *
     * @see java.lang.Comparable#compareTo(Object)
     */
    public static int compare(Version version1, Version version2) {
        return version1.compareTo(version2);
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
     * @throws IllegalArgumentException
     */
    public static Version valueOf(String version) throws IllegalArgumentException {
        Matcher matcher = VERSION.matcher(version);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Illegal version '" + version + "'");
        }

        return new Version(Integer.parseInt(matcher.group(1)),
                           Integer.parseInt(matcher.group(2)),
                           Integer.parseInt(matcher.group(3)),
                           !matcher.group(4).isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version = (Version)o;

        if (bugFix != version.bugFix) return false;
        if (major != version.major) return false;
        if (minor != version.minor) return false;
        if (shapshot != version.shapshot) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + bugFix;
        result = 31 * result + (shapshot ? 0 : 1);
        return result;
    }

    @Override
    public int compareTo(Version o) {
        if (major > o.major
            || (major == o.major && minor > o.minor)
            || (major == o.major && minor == o.minor && bugFix > o.bugFix)
            || (major == o.major && minor == o.minor && bugFix == o.bugFix && !shapshot && o.shapshot)) {
            return 1;
        } else if (major == o.major && minor == o.minor && bugFix == o.bugFix && shapshot == o.shapshot) {
            return 0;
        } else {
            return -1;
        }
    }

    public String getAsString() {
        return major + "." + minor + "." + bugFix + (shapshot ? "-SNAPSHOT" : "");
    }


    @Override
    public String toString() {
        return getAsString();
    }
}
