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

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Dmytro Nochevnov
 */
public class EventFactory {
    public static final String TIME_PARAM          = "TIME";
    public static final String USER_PARAM          = "USER";
    public static final String PLAN_PARAM          = "PLAN";
    public static final String ARTIFACT_PARAM      = "ARTIFACT";
    public static final String VERSION_PARAM       = "VERSION";
    public static final String USER_IP_PARAM       = "USER-IP";
    public static final String ERROR_MESSAGE_PARAM = "ERROR-MESSAGE";

    /**
     * Creates Event object of certain type with certain parameters + parameter(TIME = [current_system_time])
     */
    public static Event createWithTime(final Event.Type type, @Nonnull Map<String, String> parameters) {
        parameters = new LinkedHashMap<>(parameters);
        parameters.put(TIME_PARAM, getTime());

        return new Event(type, parameters);
    }

    public static Event createImSubscriptionAddedEventWithTime(final String planId, final String userId) {
        Map<String, String> eventParameters = new LinkedHashMap<>();
        eventParameters.put(PLAN_PARAM, planId);
        eventParameters.put(USER_PARAM, userId);

        return createWithTime(Event.Type.IM_SUBSCRIPTION_ADDED, eventParameters);
    }

    public static Event createImArtifactDownloadedEventWithTime(final String artifact, final String version, final String userId) {
        Map<String, String> eventParameters = new LinkedHashMap<>();
        eventParameters.put(ARTIFACT_PARAM, artifact);
        eventParameters.put(VERSION_PARAM, version);
        eventParameters.put(USER_PARAM, userId);

        return createWithTime(Event.Type.IM_ARTIFACT_DOWNLOADED, eventParameters);
    }

    public static Event createImArtifactInstallStartedEventWithTime(final String artifact, final String version) {
        Map<String, String> eventParameters = new LinkedHashMap<>();
        eventParameters.put(ARTIFACT_PARAM, artifact);
        eventParameters.put(VERSION_PARAM, version);

        return createWithTime(Event.Type.IM_ARTIFACT_INSTALL_STARTED, eventParameters);
    }

    public static Event createImArtifactInstallFinishedSuccessfullyEventWithTime(final String artifact, final String version) {
        Map<String, String> eventParameters = new LinkedHashMap<>();
        eventParameters.put(ARTIFACT_PARAM, artifact);
        eventParameters.put(VERSION_PARAM, version);

        return createWithTime(Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY, eventParameters);
    }

    public static Event createImArtifactInstallFinishedUnsuccessfullyEventWithTime(final String artifact,
                                                                                   final String version,
                                                                                   final String errorMessage) {
        Map<String, String> eventParameters = new LinkedHashMap<>();
        eventParameters.put(ARTIFACT_PARAM, artifact);
        eventParameters.put(VERSION_PARAM, version);
        eventParameters.put(ERROR_MESSAGE_PARAM, errorMessage);

        return createWithTime(Event.Type.IM_ARTIFACT_INSTALL_FINISHED_SUCCESSFULLY, eventParameters);
    }

    private static String getTime() {
        return String.valueOf(System.currentTimeMillis());
    }

}
