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
package com.codenvy.im.event;

import java.util.Map;

import static java.lang.String.format;

/**
 * @author Dmytro Nochevnov
 */
public class Event {
    private static final int MAX_EXTENDED_PARAMS_NUMBER = 3;
    private static final int RESERVED_PARAMS_NUMBER     = 6;
    private static final int MAX_PARAM_NAME_LENGTH      = 20;
    private static final int MAX_PARAM_VALUE_LENGTH     = 100;

    private Type type;
    private Map<String, String> parameters;

    public enum Type {
        IM_ARTIFACT_DOWNLOADED,
        IM_SUBSCRIPTION_ADDED,
        IM_ARTIFACT_INSTALL_STARTED,
        IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY,
        IM_ARTIFACT_INSTALL_FINISHED_UNSUCCESSFULLY;

        /**
         * transform from "IM_ARTIFACT_DOWNLOADED" to "im-artifact-downloaded"
         */
        public String toString() {
            return this.name().toLowerCase().replaceAll("_", "-");
        }
    }

    public Event() {
    }

    public Event(Type type, Map<String, String> parameters) {
        validate(parameters);

        this.type = type;
        this.parameters = parameters;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * adds new parameter or replaces existed one
     */
    public void putParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder record = new StringBuilder(format("EVENT#%s#", type));

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            record.append(format(" %s#%s#", entry.getKey(), entry.getValue()));
        }

        return record.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Event event = (Event)o;

        if (type != null ? !type.equals(event.type) : event.type != null) {
            return false;
        }

        if (parameters != null ? !parameters.equals(event.parameters) : event.parameters != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }


    private void validate(Map<String, String> parameters) throws IllegalArgumentException {
        if (parameters.size() > MAX_EXTENDED_PARAMS_NUMBER + RESERVED_PARAMS_NUMBER) {
            throw new IllegalArgumentException("The number of parameters exceeded the limit in " + MAX_EXTENDED_PARAMS_NUMBER);
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String param = entry.getKey();
            String value = entry.getValue();

            if (param.length() > MAX_PARAM_NAME_LENGTH) {
                throw new IllegalArgumentException(
                    "The length of parameter name " + param + " exceeded the length in " + MAX_PARAM_NAME_LENGTH + " characters");

            } else if (value.length() > MAX_PARAM_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                    "The length of parameter value " + value + " exceeded the length in " + MAX_PARAM_VALUE_LENGTH + " characters");
            }
        }
    }
}
