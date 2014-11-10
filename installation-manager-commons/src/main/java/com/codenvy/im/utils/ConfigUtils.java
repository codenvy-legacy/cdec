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

import com.codenvy.im.config.ConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class ConfigUtils {
    /** Utility class so there is no public constructor. */
    private ConfigUtils() {
    }

    public static Map<String, String> readProperties(InputStream in) throws ConfigException {
        Map<String, String> propertiesCandidate = new HashMap<>();
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            throw new ConfigException(format("Can't load properties: %s", e.getMessage()), e);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();   // set property name into UPPER case
            String value = (String)entry.getValue();

            propertiesCandidate.put(key, value);
        }

        return propertiesCandidate;
    }

    public static void storeProperties(Map<String, String> properties, OutputStream out) throws ConfigException {
        Properties propertiesToStore = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey().toString().toLowerCase();   // set property name into Upper case
            String value = entry.getValue();

            propertiesToStore.put(key, value);
        }

        try {
            propertiesToStore.store(out, null);
        } catch (IOException e) {
            throw new ConfigException(format("Can't store properties: %s", e.getMessage()), e);
        }
    }
}
